import asyncio
import base64
import logging
import os
import sys
import time
import traceback
import uuid
from contextlib import asynccontextmanager
from contextvars import ContextVar
from datetime import datetime
from pathlib import Path
from typing import Optional, List
from urllib.parse import quote_plus

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

logger = logging.getLogger("genai-service")

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
    f"{quote_plus(os.environ['SERVICES_POSTGRES_PASSWORD'])}@"
    f"{os.environ['SERVICES_POSTGRES_URL']}:"
    f"{os.environ['SERVICES_POSTGRES_PORT_INT']}/"
    f"{os.environ['SERVICES_POSTGRES_DB']}"
)
_engine = create_async_engine(_db_url)
_sessions = async_sessionmaker(_engine, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


# ── ORM models ────────────────────────────────────────────────────────────────
class ChatConversation(Base):
    __tablename__ = "chat_conversation"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    title: Mapped[Optional[str]] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    messages: Mapped[List["ChatMessage"]] = relationship(
        "ChatMessage", back_populates="conversation", cascade="all, delete-orphan"
    )


class ChatMessage(Base):
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
_PUBLIC_KEY_PATH = "/api/v1/users/auth/public-key"
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
# app.include_router(genai_router) below.
_PUBLIC_PATHS = {"/api/v1/health", "/api/genai/api/v1/health", "/metrics", "/api/genai/metrics"}


async def _fetch_public_key_locked():
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
    if _public_key is not None:
        return _public_key
    async with _public_key_lock:
        if _public_key is not None:
            return _public_key
        return await _fetch_public_key_locked()


async def _try_clear_cached_key_for_refetch() -> bool:
    global _public_key
    async with _public_key_lock:
        if time.monotonic() - _last_fetch_time > _MIN_REFETCH_INTERVAL_S:
            _public_key = None
            return True
        return False


def _decode(token: str, key) -> dict:
    return jwt.decode(token, key=key, algorithms=["RS256"])


async def _authenticate(token: str) -> int:
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
    user_id = _current_user_id.get()
    if user_id is None:
        raise HTTPException(status_code=500, detail="User identity not resolved")
    return user_id


def _require_auth_header() -> str:
    auth_header = _current_auth_header.get()
    if auth_header is None:
        raise HTTPException(status_code=500, detail="Auth header not resolved")
    return auth_header


# ── RAG: Weaviate + embeddings ────────────────────────────────────────────────
_weaviate_client: Optional[weaviate.WeaviateClient] = None
_embedding_model: Optional[GoogleGenerativeAIEmbeddings] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
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
            model="models/gemini-embedding-001",
            google_api_key=gemini_api_key,
        )

    yield

    if _weaviate_client:
        _weaviate_client.close()


async def _fetch_user_data() -> list[str]:
    note_url = os.environ.get("NOTE_SERVICE_URL", "http://note-service-app:8005")
    calendar_url = os.environ.get("CALENDAR_SERVICE_URL", "http://calendar-service-app:8004")
    checklist_url = os.environ.get("CHECKLIST_SERVICE_URL", "http://checklist-service-app:8003")

    # note-service, calendar-service, and checklist-service all validate the JWT
    # themselves and derive the user id from it.
    auth_headers = {"Authorization": _require_auth_header()}

    async with httpx.AsyncClient(timeout=10.0) as client:
        notes_resp, events_resp, checklists_resp = await asyncio.gather(
            client.get(f"{note_url}/api/v1/notes", headers=auth_headers),
            client.get(f"{calendar_url}/api/v1/events", headers=auth_headers),
            client.get(f"{checklist_url}/api/v1/checklists", headers=auth_headers),
            return_exceptions=True,
        )

    chunks = []

    if not isinstance(notes_resp, Exception) and notes_resp.status_code == 200:
        for note in notes_resp.json().get("notes", []):
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
    elif model == "local":
        # Local / self-hosted LLM option: talk to an Ollama server running an open model
        # (e.g. Llama 3.1) so the service can operate fully offline with no hosted API key.
        # Not deployed in our environment — a local model is too heavy for the target infra —
        # so `langchain_ollama` is imported lazily and only used if someone explicitly
        # selects model="local" against a reachable OLLAMA_BASE_URL.
        from langchain_ollama import ChatOllama
        llm = ChatOllama(
            model=os.environ.get("LOCAL_LLM_MODEL", "llama3.1"),
            base_url=os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434"),
        )
    else:
        key = os.environ.get("GEMINI_API_KEY")
        if not key:
            raise ValueError("GEMINI_API_KEY environment variable is not set")
        llm = ChatGoogleGenerativeAI(model="gemini-3.1-flash-lite", google_api_key=key)
    return _prompt | llm | StrOutputParser()


# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(title="GenAI Chatbot Service", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
Instrumentator().instrument(app).expose(app)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(
        f"Unhandled exception: {type(exc).__name__}: {exc}\n"
        f"Path: {request.scope['path']}\n"
        f"Method: {request.method}\n"
        f"Traceback: {traceback.format_exc()}"
    )
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"}
    )


@app.middleware("http")
async def _auth_middleware(request: Request, call_next):
    # request.url.path incorporates root_path (for building external URLs), but
    # routing matches on the raw ASGI scope path, so compare against that instead.
    if request.scope["path"] in _PUBLIC_PATHS:
        return await call_next(request)

    auth_header = request.headers.get("authorization", "")
    if not auth_header.lower().startswith("bearer "):
        logger.warning(f"Auth failed: Missing bearer token for {request.scope['path']}")
        return JSONResponse(status_code=401, content={"detail": "Missing bearer token"})

    try:
        user_id = await _authenticate(auth_header[len("bearer "):])
    except HTTPException as exc:
        logger.warning(f"Auth failed: {exc.detail} for {request.scope['path']}")
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
    async def health(self) -> Health200Response:
        return Health200Response(status="ok")

    async def create_conversation(self) -> Conversation:
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
        user_id = _require_user_id()
        # Reject blank messages and messages containing NUL (\x00): PostgreSQL text
        # columns can't store null bytes, so they'd otherwise blow up the INSERT below.
        if not chat_request.message.strip() or "\x00" in chat_request.message:
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
                chunks = await _fetch_user_data()
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
app.include_router(genai_router)
