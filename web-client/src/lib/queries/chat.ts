import { useMutation, useQueryClient } from '@tanstack/react-query';
import { chat, deleteConversation } from '#/services/genai/gen-a-i/gen-a-i';

export function useSendMessage() {
  return useMutation({
    mutationFn: ({
      message,
      conversationId,
    }: {
      message: string;
      conversationId?: number;
    }) =>
      chat(
        { message, conversation_id: conversationId ?? null },
        { timeout: 120_000 },
      ),
  });
}

export function useDeleteConversation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (conversationId: number) => deleteConversation(conversationId),
    onSuccess: () => {
      localStorage.removeItem('chat-last-conversation-id');
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
}
