import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import {
  getNotes,
  createNote,
  getNoteById,
  updateNote,
  deleteNote,
} from '../../services/notes/notes/notes';

describe('notes service', () => {
  it('getNotes sends GET with userId query param', async () => {
    const notes = await getNotes({ userId: 1 });
    expect(Array.isArray(notes)).toBe(true);
    expect(notes.length).toBeGreaterThan(0);
    expect(notes[0]).toHaveProperty('title');
  });

  it('createNote sends POST and returns created note', async () => {
    const note = await createNote({ title: 'New Note', content: 'Content' });
    expect(note).toHaveProperty('id');
    expect(note.title).toBe('New Note');
  });

  it('getNoteById sends GET with path param', async () => {
    const note = await getNoteById(1);
    expect(note).toHaveProperty('id', 1);
    expect(note).toHaveProperty('title');
  });

  it('updateNote sends PUT with path param and body', async () => {
    const note = await updateNote(1, { title: 'Updated', content: 'Updated Content' });
    expect(note).toHaveProperty('id', 1);
    expect(note.title).toBe('Updated');
  });

  it('deleteNote sends DELETE with path param', async () => {
    await expect(deleteNote(1)).resolves.toBeUndefined();
  });

  it('getNotes throws on 500 server error', async () => {
    server.use(
      http.get('*/api/v1/notes', () => HttpResponse.json({ message: 'Server error' }, { status: 500 })),
    );
    await expect(getNotes({ userId: 1 })).rejects.toThrow();
  });

  it('getNoteById throws on 404 not found', async () => {
    server.use(
      http.get('*/api/v1/notes/:id', () => HttpResponse.json(null, { status: 404 })),
    );
    await expect(getNoteById(999)).rejects.toThrow();
  });
});
