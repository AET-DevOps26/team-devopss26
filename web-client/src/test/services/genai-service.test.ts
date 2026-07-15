import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import { health, createConversation, getConversation, deleteConversation, chat } from '#/services/genai/gen-a-i/gen-a-i.ts';

describe('genai service', () => {
  it('health sends GET to /api/v1/health', async () => {
    const result = await health();
    expect(result).toHaveProperty('status', 'ok');
  });

  it('createConversation sends POST to /api/v1/conversations', async () => {
    const result = await createConversation();
    expect(result).toHaveProperty('id');
    expect(result).toHaveProperty('title');
  });

  it('getConversation sends GET with conversationId', async () => {
    const result = await getConversation(1);
    expect(result).toHaveProperty('id', 1);
  });

  it('deleteConversation sends DELETE with conversationId', async () => {
    const result = await deleteConversation(1);
    expect(result).toHaveProperty('message');
  });

  it('chat sends POST to /api/v1/chat', async () => {
    const result = await chat({ message: 'Hello' });
    expect(result).toHaveProperty('response');
    expect(result).toHaveProperty('conversation_id');
  });

  it('chat throws on 400 empty message', async () => {
    server.use(
      http.post('*/api/v1/chat', () => HttpResponse.json(null, { status: 400 })),
    );
    await expect(chat({ message: '' })).rejects.toThrow();
  });
});
