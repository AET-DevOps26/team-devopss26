import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  getNotes,
  createNote,
  updateNote,
  deleteNote,
} from '#/services/notes/notes/notes';
/** Cache key factory.
 * `all` → root key. `lists()` → note list. `list(filters)` → filtered list (reserved).
 */
export const notesKeys = {
  all: ['notes'] as const,
  lists: () => [...notesKeys.all, 'list'] as const,
  list: (filters: string) => [...notesKeys.lists(), filters] as const,
};

/** Pre-configured query options for fetching all notes.
 * Guard clause defaults non-array response to empty array. 30s staleTime.
 */
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

/** Mutation hook: create a new note. No optimistic update.
 * Invalidates `notesKeys.lists()` on success.
 *
 * @param title - Note headline
 * @param content - Note body (supports markdown)
 */
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

/** Mutation hook: update an existing note. Sends full payload (both fields required).
 * Invalidates `notesKeys.lists()` on success.
 *
 * @param id - Note ID to update
 * @param title - Updated headline
 * @param content - Updated body (supports markdown)
 */
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

/** Mutation hook: delete a note. No optimistic removal.
 * Invalidates `notesKeys.lists()` on success.
 *
 * @param id - Note ID to delete
 */
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
