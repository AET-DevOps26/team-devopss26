import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '../setup';
import {
  getChecklists,
  createChecklist,
  getChecklistById,
  updateChecklist,
  deleteChecklist,
  addChecklistItem,
  updateChecklistItem,
  deleteChecklistItem,
} from '#/services/checklist/checklists/checklists.ts';

describe('checklist service', () => {
  it('getChecklists sends GET', async () => {
    const result = await getChecklists();
    expect(result.checklists).toBeDefined();
    expect(Array.isArray(result.checklists)).toBe(true);
    if (result.checklists) {
      expect(result.checklists[0]).toHaveProperty('title');
    }
  });

  it('createChecklist sends POST and returns created checklist', async () => {
    const result = await createChecklist({ title: 'New Checklist' });
    expect(result).toHaveProperty('id');
    expect(result.title).toBe('New Checklist');
  });

  it('getChecklistById sends GET with path param', async () => {
    const result = await getChecklistById(1);
    expect(result).toHaveProperty('id', 1);
  });

  it('updateChecklist sends PUT with path param and body', async () => {
    const result = await updateChecklist(1, { title: 'Updated' });
    expect(result).toHaveProperty('id', 1);
  });

  it('deleteChecklist sends DELETE with path param', async () => {
    await expect(deleteChecklist(1)).resolves.toBeUndefined();
  });

  it('addChecklistItem sends POST with checklist id and item body', async () => {
    const result = await addChecklistItem(1, { text: 'New Item', completed: false });
    expect(result).toHaveProperty('id');
    expect(result).toHaveProperty('text');
  });

  it('updateChecklistItem sends PUT with ids and item body', async () => {
    const result = await updateChecklistItem(1, 1, { text: 'Updated Item', completed: true });
    expect(result).toHaveProperty('id');
  });

  it('deleteChecklistItem sends DELETE with ids', async () => {
    await expect(deleteChecklistItem(1, 1)).resolves.toBeUndefined();
  });

  it('getChecklists throws on 500', async () => {
    server.use(
      http.get('*/api/v1/checklists', () => HttpResponse.json(null, { status: 500 })),
    );
    await expect(getChecklists()).rejects.toThrow();
  });

  it('addChecklistItem throws on 404 for missing checklist', async () => {
    server.use(
      http.post('*/api/v1/checklists/:id/items', () => HttpResponse.json(null, { status: 404 })),
    );
    await expect(addChecklistItem(999, { text: 'Item' })).rejects.toThrow();
  });
});
