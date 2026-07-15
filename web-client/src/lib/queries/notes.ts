import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  getNotes,
  createNote,
  updateNote,
  deleteNote,
} from '#/services/notes/notes/notes';
export const notesKeys = {
  all: ['notes'] as const,
  lists: () => [...notesKeys.all, 'list'] as const,
  list: (filters: string) => [...notesKeys.lists(), filters] as const,
};

export const notesQueries = {
  all: () =>
    queryOptions({
      queryKey: notesKeys.lists(),
      queryFn: async () => {
        const response = await getNotes();
        return Array.isArray(response.notes) ? response.notes : [];
      },
      staleTime: 30_000,
    }),
};

export function useCreateNote() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ title, content }: { title: string; content: string }) =>
      createNote({ title, content }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notesKeys.lists() });
      toast.success('Note created');
    },

    onError: () => {
      toast.error('Failed to create note');
    },
  });
}

export function useUpdateNote() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, title, content }: { id: number; title: string; content: string }) =>
      updateNote(id, { title, content }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notesKeys.lists() });
      toast.success('Note updated');
    },

    onError: () => {
      toast.error('Failed to update note');
    },
  });
}

export function useDeleteNote() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteNote(id),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notesKeys.lists() });
      toast.success('Note deleted');
    },

    onError: () => {
      toast.error('Failed to delete note');
    },
  });
}
