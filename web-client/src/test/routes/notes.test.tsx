import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Suspense } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NotesPage } from '#/routes/_authenticated/notes';
import { notesKeys } from '#/lib/queries/notes.ts';
import { checklistKeys } from '#/lib/queries/checklists.ts';
import type { Note } from '#/types/notes';
import type { Checklist } from '#/types/checklist';

// ── Test helpers ───────────────────────────────────────────────

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderNotesPage(queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <Suspense fallback={<div>Loading...</div>}>
        <NotesPage />
      </Suspense>
    </QueryClientProvider>,
  );
}

const mockNote: Note = {
  id: 1,
  title: 'Test Note',
  content: 'This is a test note body',
  createdAt: '2024-01-01T00:00:00Z',
  lastUpdatedAt: '2024-01-01T00:00:00Z',
};

const mockChecklist: Checklist = {
  id: 2,
  userId: 1,
  title: 'Test Checklist',
  createdAt: '2024-01-02T00:00:00Z',
  items: [
    { id: 1, text: 'Item 1', completed: false, position: 0 },
    { id: 2, text: 'Item 2', completed: true, position: 1 },
  ],
};

function seedDefaultData(queryClient: QueryClient) {
  queryClient.setQueryData(notesKeys.lists(), [mockNote]);
  queryClient.setQueryData(checklistKeys.lists(), [mockChecklist]);
}

beforeEach(() => {
  // Clear any lingering state between tests
});

describe('notes page — happy path', () => {
  it('renders notes and checklists from API', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    expect(await screen.findByText('Test Note')).toBeInTheDocument();
    expect(await screen.findByText('Test Checklist')).toBeInTheDocument();
    expect(await screen.findByText('Note')).toBeInTheDocument();
    expect(await screen.findByText('Checklist')).toBeInTheDocument();
  });

  it('shows checklist progress on cards', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    expect(await screen.findByText('1/2 tasks completed')).toBeInTheDocument();
  });

  it('shows note body preview on cards', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    // Note body text should appear as preview
    expect(await screen.findByText(/test note body/i)).toBeInTheDocument();
  });
});

describe('notes page — empty state', () => {
  it('shows empty state when no items exist', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setQueryData(notesKeys.lists(), []);
    queryClient.setQueryData(checklistKeys.lists(), []);
    renderNotesPage(queryClient);

    expect(await screen.findByText('No notes yet')).toBeInTheDocument();
    expect(await screen.findByText('Create your first note or checklist to get started.')).toBeInTheDocument();
    // There are two "New Note" buttons: toolbar + empty state CTA
    const newNoteButtons = screen.queryAllByText('New Note');
    expect(newNoteButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('shows "no matching" state when filter excludes everything', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    // Type a search that won't match anything
    const searchInput = await screen.findByPlaceholderText('Search notes...');
    const user = userEvent.setup();
    await user.type(searchInput, 'zzzzz_nonexistent');

    expect(await screen.findByText('No matching notes')).toBeInTheDocument();
    expect(await screen.findByText('Try adjusting your search or filter.')).toBeInTheDocument();
  });
});

describe('notes page — search filters', () => {
  it('filters by title across both types', async () => {
    const queryClient = createTestQueryClient();

    // Add a second note with distinct title
    const secondNote: Note = {
      id: 3,
      title: 'Special Report',
      content: 'Content about something',
      createdAt: '2024-01-03T00:00:00Z',
      lastUpdatedAt: '2024-01-03T00:00:00Z',
    };

    const secondChecklist: Checklist = {
      id: 4,
      userId: 1,
      title: 'Special Tasks',
      createdAt: '2024-01-04T00:00:00Z',
      items: [],
    };

    queryClient.setQueryData(notesKeys.lists(), [mockNote, secondNote]);
    queryClient.setQueryData(checklistKeys.lists(), [mockChecklist, secondChecklist]);
    renderNotesPage(queryClient);

    // Initially all items visible
    expect(await screen.findByText('Test Note')).toBeInTheDocument();
    expect(await screen.findByText('Test Checklist')).toBeInTheDocument();
    expect(await screen.findByText('Special Report')).toBeInTheDocument();
    expect(await screen.findByText('Special Tasks')).toBeInTheDocument();

    // Search for "special"
    const searchInput = await screen.findByPlaceholderText('Search notes...');
    const user = userEvent.setup();
    await user.type(searchInput, 'special');

    // Should match both Special Report and Special Tasks
    expect(await screen.findByText('Special Report')).toBeInTheDocument();
    expect(await screen.findByText('Special Tasks')).toBeInTheDocument();

    // Should NOT show Test Note or Test Checklist (no "special" in title/body)
    expect(screen.queryByText('Test Note')).not.toBeInTheDocument();
    expect(screen.queryByText('Test Checklist')).not.toBeInTheDocument();
  });
});

describe('notes page — type filter', () => {
  it('shows only notes when type filter is set to notes', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    await screen.findByText('Test Note');
    await screen.findByText('Test Checklist');

    // Select "Notes" filter — click trigger, then wait for popover, then click the option
    const selectTrigger = screen.getByRole('combobox');
    const user = userEvent.setup();
    await user.click(selectTrigger);
    const listbox = await screen.findByRole('listbox');
    const notesOption = await within(listbox).findByText('Notes');
    await user.click(notesOption);

    // Only notes should remain
    expect(await screen.findByText('Test Note')).toBeInTheDocument();
    expect(screen.queryByText('Test Checklist')).not.toBeInTheDocument();
  });

  it('shows only checklists when type filter is set to checklists', async () => {
    const queryClient = createTestQueryClient();
    seedDefaultData(queryClient);
    renderNotesPage(queryClient);

    await screen.findByText('Test Note');
    await screen.findByText('Test Checklist');

    // Select "Checklists" filter
    const selectTrigger = screen.getByRole('combobox');
    const user = userEvent.setup();
    await user.click(selectTrigger);
    const listbox2 = await screen.findByRole('listbox');
    const checklistsOption = await within(listbox2).findByText('Checklists');
    await user.click(checklistsOption);

    // Only checklists should remain
    expect(await screen.findByText('Test Checklist')).toBeInTheDocument();
    expect(screen.queryByText('Test Note')).not.toBeInTheDocument();
  });
});
