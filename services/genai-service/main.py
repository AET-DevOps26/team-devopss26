## retrieving data from the database for queries is yet to be implemented.
## currently, the service routes the query directly to chatgpt

import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from langchain_groq import ChatGroq
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

app = FastAPI(title="GenAI Chatbot Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_prompt = ChatPromptTemplate.from_messages([
    ("system", "You are a helpful assistant for a personal productivity app. "
               "Users can ask you about their notes, calendar events, and checklists."),
    ("human", "{message}"),
])

_llm = ChatGroq(
    model="llama-3.1-8b-instant",
    api_key=os.environ["GROQ_API_KEY"],
)

_chain = _prompt | _llm | StrOutputParser()


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    response: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    if not request.message.strip():
        raise HTTPException(status_code=400, detail="message must not be empty")
    response = await _chain.ainvoke({"message": request.message})
    return ChatResponse(response=response)
