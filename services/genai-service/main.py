import asyncio
import os
import time
import uuid
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional, List, Literal

import httpx
import jwt
import weaviate
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from langchain_cohere import ChatCohere
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings
from langchain_groq import ChatGroq
from langchain_mistralai import ChatMistralAI
from pydantic import BaseModel
from sqlalchemy import String, Text, ForeignKey, DateTime, BigInteger, CheckConstraint, func
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.future import select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, selectinload
from weaviate.classes.config import Property, DataType, Configure


# ── Database ─────────────────────────────────────────────────────────────────
_db_url = (
    f"postgresql+asyncpg://{os.environ['GENAI_SERVICE_POSTGRES_USER']}:"
    f"{os.environ['GENAI_SERVICE_POSTGRES_PASSWORD']}@"
    f"{os.environ['GENAI_SERVICE_POSTGRES_URL']}:"
    f"{os.environ['GENAI_SERVICE_POSTGRES_PORT_INT']}/"
    f"{os.environ['GENAI_SERVICE_POSTGRES_DB']}"
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
_JWT_PUBLIC_KEY = os.environ.get("JWT_PUBLIC_KEY", "").strip()
_bearer = HTTPBearer(auto_error=False)


def _get_current_user_id(credentials: Optional[HTTPAuthorizationCredentials] = Depends(_bearer)) -> Optional[int]:
    if credentials is None:
        return None
    try:
        if _JWT_PUBLIC_KEY:
            payload = jwt.decode(credentials.credentials, _JWT_PUBLIC_KEY, algorithms=["RS256"])
        else:
            payload = jwt.decode(credentials.credentials, options={"verify_signature": False}, algorithms=["RS256"])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError as exc:
        raise HTTPException(status_code=401, detail=f"Invalid token: {exc}")

    raw_id = payload.get("user_id")
    if raw_id is None:
        raise HTTPException(status_code=401, detail="Token does not contain a user identity")
    try:
        return int(raw_id)
    except (TypeError, ValueError):
        raise HTTPException(status_code=401, detail="Token user identity is not a valid integer")


# ── RAG: Weaviate + embeddings ────────────────────────────────────────────────
_weaviate_client: Optional[weaviate.WeaviateClient] = None
_embeddings: Optional[GoogleGenerativeAIEmbeddings] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _weaviate_client, _embeddings

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

    gemini_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if gemini_key:
        _embeddings = GoogleGenerativeAIEmbeddings(
            model="models/text-embedding-004",
            google_api_key=gemini_key,
        )

    yield

    if _weaviate_client:
        _weaviate_client.close()


async def _fetch_user_data(user_id: int) -> list[str]:
    note_url = os.environ.get("NOTE_SERVICE_URL", "http://note-service-app:8005")
    calendar_url = os.environ.get("CALENDAR_SERVICE_URL", "http://calendar-service-app:8004")
    checklist_url = os.environ.get("CHECKLIST_SERVICE_URL", "http://checklist-service-app:8003")

    async with httpx.AsyncClient(timeout=10.0) as client:
        notes_resp, events_resp, checklists_resp = await asyncio.gather(
            client.get(f"{note_url}/api/v1/notes", params={"userId": user_id}),
            client.get(f"{calendar_url}/api/v1/events", params={"userId": user_id}),
            client.get(f"{checklist_url}/api/v1/checklists", params={"userId": user_id}),
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
        for event in events_resp.json():
            chunks.append(
                f"[Calendar Event] {event.get('title', '')} "
                f"from {event.get('startTime', '')} to {event.get('endTime', '')} "
                f"at {event.get('location', '')}: {event.get('description', '')}"
            )

    if not isinstance(checklists_resp, Exception) and checklists_resp.status_code == 200:
        for checklist in checklists_resp.json():
            title = checklist.get("title", "")
            for item in checklist.get("items", []):
                status = "completed" if item.get("completed") else "not completed"
                chunks.append(f"[Checklist '{title}'] {item.get('text', '')} ({status})")

    return chunks


def _weaviate_search_sync(chunks: list[str], chunk_vecs: list, query_vec: list, top_k: int) -> str:
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
    if not chunks or _weaviate_client is None or _embeddings is None:
        return ""
    all_vecs = await _embeddings.aembed_documents(chunks + [query])
    chunk_vecs, query_vec = all_vecs[:-1], all_vecs[-1]
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(None, _weaviate_search_sync, chunks, chunk_vecs, query_vec, top_k)


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
        llm = ChatGroq(model="llama-3.1-8b-instant", api_key=os.environ["GROQ_API_KEY"])
    elif model == "mistral":
        llm = ChatMistralAI(model="mistral-small-latest", api_key=os.environ["MISTRAL_API_KEY"])
    elif model == "cohere":
        llm = ChatCohere(model="command-r", cohere_api_key=os.environ["COHERE_API_KEY"])
    else:
        llm = ChatGoogleGenerativeAI(model="gemini-2.0-flash", google_api_key=os.environ["GEMINI_API_KEY"])
    return _prompt | llm | StrOutputParser()


# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(title="GenAI Chatbot Service", root_path="/api/v1", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


async def get_db():
    async with _sessions() as db:
        yield db


def _now_ms() -> int:
    return int(time.time() * 1000)


# ── Schemas ───────────────────────────────────────────────────────────────────
class ConversationCreateRequest(BaseModel):
    user_id: int


class ChatRequest(BaseModel):
    message: str
    user_id: int
    conversation_id: Optional[int] = None
    model: Literal["gemini", "groq-llama", "mistral", "cohere"] = "gemini"


class MessageOut(BaseModel):
    id: int
    role: str
    content: str
    timestamp: int
    model_config = {"from_attributes": True}


class ConversationOut(BaseModel):
    id: int
    user_id: int
    title: Optional[str]
    created_at: datetime
    messages: List[MessageOut] = []
    model_config = {"from_attributes": True}


class ChatResponse(BaseModel):
    response: str
    conversation_id: int


# ── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/conversations", response_model=ConversationOut)
async def create_conversation(
    request: ConversationCreateRequest,
    jwt_user_id: Optional[int] = Depends(_get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user_id = jwt_user_id if jwt_user_id is not None else request.user_id
    conversation = ChatConversation(user_id=user_id)
    db.add(conversation)
    await db.commit()
    result = await db.execute(
        select(ChatConversation)
        .where(ChatConversation.id == conversation.id)
        .options(selectinload(ChatConversation.messages))
    )
    return result.scalar_one()


@app.get("/conversations/{conversation_id}", response_model=ConversationOut)
async def get_conversation(
    conversation_id: int,
    jwt_user_id: Optional[int] = Depends(_get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(ChatConversation)
        .where(ChatConversation.id == conversation_id)
        .options(selectinload(ChatConversation.messages))
    )
    conversation = result.scalar_one_or_none()
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation not found")
    if jwt_user_id is not None and conversation.user_id != jwt_user_id:
        raise HTTPException(status_code=403, detail="Access denied")
    return conversation


@app.delete("/conversations/{conversation_id}")
async def delete_conversation(
    conversation_id: int,
    jwt_user_id: Optional[int] = Depends(_get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(ChatConversation).where(ChatConversation.id == conversation_id))
    conversation = result.scalar_one_or_none()
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation not found")
    if jwt_user_id is not None and conversation.user_id != jwt_user_id:
        raise HTTPException(status_code=403, detail="Access denied")
    await db.delete(conversation)
    await db.commit()
    return {"message": "Conversation deleted"}


@app.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    jwt_user_id: Optional[int] = Depends(_get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    if not request.message.strip():
        raise HTTPException(status_code=400, detail="message must not be empty")

    user_id = jwt_user_id if jwt_user_id is not None else request.user_id

    if request.conversation_id:
        result = await db.execute(
            select(ChatConversation).where(ChatConversation.id == request.conversation_id)
        )
        conversation = result.scalar_one_or_none()
        if not conversation:
            raise HTTPException(status_code=404, detail="Conversation not found")
        if jwt_user_id is not None and conversation.user_id != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
    else:
        conversation = ChatConversation(user_id=user_id, title=request.message[:100])
        db.add(conversation)
        await db.flush()

    db.add(ChatMessage(
        conversation_id=conversation.id, role="USER",
        content=request.message, timestamp=_now_ms(),
    ))

    context = ""
    try:
        chunks = await _fetch_user_data(user_id)
        context = await _rag_retrieve(request.message, chunks)
    except Exception:
        pass

    response_text = await _build_chain(request.model).ainvoke({
        "message": request.message,
        "context": context or "No relevant data found in the user's notes, calendar, or checklists.",
    })

    db.add(ChatMessage(
        conversation_id=conversation.id, role="AGENT",
        content=response_text, timestamp=_now_ms(),
    ))
    await db.commit()

    return ChatResponse(response=response_text, conversation_id=conversation.id)
