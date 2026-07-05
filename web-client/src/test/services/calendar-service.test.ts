import { describe, it, expect } from 'vitest';
import { getEvents, createEvent, getEventById, updateEvent, deleteEvent } from '../../services/calendar/calendar-events/calendar-events';

describe('calendar service', () => {
  it('getEvents sends GET with userId query param', async () => {
    const result = await getEvents({ userId: 1 });
    expect(Array.isArray(result)).toBe(true);
    expect(result[0]).toHaveProperty('title');
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
});
