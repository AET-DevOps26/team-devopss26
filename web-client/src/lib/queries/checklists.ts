import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  getChecklists,
  createChecklist,
  updateChecklist,
  deleteChecklist,
  addChecklistItem,
  updateChecklistItem,
  deleteChecklistItem,
} from '#/services/checklist/checklists/checklists';

/** Cache key factory.
 * `all` → root key. `lists()` → checklist list. `list(filters)` → scoped view (reserved).
 */
export const checklistKeys = {
  all: ['checklists'] as const,
  lists: () => [...checklistKeys.all, 'list'] as const,
  list: (filters: string) => [...checklistKeys.lists(), filters] as const,
};


/** Pre-configured query options for fetching all checklists.
 * Guard clause defaults non-array response to empty array. 30s staleTime.
 */
export const checklistQueries = {
  all: () =>
    queryOptions({
      queryKey: checklistKeys.lists(),
      queryFn: async () => {
        const response = await getChecklists();
        return Array.isArray(response.checklists) ? response.checklists : [];
      },
      staleTime: 30_000,
    }),
};

/** Mutation hook: create a new checklist. No optimistic update.
 * Invalidates `checklistKeys.lists()` on success.
 *
 * @param title - Display name for the new checklist
 */
export function useCreateChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { title: string }) => {
      return createChecklist({ title: data.title });
    },

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist created');
    },

    onError: () => {
      toast.error('Failed to create checklist');
    },
  });
}

/** Mutation hook: rename an existing checklist. No optimistic update.
 * Invalidates `checklistKeys.lists()` on success.
 *
 * @param id - Checklist ID to update
 * @param title - New display name for the checklist
 */
export function useUpdateChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) =>
      updateChecklist(id, { title }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist updated');
    },

    onError: () => {
      toast.error('Failed to update checklist');
    },
  });
}

/** Mutation hook: delete a checklist (server cascades to items). No optimistic removal.
 * Invalidates `checklistKeys.lists()` on success.
 *
 * @param id - Checklist ID to delete
 */
export function useDeleteChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteChecklist(id),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist deleted');
    },

    onError: () => {
      toast.error('Failed to delete checklist');
    },
  });
}

/** Mutation hook: add a new item to a checklist.
 * Invalidates `checklistKeys.lists()` on success.
 *
 * @param checklistId - Parent checklist ID
 * @param text - Item description text
 */
export function useAddChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, text }: { checklistId: number; text: string }) =>
      addChecklistItem(checklistId, { text }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Item added');
    },

    onError: () => {
      toast.error('Failed to add item');
    },
  });
}

/** Mutation hook: toggle completion or edit text of a checklist item.
 * Both params optional — send only the field being updated. No success toast
 * (avoids spam when toggling many items). Invalidates `checklistKeys.lists()` on success.
 *
 * @param checklistId - Parent checklist ID
 * @param itemId - Item ID to update
 * @param completed - New completion state (omit if not changing)
 * @param text - New item text (omit if not changing)
 */
export function useUpdateChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, itemId, completed, text }: { checklistId: number; itemId: number; completed?: boolean; text?: string }) =>
      updateChecklistItem(checklistId, itemId, { completed, text }),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
    },

    onError: () => {
      toast.error('Failed to update item');
    },
  });
}

/** Mutation hook: delete a single item from a checklist. No cascade.
 * Invalidates `checklistKeys.lists()` on success.
 *
 * @param checklistId - Parent checklist ID
 * @param itemId - Item ID to remove
 */
export function useDeleteChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, itemId }: { checklistId: number; itemId: number }) =>
      deleteChecklistItem(checklistId, itemId),

    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Item deleted');
    },

    onError: () => {
      toast.error('Failed to delete item');
    },
  });
}
