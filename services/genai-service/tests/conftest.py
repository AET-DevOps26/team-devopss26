import os
import sys
from pathlib import Path

os.environ.setdefault("SERVICES_POSTGRES_USER", "postgres")
os.environ.setdefault("SERVICES_POSTGRES_PASSWORD", "postgres")
os.environ.setdefault("SERVICES_POSTGRES_URL", "localhost")
os.environ.setdefault("SERVICES_POSTGRES_PORT_INT", "5432")
os.environ.setdefault("SERVICES_POSTGRES_DB", "genai_service_db")

sys.path.insert(0, str(Path(__file__).parent.parent))

import httpx
import jwt
import pytest
import pytest_asyncio
from cryptography.hazmat.primitives.asymmetric import rsa
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from sqlalchemy.pool import NullPool

import main


class _FakeChain:
    """Stands in for the real LangChain chain so tests don't need LLM provider
    API keys or network access."""

    async def ainvoke(self, _payload):
        return "This is a canned test response."


@pytest.fixture(scope="session")
def rsa_key():
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


@pytest.fixture
def make_token(rsa_key):
    def _make(user_id: int) -> str:
        return jwt.encode({"sub": str(user_id)}, rsa_key, algorithm="RS256")

    return _make


@pytest_asyncio.fixture
async def app(rsa_key, monkeypatch):
    db_url = (
        f"postgresql+asyncpg://{os.environ['SERVICES_POSTGRES_USER']}:"
        f"{os.environ['SERVICES_POSTGRES_PASSWORD']}@"
        f"{os.environ['SERVICES_POSTGRES_URL']}:"
        f"{os.environ['SERVICES_POSTGRES_PORT_INT']}/genai_service_db"
    )
    # Schema setup happens on its own throwaway engine/connection so that
    # main._engine's connection pool isn't bound to *this* event loop: some
    # callers (schemathesis's call_asgi) run requests on a different loop than
    # this fixture's, and asyncpg connections can't cross loops.
    schema_engine = create_async_engine(db_url)
    async with schema_engine.begin() as conn:
        await conn.run_sync(main.Base.metadata.drop_all)
        await conn.run_sync(main.Base.metadata.create_all)
    await schema_engine.dispose()

    # NullPool: schemathesis's call_asgi spins up a fresh event loop per generated
    # example, and asyncpg connections can't be reused across loops, so every
    # checkout here must open a brand new physical connection.
    main._engine = create_async_engine(db_url, poolclass=NullPool)
    main._sessions = async_sessionmaker(main._engine, expire_on_commit=False)

    public_key = rsa_key.public_key()

    async def fake_get_or_fetch_public_key():
        return public_key

    monkeypatch.setattr(main, "_get_or_fetch_public_key", fake_get_or_fetch_public_key)
    monkeypatch.setattr(main, "_build_chain", lambda model: _FakeChain())

    yield main.app

    await main._engine.dispose()


@pytest_asyncio.fixture
async def client(app):
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as c:
        yield c


@pytest.fixture
def auth_headers(make_token):
    return {"Authorization": f"Bearer {make_token(1)}"}
