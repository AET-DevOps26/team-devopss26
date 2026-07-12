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
import type { CreateChecklistRequest, UpdateChecklistRequest, AddChecklistItemRequest, UpdateChecklistItemRequest } from '#/types/checklist';
import { useAuthStore } from '#/stores/authStore';

export const checklistKeys = {
  all: ['checklists'] as const,
  lists: () => [...checklistKeys.all, 'list'] as const,
  list: (filters: string) => [...checklistKeys.lists(), filters] as const,
};

export const checklistQueries = {
  all: () =>
    queryOptions({
      queryKey: checklistKeys.lists(),
      queryFn: async () => {
        const userId = useAuthStore.getState().userId;
        const response = await getChecklists({ userId: userId! });
        return response.checklists ?? [];
      },
      staleTime: 30_000,
    }),
};

export function useCreateChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: { title: string }) => {
      const userId = useAuthStore.getState().userId;
      return createChecklist({ userId: userId!, title: data.title } as CreateChecklistRequest);
    },

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist created');
    },

    onError: () => {
      toast.error('Failed to create checklist');
    },
  });
}

export function useUpdateChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) =>
      updateChecklist(id, { title } as UpdateChecklistRequest),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist updated');
    },

    onError: () => {
      toast.error('Failed to update checklist');
    },
  });
}

export function useDeleteChecklist() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteChecklist(id),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Checklist deleted');
    },

    onError: () => {
      toast.error('Failed to delete checklist');
    },
  });
}

export function useAddChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, text }: { checklistId: number; text: string }) =>
      addChecklistItem(checklistId, { text } as AddChecklistItemRequest),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Item added');
    },

    onError: () => {
      toast.error('Failed to add item');
    },
  });
}

export function useUpdateChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, itemId, completed, text }: { checklistId: number; itemId: number; completed?: boolean; text?: string }) =>
      updateChecklistItem(checklistId, itemId, { completed, text } as UpdateChecklistItemRequest),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
    },

    onError: () => {
      toast.error('Failed to update item');
    },
  });
}

export function useDeleteChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ checklistId, itemId }: { checklistId: number; itemId: number }) =>
      deleteChecklistItem(checklistId, itemId),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: checklistKeys.lists() });
      toast.success('Item deleted');
    },

    onError: () => {
      toast.error('Failed to delete item');
    },
  });
}
