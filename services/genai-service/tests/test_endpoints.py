import pytest

pytestmark = pytest.mark.asyncio


async def test_health_is_public(client):
    response = await client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


async def test_create_conversation_requires_auth(client):
    response = await client.post("/api/v1/conversations")
    assert response.status_code == 401


async def test_create_and_get_conversation(client, auth_headers):
    create_resp = await client.post("/api/v1/conversations", headers=auth_headers)
    assert create_resp.status_code == 200
    conversation = create_resp.json()
    assert conversation["messages"] == []

    get_resp = await client.get(f"/api/v1/conversations/{conversation['id']}", headers=auth_headers)
    assert get_resp.status_code == 200
    assert get_resp.json()["id"] == conversation["id"]


async def test_get_conversation_owned_by_another_user_is_forbidden(client, auth_headers, make_token):
    create_resp = await client.post("/api/v1/conversations", headers=auth_headers)
    conversation_id = create_resp.json()["id"]

    other_headers = {"Authorization": f"Bearer {make_token(2)}"}
    response = await client.get(f"/api/v1/conversations/{conversation_id}", headers=other_headers)
    assert response.status_code == 403


async def test_get_missing_conversation_is_not_found(client, auth_headers):
    response = await client.get("/api/v1/conversations/999999", headers=auth_headers)
    assert response.status_code == 404


async def test_delete_conversation(client, auth_headers):
    create_resp = await client.post("/api/v1/conversations", headers=auth_headers)
    conversation_id = create_resp.json()["id"]

    delete_resp = await client.delete(f"/api/v1/conversations/{conversation_id}", headers=auth_headers)
    assert delete_resp.status_code == 200
    assert delete_resp.json() == {"message": "Conversation deleted"}

    get_resp = await client.get(f"/api/v1/conversations/{conversation_id}", headers=auth_headers)
    assert get_resp.status_code == 404


async def test_chat_rejects_empty_message(client, auth_headers):
    response = await client.post(
        "/api/v1/chat", json={"message": "   "}, headers=auth_headers
    )
    assert response.status_code == 400


async def test_chat_creates_conversation_and_replies(client, auth_headers):
    response = await client.post(
        "/api/v1/chat", json={"message": "hello"}, headers=auth_headers
    )
    assert response.status_code == 200
    body = response.json()
    assert body["response"] == "This is a canned test response."
    assert isinstance(body["conversation_id"], int)


async def test_request_with_malformed_bearer_token_is_unauthorized(client):
    response = await client.get(
        "/api/v1/conversations/1", headers={"Authorization": "Bearer not-a-jwt"}
    )
    assert response.status_code == 401
