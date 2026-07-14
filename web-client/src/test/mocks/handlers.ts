import { http, HttpResponse } from 'msw';

const API_BASE = '';

export const handlers = [
  // ── User Service ────────────────────────────────────────────
  http.post(`${API_BASE}/api/v1/users/auth/register`, () =>
    new HttpResponse(null, { status: 201 }),
  ),
  http.post(`${API_BASE}/api/v1/users/auth/login`, () =>
    HttpResponse.json({ token: 'mock-jwt-token' }),
  ),
  http.get(`${API_BASE}/api/v1/users/auth/check-token`, () =>
    new HttpResponse(null, { status: 200 }),
  ),

  // ── Note Service ────────────────────────────────────────────
  http.get(`${API_BASE}/api/v1/notes`, () =>
    HttpResponse.json({
      notes: [
        { id: 1, title: 'Test Note', content: 'Content', createdAt: '2024-01-01T00:00:00Z', lastUpdatedAt: '2024-01-01T00:00:00Z' },
      ],
    }),
  ),
  http.post(`${API_BASE}/api/v1/notes`, () =>
    HttpResponse.json({ id: 2, title: 'New Note', content: 'New', createdAt: '2024-01-01T00:00:00Z', lastUpdatedAt: '2024-01-01T00:00:00Z' }, { status: 201 }),
  ),
  http.get(`${API_BASE}/api/v1/notes/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Test Note', content: 'Content', createdAt: '2024-01-01T00:00:00Z', lastUpdatedAt: '2024-01-01T00:00:00Z' }),
  ),
  http.put(`${API_BASE}/api/v1/notes/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Updated', content: 'Updated Content', createdAt: '2024-01-01T00:00:00Z', lastUpdatedAt: '2024-01-01T00:00:00Z' }),
  ),
  http.delete(`${API_BASE}/api/v1/notes/:id`, () => new HttpResponse(null, { status: 204 })),

  // ── Checklist Service ───────────────────────────────────────
  http.get(`${API_BASE}/api/v1/checklists`, () =>
    HttpResponse.json({
      checklists: [
        { id: 1, title: 'Test Checklist', createdAt: '2024-01-01T00:00:00Z', items: [] },
      ],
    }),
  ),
  http.post(`${API_BASE}/api/v1/checklists`, () =>
    HttpResponse.json({ id: 2, title: 'New Checklist', createdAt: '2024-01-01T00:00:00Z', items: [] }, { status: 201 }),
  ),
  http.get(`${API_BASE}/api/v1/checklists/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Test Checklist', createdAt: '2024-01-01T00:00:00Z', items: [] }),
  ),
  http.put(`${API_BASE}/api/v1/checklists/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Updated Checklist', createdAt: '2024-01-01T00:00:00Z', items: [] }),
  ),
  http.delete(`${API_BASE}/api/v1/checklists/:id`, () => new HttpResponse(null, { status: 204 })),
  http.post(`${API_BASE}/api/v1/checklists/:id/items`, () =>
    HttpResponse.json({ id: 1, text: 'New Item', completed: false, position: 0 }, { status: 201 }),
  ),
  http.put(`${API_BASE}/api/v1/checklists/:id/items/:itemId`, () =>
    HttpResponse.json({ id: 1, text: 'Updated Item', completed: true, position: 0 }),
  ),
  http.delete(`${API_BASE}/api/v1/checklists/:id/items/:itemId`, () => new HttpResponse(null, { status: 204 })),

  // ── Calendar Service ────────────────────────────────────────
  http.get(`${API_BASE}/api/v1/events`, () =>
    HttpResponse.json({
      events: [
        { id: 1, title: 'Test Event', description: 'Desc', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T01:00:00Z', location: 'Room 1' },
      ],
    }),
  ),
  http.post(`${API_BASE}/api/v1/events`, () =>
    HttpResponse.json({ id: 2, title: 'New Event', description: '', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T01:00:00Z', location: '' }, { status: 201 }),
  ),
  http.get(`${API_BASE}/api/v1/events/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Test Event', description: 'Desc', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T01:00:00Z', location: 'Room 1' }),
  ),
  http.put(`${API_BASE}/api/v1/events/:id`, ({ params }) =>
    HttpResponse.json({ id: Number(params.id), title: 'Updated Event', description: 'Updated', startTime: '2024-01-01T00:00:00Z', endTime: '2024-01-01T02:00:00Z', location: 'Room 2' }),
  ),
  http.delete(`${API_BASE}/api/v1/events/:id`, () => new HttpResponse(null, { status: 204 })),

  // ── GenAI Service ──────────────────────────────────────────
  http.get(`${API_BASE}/api/v1/health`, () =>
    HttpResponse.json({ status: 'ok' }),
  ),
  http.post(`${API_BASE}/api/v1/conversations`, () =>
    HttpResponse.json({ id: 1, user_id: 1, title: 'New Chat', created_at: '2024-01-01T00:00:00Z', messages: [] }),
  ),
  http.get(`${API_BASE}/api/v1/conversations/:conversationId`, ({ params }) =>
    HttpResponse.json({ id: Number(params.conversationId), user_id: 1, title: 'Test Chat', created_at: '2024-01-01T00:00:00Z', messages: [] }),
  ),
  http.delete(`${API_BASE}/api/v1/conversations/:conversationId`, () =>
    HttpResponse.json({ message: 'Conversation deleted' }),
  ),
  http.post(`${API_BASE}/api/v1/chat`, () =>
    HttpResponse.json({ response: 'AI response text', conversation_id: 1 }),
  ),
];
