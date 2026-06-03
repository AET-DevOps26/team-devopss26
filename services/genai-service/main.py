import os
import time
from datetime import datetime
from typing import Optional, List

from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from pydantic import BaseModel
from sqlalchemy import String, Text, ForeignKey, DateTime, BigInteger, CheckConstraint, func
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.future import select
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship, selectinload


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


# ── LangChain ─────────────────────────────────────────────────────────────────
_prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant for a personal productivity app. "
               "Users can ask you about their notes, calendar events, and checklists."),
    ("human", "{message}"),
])
_llm = ChatGoogleGenerativeAI(model="gemini-2.0-flash", google_api_key=os.environ["GEMINI_API_KEY"])
_chain = _prompt | _llm | StrOutputParser()


# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(title="GenAI Chatbot Service")
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
async def create_conversation(request: ConversationCreateRequest, db: AsyncSession = Depends(get_db)):
    conversation = ChatConversation(user_id=request.user_id)
    db.add(conversation)
    await db.commit()
    await db.refresh(conversation)
    return conversation

# The schema of the output is defined above as ConversationOut
@app.get("/conversations/{conversation_id}", response_model=ConversationOut)
async def get_conversation(conversation_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(ChatConversation)
        .where(ChatConversation.id == conversation_id)
        .options(selectinload(ChatConversation.messages))
    )
    conversation = result.scalar_one_or_none()
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation not found")
    return conversation


@app.delete("/conversations/{conversation_id}")
async def delete_conversation(conversation_id: int, db: AsyncSession = Depends(get_db)):

    # Check whether the conversation actually exists
    result = await db.execute(select(ChatConversation).where(ChatConversation.id == conversation_id))
    conversation = result.scalar_one_or_none()
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation not found")
    
    # Delete the conversation
    await db.delete(conversation)
    await db.commit()
    return {"message": "Conversation deleted"}


## This is the main endpoint that chat requests go through
@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, db: AsyncSession = Depends(get_db)):
    if not request.message.strip():
        raise HTTPException(status_code=400, detail="message must not be empty")

    if request.conversation_id:             # If this is not a new conversation
        # Check whether the conversation actually exists in the database
        result = await db.execute(
            select(ChatConversation).where(ChatConversation.id == request.conversation_id)
        )
        conversation = result.scalar_one_or_none()
        if not conversation:
            raise HTTPException(status_code=404, detail="Conversation not found")
    else:
        # Otherwise create a new conversation
        conversation = ChatConversation(user_id=request.user_id, title=request.message[:100])
        db.add(conversation)
        await db.flush()

    # Add the user's message to the conversation with the role "USER"
    db.add(ChatMessage( 
        conversation_id=conversation.id, role="USER",
        content=request.message, timestamp=_now_ms(),
    ))
    response_text = await _chain.ainvoke({"message": request.message})  # Generate the model's message

    # Add the model's message to the conversation with the role "AGENT"
    db.add(ChatMessage(
        conversation_id=conversation.id, role="AGENT",
        content=response_text, timestamp=_now_ms(),
    ))
    await db.commit()

    return ChatResponse(response=response_text, conversation_id=conversation.id)
