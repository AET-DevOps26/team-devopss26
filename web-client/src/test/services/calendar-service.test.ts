import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import { getEvents, createEvent, getEventById, updateEvent, deleteEvent } from '#/services/calendar/calendar-events/calendar-events.ts';

describe('calendar service', () => {
  it('getEvents sends GET and returns list of events', async () => {
    const result = await getEvents();
    expect(Array.isArray(result.events)).toBe(true);
    expect(result.events[0]).toHaveProperty('title');
  });

  it('createEvent sends POST and returns created event', async () => {
    const result = await createEvent({ title: 'New Event', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T01:00:00Z' });
    expect(result).toHaveProperty('id');
  });

  it('getEventById sends GET with path param', async () => {
    const result = await getEventById(1);
    expect(result).toHaveProperty('id', 1);
  });

  it('updateEvent sends PUT with path param and body', async () => {
    const result = await updateEvent(1, { title: 'Updated Event', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T02:00:00Z' });
    expect(result).toHaveProperty('id', 1);
  });

  it('deleteEvent sends DELETE with path param', async () => {
    await expect(deleteEvent(1)).resolves.toBeUndefined();
  });

  it('getEvents throws on 500', async () => {
    server.use(
      http.get('*/api/v1/events', () => HttpResponse.json(null, { status: 500 })),
    );
    await expect(getEvents()).rejects.toThrow();
  });
});
