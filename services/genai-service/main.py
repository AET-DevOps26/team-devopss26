"""GenAI chatbot service with RAG across user notes, calendar events, and checklists.

Provides a FastAPI application with LangChain-powered LLM chains (Gemini, Groq,
Mistral, Cohere), Weaviate vector search for retrieval-augmented generation, and
JWT-authenticated CRUD endpoints for chat conversations.
"""

import asyncio
import base64
import os
import sys
import time
import uuid
from contextlib import asynccontextmanager
from contextvars import ContextVar
from datetime import datetime
from pathlib import Path
from typing import Optional, List

import httpx
import jwt
import weaviate
from cryptography.hazmat.primitives.serialization import load_der_public_key
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from prometheus_fastapi_instrumentator import Instrumentator
from langchain_cohere import ChatCohere
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings
from langchain_groq import ChatGroq
from langchain_mistralai import ChatMistralAI
from sqlalchemy import String, Text, ForeignKey, DateTime, BigInteger, CheckConstraint, func
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from sqlalchemy.future import select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, selectinload
from weaviate.classes.config import Property, DataType, Configure

# The generated OpenAPI client (services/genai-service/generated/) is gitignored and
# regenerated via `npm run openapi:generate`; it isn't an installed package, so make it
# importable as `openapi_server` without requiring a separate pip install step.
sys.path.insert(0, str(Path(__file__).parent / "generated" / "src"))

from openapi_server.apis.gen_ai_api import router as genai_router  # noqa: E402
from openapi_server.apis.gen_ai_api_base import BaseGenAIApi  # noqa: E402
from openapi_server.models.chat_message import ChatMessage as ChatMessageModel  # noqa: E402
from openapi_server.models.chat_request import ChatRequest  # noqa: E402
from openapi_server.models.chat_response import ChatResponse  # noqa: E402
from openapi_server.models.conversation import Conversation  # noqa: E402
from openapi_server.models.delete_conversation200_response import DeleteConversation200Response  # noqa: E402
from openapi_server.models.health200_response import Health200Response  # noqa: E402


# ── Database ─────────────────────────────────────────────────────────────────
_db_url = (
    f"postgresql+asyncpg://{os.environ['SERVICES_POSTGRES_USER']}:"
    f"{os.environ['SERVICES_POSTGRES_PASSWORD']}@"
    f"{os.environ['SERVICES_POSTGRES_URL']}:"
    f"{os.environ['SERVICES_POSTGRES_PORT_INT']}/"
    f"genai_service_db"
)
_engine = create_async_engine(_db_url)
_sessions = async_sessionmaker(_engine, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


# ── ORM models ────────────────────────────────────────────────────────────────
class ChatConversation(Base):
    """Each row represents a single chat session owned by a user, with an optional
    title and a one-to-many relationship to :class:`ChatMessage`. Conversations
    cascade-delete their messages when removed.
    """
    __tablename__ = "chat_conversation"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[Optional[str]] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    messages: Mapped[List["ChatMessage"]] = relationship(
        "ChatMessage", back_populates="conversation", cascade="all, delete-orphan"
    )


class ChatMessage(Base):
    """Represents a single turn (``USER`` or ``AGENT``) within a conversation.
    The ``role`` column is constrained to ``'USER'`` or ``'AGENT'`` by a CHECK
    constraint. Each message belongs to exactly one :class:`ChatConversation`.
    """
    __tablename__ = "chat_message"
    __table_args__ = (CheckConstraint("role IN ('USER', 'AGENT')", name="chk_chat_message_role"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    conversation_id: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("chat_conversation.id", ondelete="CASCADE"), nullable=False
    )
    role: Mapped[str] = mapped_column(String(10), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    timestamp: Mapped[int] = mapped_column(BigInteger, nullable=False)
    conversation: Mapped["ChatConversation"] = relationship("ChatConversation", back_populates="messages")


# ── JWT auth ──────────────────────────────────────────────────────────────────
# Mirrors services/shared/.../TokenValidationInterceptor.java: fetch the shared
# RSA public key from user-service, cache it, and refetch on signature failure
# (e.g. user-service restarted with a new key) at most once per interval.
_USER_SERVICE_URL = os.environ.get("USER_SERVICE_URL", "http://user-service-app:8001").rstrip("/")
# user-service mounts its API under this Spring context-path; the shared
# USER_SERVICE_URL env var only carries scheme+host+port, so we append it here.
_PUBLIC_KEY_PATH = "/api/user/api/v1/users/auth/public-key"
_MIN_REFETCH_INTERVAL_S = 30.0

_public_key = None
_last_fetch_time = 0.0
_public_key_lock = asyncio.Lock()

# The generated BaseGenAIApi methods have a fixed signature (dictated by the OpenAPI
# spec) and can't take a FastAPI `Depends(...)` parameter, so authentication happens in
# HTTP middleware instead, which stashes the resolved user id here for the duration of
# the request.
_current_user_id: ContextVar[Optional[int]] = ContextVar("_current_user_id", default=None)
# Downstream services (calendar, checklist) validate the JWT themselves and derive the
# user id from it, so the original bearer token must be forwarded rather than the user id.
_current_auth_header: ContextVar[Optional[str]] = ContextVar("_current_auth_header", default=None)
# The Caddy gateway forwards /api/genai/* to this service without stripping the
# prefix (mirroring how Spring's server.servlet.context-path works for the other
# services), so routes must actually be mounted under it; see
# app.include_router(genai_router, prefix="/api/genai") below.
_PUBLIC_PATHS = {"/api/genai/api/v1/health", "/metrics"}


async def _fetch_public_key_locked():
    """Called under ``_public_key_lock``. Stores the deserialised key and a
    monotonic timestamp so the cache layer can enforce a minimum refetch
    interval. On any network or parsing failure the cached key is set to
    ``None``, forcing a retry on the next authentication attempt.
    """
    global _public_key, _last_fetch_time
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.get(f"{_USER_SERVICE_URL}{_PUBLIC_KEY_PATH}")
            resp.raise_for_status()
            base64_key = resp.json()["publicKey"]
        _public_key = load_der_public_key(base64.b64decode(base64_key))
        _last_fetch_time = time.monotonic()
    except Exception:
        _public_key = None
    return _public_key


async def _get_or_fetch_public_key():
    """Uses a double-checked locking pattern guarded by ``_public_key_lock`` so
    that concurrent requests block only once while the key is being fetched
    from user-service.
    """
    if _public_key is not None:
        return _public_key
    async with _public_key_lock:
        if _public_key is not None:
            return _public_key
        return await _fetch_public_key_locked()


async def _try_clear_cached_key_for_refetch() -> bool:
    """Rate-limits refetch attempts to one per ``_MIN_REFETCH_INTERVAL_S`` seconds,
    preventing a thundering-herd against user-service during key rotation.

    Returns ``True`` when the key was cleared (the caller should retry
    authentication), ``False`` when the interval has not yet passed.
    """
    global _public_key
    async with _public_key_lock:
        if time.monotonic() - _last_fetch_time > _MIN_REFETCH_INTERVAL_S:
            _public_key = None
            return True
        return False


def _decode(token: str, key) -> dict:
    return jwt.decode(token, key=key, algorithms=["RS256"])


async def _authenticate(token: str) -> int:
    """On :class:`jwt.InvalidSignatureError` the cached key is invalidated and one
    refetch attempt is made — this handles key rotation when user-service
    restarts with a new key pair. Other token errors (expired, malformed) are
    raised immediately.
    """
    active_key = await _get_or_fetch_public_key()
    if active_key is None:
        raise HTTPException(status_code=500, detail="Token validation service unavailable (public key not fetched)")

    try:
        payload = _decode(token, active_key)
    except jwt.InvalidSignatureError:
        # Public key may have rotated (e.g. user-service restarted). Refetch and retry once.
        payload = None
        if await _try_clear_cached_key_for_refetch():
            new_key = await _get_or_fetch_public_key()
            if new_key is not None and new_key != active_key:
                try:
                    payload = _decode(token, new_key)
                except jwt.InvalidTokenError:
                    payload = None
        if payload is None:
            raise HTTPException(status_code=401, detail="Invalid or expired JWT token")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError as exc:
        raise HTTPException(status_code=401, detail=f"Invalid token: {exc}")

    raw_id = payload.get("sub")
    if raw_id is None:
        raise HTTPException(status_code=401, detail="Token does not contain a user identity")
    try:
        return int(raw_id)
    except (TypeError, ValueError):
        raise HTTPException(status_code=401, detail="Token user identity is not a valid integer")


def _require_user_id() -> int:
    """The value is set by :func:`_auth_middleware` into a :class:`ContextVar` —
    this pattern is used instead of FastAPI ``Depends()`` because the generated
    endpoint signatures are fixed by the OpenAPI spec and cannot accept extra
    parameters.
    """
    user_id = _current_user_id.get()
    if user_id is None:
        raise HTTPException(status_code=500, detail="User identity not resolved")
    return user_id


def _require_auth_header() -> str:
    """Downstream services (calendar, checklist) validate the JWT themselves and
    need the raw bearer token rather than the derived user id. The header is
    stashed by :func:`_auth_middleware` in a :class:`ContextVar`.
    """
    auth_header = _current_auth_header.get()
    if auth_header is None:
        raise HTTPException(status_code=500, detail="Auth header not resolved")
    return auth_header


# ── RAG: Weaviate + embeddings ────────────────────────────────────────────────
_weaviate_client: Optional[weaviate.WeaviateClient] = None
_embedding_model: Optional[GoogleGenerativeAIEmbeddings] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """FastAPI lifespan context manager for Weaviate and embedding model lifecycle.

    Initialises the Weaviate custom connection (HTTP + gRPC) and the Google
    Generative AI embedding model on startup. On shutdown the Weaviate client
    is closed. Both operations are best-effort: if Weaviate is unavailable the
    service degrades gracefully (RAG returns empty context).
    """
    global _weaviate_client, _embedding_model

    try:
        _weaviate_client = weaviate.connect_to_custom(
            http_host=os.environ.get("WEAVIATE_HOST", "weaviate"),
            http_port=int(os.environ.get("WEAVIATE_HTTP_PORT", "8080")),
            http_secure=False,
            grpc_host=os.environ.get("WEAVIATE_HOST", "weaviate"),
            grpc_port=int(os.environ.get("WEAVIATE_GRPC_PORT", "50051")),
            grpc_secure=False,
        )
    except Exception:
        _weaviate_client = None

    gemini_api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if gemini_api_key:
        _embedding_model = GoogleGenerativeAIEmbeddings(
            model="models/text-embedding-004",
            google_api_key=gemini_api_key,
        )

    yield

    if _weaviate_client:
        _weaviate_client.close()


async def _fetch_user_data(user_id: int) -> list[str]:
    """Each service URL is built from environment variables plus a Spring context-path
    suffix. Calendar and checklist services authenticate via JWT (forwarding the
    original bearer token), while note-service accepts a ``userId`` query parameter.
    Responses are flattened into a list of formatted text chunks suitable for RAG
    embedding. Individual service failures are silently ignored — only successful
    (HTTP 200) responses contribute chunks.
    """
    # *_SERVICE_URL env vars only carry scheme+host+port; each service serves its API
    # under its own Spring context path, so it must be appended here (same as
    # _PUBLIC_KEY_PATH above).
    note_url = os.environ.get("NOTE_SERVICE_URL", "http://note-service-app:8005") + "/api/note"
    calendar_url = os.environ.get("CALENDAR_SERVICE_URL", "http://calendar-service-app:8004") + "/api/calendar"
    checklist_url = os.environ.get("CHECKLIST_SERVICE_URL", "http://checklist-service-app:8003") + "/api/checklist"

    # calendar-service and checklist-service validate the JWT themselves and derive the
    # user id from it (no userId query param accepted); note-service is still an
    # unauthenticated openapi stub that takes userId directly.
    auth_headers = {"Authorization": _require_auth_header()}

    async with httpx.AsyncClient(timeout=10.0) as client:
        notes_resp, events_resp, checklists_resp = await asyncio.gather(
            client.get(f"{note_url}/api/v1/notes", params={"userId": user_id}),
            client.get(f"{calendar_url}/api/v1/events", headers=auth_headers),
            client.get(f"{checklist_url}/api/v1/checklists", headers=auth_headers),
            return_exceptions=True,
        )

    chunks = []

    if not isinstance(notes_resp, Exception) and notes_resp.status_code == 200:
        for note in notes_resp.json():
            title = note.get("title", "")
            content = note.get("content", "")
            if title or content:
                chunks.append(f"[Note] {title}: {content}")

    if not isinstance(events_resp, Exception) and events_resp.status_code == 200:
        for event in events_resp.json().get("events", []):
            chunks.append(
                f"[Calendar Event] {event.get('title', '')} "
                f"from {event.get('startTime', '')} to {event.get('endTime', '')} "
                f"at {event.get('location', '')}: {event.get('description', '')}"
            )

    if not isinstance(checklists_resp, Exception) and checklists_resp.status_code == 200:
        for checklist in checklists_resp.json().get("checklists", []):
            title = checklist.get("title", "")
            for item in checklist.get("items", []):
                status = "completed" if item.get("completed") else "not completed"
                chunks.append(f"[Checklist '{title}'] {item.get('text', '')} ({status})")

    return chunks


def _rag_sync(query: str, chunks: list[str], top_k: int) -> str:
    """Synchronous vector similarity search over user data chunks.

    Embeds both the chunks and the query using the global embedding model,
    creates a temporary Weaviate collection (``Session{uuid}``), inserts
    chunks with their vectors, runs a ``near_vector`` search for the top-``k``
    results, and cleans up the collection in a ``finally`` block. This
    synchronous design lets it be offloaded to a thread pool via
    :func:`_rag_retrieve`.
    """
    all_vecs = _embedding_model.embed_documents(chunks + [query])
    chunk_vecs = all_vecs[:-1]
    query_vec = all_vecs[-1]

    collection_name = f"Session{uuid.uuid4().hex}"
    try:
        collection = _weaviate_client.collections.create(
            name=collection_name,
            properties=[Property(name="text", data_type=DataType.TEXT)],
            vectorizer_config=Configure.Vectorizer.none(),
        )
        with collection.batch.dynamic() as batch:
            for text, vector in zip(chunks, chunk_vecs):
                batch.add_object(properties={"text": text}, vector=vector)
        results = collection.query.near_vector(
            near_vector=query_vec,
            limit=min(top_k, len(chunks)),
        )
        return "\n".join(obj.properties["text"] for obj in results.objects)
    finally:
        _weaviate_client.collections.delete(collection_name)


async def _rag_retrieve(query: str, chunks: list[str], top_k: int = 5) -> str:
    """Offloads the blocking embedding and Weaviate calls to avoid stalling the
    asyncio event loop. Returns an empty string gracefully when no chunks are
    available, Weaviate is not connected, or the embedding model was not
    initialised.
    """
    if not chunks or _weaviate_client is None or _embedding_model is None:
        return ""
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(None, _rag_sync, query, chunks, top_k)


# ── LangChain ─────────────────────────────────────────────────────────────────
_prompt = ChatPromptTemplate.from_messages([
    ("system",
     "You are a helpful assistant for a personal productivity app. "
     "Use the context below, retrieved from the user's personal notes, calendar events, and checklists, "
     "to answer their question. If the context does not contain enough information, say so.\n\n"
     "Context:\n{context}"),
    ("human", "{message}"),
])


def _build_chain(model: str):
    """Build a LangChain ``Runnable`` (prompt | LLM | output parser) for the given model.

    Supported models:
    - ``groq-llama`` → ChatGroq (llama-3.1-8b-instant)
    - ``mistral`` → ChatMistralAI (mistral-small-latest)
    - ``cohere`` → ChatCohere (command-r)
    - anything else  → ChatGoogleGenerativeAI (gemini-2.0-flash) as fallback

    The prompt template injects RAG-retrieved context before the user's message.
    Raises :class:`ValueError` if the required API key for the chosen model is
    not set in the environment.
    """
    if model == "groq-llama":
        key = os.environ.get("GROQ_API_KEY")
        if not key:
            raise ValueError("GROQ_API_KEY environment variable is not set")
        llm = ChatGroq(model="llama-3.1-8b-instant", api_key=key)
    elif model == "mistral":
        key = os.environ.get("MISTRAL_API_KEY")
        if not key:
            raise ValueError("MISTRAL_API_KEY environment variable is not set")
        llm = ChatMistralAI(model="mistral-small-latest", api_key=key)
    elif model == "cohere":
        key = os.environ.get("COHERE_API_KEY")
        if not key:
            raise ValueError("COHERE_API_KEY environment variable is not set")
        llm = ChatCohere(model="command-r", cohere_api_key=key)
    else:
        key = os.environ.get("GEMINI_API_KEY")
        if not key:
            raise ValueError("GEMINI_API_KEY environment variable is not set")
        llm = ChatGoogleGenerativeAI(model="gemini-2.0-flash", google_api_key=key)
    return _prompt | llm | StrOutputParser()


# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(title="GenAI Chatbot Service", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
Instrumentator().instrument(app).expose(app)


@app.middleware("http")
async def _auth_middleware(request: Request, call_next):
    """Extracts the ``Authorization`` bearer token, resolves the user id via
    :func:`_authenticate`, and stashes both the user id and the raw auth header
    into :class:`ContextVar`\\ s for the duration of the request. This approach
    works around the constraint that OpenAPI-generated endpoint signatures cannot
    accept FastAPI ``Depends()`` parameters. On authentication failure a 401 JSON
    response is returned immediately without calling the downstream handler.
    """
    # request.url.path incorporates root_path (for building external URLs), but
    # routing matches on the raw ASGI scope path, so compare against that instead.
    if request.scope["path"] in _PUBLIC_PATHS:
        return await call_next(request)

    auth_header = request.headers.get("authorization", "")
    if not auth_header.lower().startswith("bearer "):
        return JSONResponse(status_code=401, content={"detail": "Missing bearer token"})

    try:
        user_id = await _authenticate(auth_header[len("bearer "):])
    except HTTPException as exc:
        return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})

    reset_user_id = _current_user_id.set(user_id)
    reset_auth_header = _current_auth_header.set(auth_header)
    try:
        return await call_next(request)
    finally:
        _current_user_id.reset(reset_user_id)
        _current_auth_header.reset(reset_auth_header)


def _now_ms() -> int:
    return int(time.time() * 1000)


def _to_conversation_model(conversation: ChatConversation) -> Conversation:
    """Requires eagerly loaded messages for serialisation.
    """
    return Conversation(
        id=conversation.id,
        user_id=conversation.user_id,
        title=conversation.title,
        created_at=conversation.created_at,
        messages=[
            ChatMessageModel(id=m.id, role=m.role, content=m.content, timestamp=m.timestamp)
            for m in conversation.messages
        ],
    )


# ── Endpoints ─────────────────────────────────────────────────────────────────
# Routes, request/response models, and their FastAPI signatures come from the
# generated OpenAPI client (services/genai-service/generated/), not from hand-written
# routes here, so the implementation can't silently drift from api/genai-service.yaml.
class GenAIApiImpl(BaseGenAIApi):
    """Registered automatically via ``__init_subclass__`` when the class is defined.
    Each method corresponds to an endpoint in the ``api/genai-service.yaml`` spec
    and enforces ownership checks, persists conversation state, and orchestrates
    the RAG + LLM pipeline.
    """

    async def health(self) -> Health200Response:
        """Listed in ``_PUBLIC_PATHS`` and skipped by the auth middleware.
        """
        return Health200Response(status="ok")

    async def create_conversation(self) -> Conversation:
        """Create a new empty conversation for the authenticated user.

        The conversation has no title initially; the title is set on the first
        chat message. The returned object includes the (empty) message list.
        """
        user_id = _require_user_id()
        async with _sessions() as db:
            conversation = ChatConversation(user_id=user_id)
            db.add(conversation)
            await db.commit()
            result = await db.execute(
                select(ChatConversation)
                .where(ChatConversation.id == conversation.id)
                .options(selectinload(ChatConversation.messages))
            )
            return _to_conversation_model(result.scalar_one())

    async def get_conversation(self, conversationId: int) -> Conversation:
        """Ownership is enforced: raises 403 if the conversation belongs to another
        user, or 404 if it does not exist.
        """
        user_id = _require_user_id()
        async with _sessions() as db:
            result = await db.execute(
                select(ChatConversation)
                .where(ChatConversation.id == conversationId)
                .options(selectinload(ChatConversation.messages))
            )
            conversation = result.scalar_one_or_none()
            if not conversation:
                raise HTTPException(status_code=404, detail="Conversation not found")
            if conversation.user_id != user_id:
                raise HTTPException(status_code=403, detail="Access denied")
            return _to_conversation_model(conversation)

    async def delete_conversation(self, conversationId: int) -> DeleteConversation200Response:
        """Ownership is enforced identically to :meth:`get_conversation`. Returns a
        confirmation message on success.
        """
        user_id = _require_user_id()
        async with _sessions() as db:
            result = await db.execute(select(ChatConversation).where(ChatConversation.id == conversationId))
            conversation = result.scalar_one_or_none()
            if not conversation:
                raise HTTPException(status_code=404, detail="Conversation not found")
            if conversation.user_id != user_id:
                raise HTTPException(status_code=403, detail="Access denied")
            await db.delete(conversation)
            await db.commit()
            return DeleteConversation200Response(message="Conversation deleted")

    async def chat(self, chat_request: ChatRequest) -> ChatResponse:
        """Process a chat message within an existing or new conversation.

        If ``conversation_id`` is provided the message is appended to that
        conversation (ownership is checked); otherwise a new conversation is
        created with the message text as its title. The pipeline then:

        1. Persists the user message immediately.
        2. Fetches the user's notes, calendar events, and checklists in parallel.
        3. Runs RAG vector search over the fetched data.
        4. Invokes the selected LLM chain with the RAG context and the user message.
        5. Persists the agent response and commits the transaction.

        RAG failures are silently caught so the LLM can still respond without
        retrieved context. The response includes both the generated text and
        the (possibly new) conversation id.

        Raises:
            HTTPException 400: If the message is empty after stripping.
            HTTPException 404: If the specified conversation does not exist.
            HTTPException 403: If the conversation belongs to another user.
        """
        user_id = _require_user_id()
        if not chat_request.message.strip():
            raise HTTPException(status_code=400, detail="message must not be empty")

        async with _sessions() as db:
            if chat_request.conversation_id:
                result = await db.execute(
                    select(ChatConversation).where(ChatConversation.id == chat_request.conversation_id)
                )
                conversation = result.scalar_one_or_none()
                if not conversation:
                    raise HTTPException(status_code=404, detail="Conversation not found")
                if conversation.user_id != user_id:
                    raise HTTPException(status_code=403, detail="Access denied")
            else:
                conversation = ChatConversation(user_id=user_id, title=chat_request.message[:100])
                db.add(conversation)
                await db.flush()

            db.add(ChatMessage(
                conversation_id=conversation.id, role="USER",
                content=chat_request.message, timestamp=_now_ms(),
            ))

            context = ""
            try:
                chunks = await _fetch_user_data(user_id)
                context = await _rag_retrieve(chat_request.message, chunks)
            except Exception:
                pass

            response_text = await _build_chain(chat_request.model).ainvoke({
                "message": chat_request.message,
                "context": context or "No relevant data found in the user's notes, calendar, or checklists.",
            })

            db.add(ChatMessage(
                conversation_id=conversation.id, role="AGENT",
                content=response_text, timestamp=_now_ms(),
            ))
            await db.commit()

            return ChatResponse(response=response_text, conversation_id=conversation.id)


# Defining the class above already registers it as BaseGenAIApi.subclasses[0]
# (via __init_subclass__); the generated router instantiates it per-request.
app.include_router(genai_router, prefix="/api/genai")
