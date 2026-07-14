import { useMutation, useQueryClient } from '@tanstack/react-query';
import { chat, deleteConversation } from '#/services/genai/gen-a-i/gen-a-i';
import { useAuthStore } from '#/stores/authStore';

export function useSendMessage() {
  return useMutation({
    mutationFn: ({
      message,
      conversationId,
    }: {
      message: string;
      conversationId?: number;
    }) => {
      const userId = useAuthStore.getState().userId ?? 1;
      return chat({ message, user_id: userId, conversation_id: conversationId ?? null });
    },
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
