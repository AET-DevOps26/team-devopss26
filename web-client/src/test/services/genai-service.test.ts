import { describe, it, expect } from 'vitest';
import { health, createConversation, getConversation, deleteConversation, chat } from '../../services/genai/gen-a-i/gen-a-i';

describe('genai service', () => {
  it('health sends GET to /api/v1/health', async () => {
    const result = await health();
    expect(result).toHaveProperty('status', 'ok');
  });

  it('createConversation sends POST to /api/v1/conversations', async () => {
    const result = await createConversation({ user_id: 1 });
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
    const result = await chat({ message: 'Hello', user_id: 1 });
    expect(result).toHaveProperty('response');
    expect(result).toHaveProperty('conversation_id');
  });
});
