import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Suspense } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { notesKeys } from '#/lib/queries/notes.ts';
import { checklistKeys } from '#/lib/queries/checklists.ts';
import { calendarKeys } from '#/lib/queries/calendar.ts';
import type { Note } from '#/types/notes';
import type { Checklist } from '#/types/checklist';
import type { IdentifiedCalendarEvent } from '#/types/calendar';
import { Home } from '#/routes/_authenticated';

// useSearch and useRouter require a RouterProvider context; mock them for isolated tests
vi.mock('@tanstack/react-router', async () => {
  const actual = await vi.importActual('@tanstack/react-router');
  return {
    ...actual,
    useSearch: () => ({ action: undefined, type: undefined, detailId: undefined }),
    useRouter: () => ({ navigate: vi.fn() }),
    useNavigate: () => vi.fn(),
  };
});

// ── Test helpers ───────────────────────────────────────────────

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderDashboard(queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <Suspense fallback={<div>Loading...</div>}>
        <Home />
      </Suspense>
    </QueryClientProvider>,
  );
}

const mockNote: Note = {
  id: 1,
  title: 'Test Note',
  content: 'This is a test note body',
  createdAt: '2024-06-15T10:00:00Z',
  lastUpdatedAt: '2024-06-15T10:00:00Z',
};

const mockChecklist: Checklist = {
  id: 2,
  userId: 1,
  title: 'Test Checklist',
  createdAt: '2024-06-14T10:00:00Z',
  items: [
    { id: 1, text: 'Item 1', completed: true, position: 0 },
    { id: 2, text: 'Item 2', completed: false, position: 1 },
  ],
};

const mockEvent: IdentifiedCalendarEvent = {
  id: 1,
  title: 'Team standup',
  startTime: new Date(Date.now() + 3600000).toISOString(),
  endTime: new Date(Date.now() + 7200000).toISOString(),
  description: 'Daily sync',
};

function seedDefaultData(queryClient: QueryClient) {
  queryClient.setQueryData(notesKeys.lists(), [mockNote]);
  queryClient.setQueryData(checklistKeys.lists(), [mockChecklist]);
  queryClient.setQueryData(calendarKeys.events(), [mockEvent]);
}

// ── Tests ──────────────────────────────────────────────────────

describe('dashboard page — happy path', () => {
  it('renders greeting, stat cards, events, and notes from API', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderDashboard(queryClient);

    // Greeting
    expect(await screen.findByText(/Good (morning|afternoon|evening)/)).toBeInTheDocument();

    // Stat cards with real counts
    expect(await screen.findByText('Total Notes')).toBeInTheDocument();
    expect(await screen.findByText('Tasks Complete')).toBeInTheDocument();
    expect(await screen.findByText('50%')).toBeInTheDocument(); // Tasks Complete (1/2 = 50%)
    // "Upcoming Events" appears in both stat cards label and widget title
    const upcomingTexts = screen.getAllByText('Upcoming Events');
    expect(upcomingTexts.length).toBeGreaterThanOrEqual(1);

    // Quick action buttons
    expect(screen.getByText('New Note')).toBeInTheDocument();
    expect(screen.getByText('Add Event')).toBeInTheDocument();
    expect(screen.getByText('New Task')).toBeInTheDocument();
    expect(screen.getByText('Ask AI')).toBeInTheDocument();

    // Events widget
    expect(await screen.findByText('Team standup')).toBeInTheDocument();

    // Notes widget
    expect(await screen.findByText('Test Note')).toBeInTheDocument();
    expect(await screen.findByText('Test Checklist')).toBeInTheDocument();
  });
});

describe('dashboard page — empty state', () => {
  it('shows empty states when no items exist', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(notesKeys.lists(), []);
    queryClient.setQueryData(checklistKeys.lists(), []);
    queryClient.setQueryData(calendarKeys.events(), []);
    renderDashboard(queryClient);

    expect(await screen.findByText('No upcoming events')).toBeInTheDocument();
    expect(await screen.findByText('No notes yet')).toBeInTheDocument();
  });
});
