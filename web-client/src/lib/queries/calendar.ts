import {
  queryOptions,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  getEvents,
  createEvent,
  updateEvent,
  deleteEvent,
} from '#/services/calendar/calendar-events/calendar-events.ts';
import type {
  IdentifiedCalendarEvent,
  CalendarEvent,
} from '#/types/calendar';
import type { CalendarFormEvent } from '../utils/calendar';
import { toApiEvent } from '../utils/calendar';

export const calendarKeys = {
  all: ['calendar'] as const,
  events: () => [...calendarKeys.all, 'events'] as const,
  event: (id: number) => [...calendarKeys.events(), id] as const,
};

export const calendarQueries = {
  all: () =>
    queryOptions({
      queryKey: calendarKeys.events(),
      queryFn: async () => {
        const response = await getEvents();
        return response.events;
      },
      staleTime: 30_000,
    }),
};

export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (form: CalendarFormEvent) => {
      const apiPayload = toApiEvent(form);
      return createEvent(apiPayload);
    },

    onMutate: async (form) => {
      await queryClient.cancelQueries({ queryKey: calendarKeys.events() });
      const previousEvents = queryClient.getQueryData<IdentifiedCalendarEvent[]>(
        calendarKeys.events(),
      );

      const optimisticEvent: IdentifiedCalendarEvent = {
        id: -Date.now(),
        title: form.title,
        startTime: `${form.date}T${form.startTime}:00Z`,
        endTime: `${form.date}T${form.endTime}:00Z`,
        description: form.description,
      };

      queryClient.setQueryData<IdentifiedCalendarEvent[]>(
        calendarKeys.events(),
        (old) => [...(old ?? []), optimisticEvent],
      );

      return { previousEvents };
    },

    onError: (_err, _form, context) => {
      if (context?.previousEvents) {
        queryClient.setQueryData(calendarKeys.events(), context.previousEvents);
      }
      toast.error('Failed to create event');
    },

    onSuccess: () => {
      toast.success('Event created');
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });
}

export function useUpdateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, form }: { id: number; form: CalendarFormEvent }) => {
      const apiPayload: CalendarEvent = toApiEvent(form);
      return updateEvent(id, apiPayload);
    },

    onError: () => {
      toast.error('Failed to update event');
    },

    onSuccess: () => {
      toast.success('Event updated');
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });
}

export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteEvent(id),

    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: calendarKeys.events() });
      const previousEvents = queryClient.getQueryData<IdentifiedCalendarEvent[]>(
        calendarKeys.events(),
      );

      queryClient.setQueryData<IdentifiedCalendarEvent[]>(
        calendarKeys.events(),
        (old) => old?.filter((event) => event.id !== id) ?? [],
      );

      return { previousEvents };
    },

    onError: (_err, _id, context) => {
      if (context?.previousEvents) {
        queryClient.setQueryData(calendarKeys.events(), context.previousEvents);
      }
      toast.error('Failed to delete event');
    },

    onSuccess: () => {
      toast.success('Event deleted');
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: calendarKeys.events() });
    },
  });
}
