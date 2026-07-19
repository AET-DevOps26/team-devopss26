import { useState, useMemo, useCallback } from 'react';
import { createFileRoute, useSearch, useRouter } from '@tanstack/react-router';
import { useSuspenseQuery, useQueryErrorResetBoundary } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '#/components/ui/card.tsx';
import { Button } from '#/components/ui/button.tsx';
import { Input } from '#/components/ui/input.tsx';
import { Badge } from '#/components/ui/badge.tsx';
import { Textarea } from '#/components/ui/textarea.tsx';
import { Checkbox } from '#/components/ui/checkbox.tsx';
import { Progress, ProgressLabel, ProgressValue } from '#/components/ui/progress.tsx';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from '#/components/ui/dialog.tsx';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '#/components/ui/select.tsx';
import { Empty, EmptyTitle, EmptyDescription, EmptyMedia, EmptyContent } from '#/components/ui/empty.tsx';
import {
  SearchIcon,
  FileTextIcon,
  ListChecksIcon,
  PlusIcon,
  Trash2Icon,
  PencilIcon,
  ArrowLeftIcon,
  CheckIcon,
  XIcon,
  AlertCircleIcon,
} from 'lucide-react';
import { notesQueries, useCreateNote, useUpdateNote, useDeleteNote } from '#/lib/queries/notes.ts';
import {
  checklistQueries,
  useCreateChecklist,
  useUpdateChecklist,
  useDeleteChecklist,
  useAddChecklistItem,
  useUpdateChecklistItem,
  useDeleteChecklistItem,
} from '#/lib/queries/checklists.ts';
import type { IdentifiedTimestampedNote as ApiNote } from '#/types/notes';
import type { IdentifiedChecklist as ApiChecklist } from '#/types/checklist';

// ── Types ──────────────────────────────────────────────────────

/** Union of the two supported content types — notes (free text) and checklists (task list). */
type NoteType = 'note' | 'checklist';

/**
 * A single item within a checklist.
 * `id` can be a **number** (persisted via API, used for toggling completion) or a
 * **string** (locally generated via `crypto.randomUUID()` for items not yet saved).
 * The form uses this distinction: numeric IDs map to API items, string IDs are new
 * items that will be created via `addChecklistItem`.
 */
interface ChecklistItem {
  id: string | number;
  text: string;
  done: boolean;
}

/**
 * Unified display model that merges the API shapes for notes and checklists.
 * Both types share `id`, `title`, and timestamps, but differ in body content
 * (a note has `body`, a checklist has `checklist` items). This indirection lets
 * the list/detail views handle both types polymorphically without runtime type checks
 * on the API response.
 */
interface DisplayNote {
  id: number;
  title: string;
  body: string;
  type: NoteType;
  checklist: ChecklistItem[];
  createdAt: string;
  updatedAt: string;
}

/**
 * The page supports four mutually exclusive views managed by a single state variable:
 * - `'list'` — the grid/search/filter overview.
 * - `'detail'` — full view of a single note or checklist.
 * - `'create'` — the form to create a new note/checklist.
 * - `'edit'` — the form to edit an existing note/checklist.
 */
type ViewMode = 'list' | 'detail' | 'create' | 'edit';

// ── Search params ──────────────────────────────────────────────

interface NotesSearch {
  action?: 'create';
  type?: 'note' | 'checklist';
  detailId?: string;
  detailType?: 'note' | 'checklist';
}

interface NotesSearch {
  action?: 'create';
  type?: 'note' | 'checklist';
  detailId?: string;
  detailType?: 'note' | 'checklist';
}

function NotesPageWithKey() {
  const search: NotesSearch = useSearch({ from: '/_authenticated/notes/' });
  return <NotesPage key={`${search.action ?? ''}-${search.detailId ?? ''}-${search.detailType ?? ''}`} />;
}

/**
 * Displays a searchable grid of notes and checklists with full CRUD.
 *
 * Data loading: The `loader` prefetches both notes and checklists in parallel
 * via `ensureQueryData`.
 */
export const Route = createFileRoute('/_authenticated/notes/')({
  validateSearch: (input: Record<string, unknown>): NotesSearch => ({
    action: input.action === 'create' ? 'create' : undefined,
    type: input.type === 'checklist' ? 'checklist' : input.type === 'note' ? 'note' : undefined,
    detailId: typeof input.detailId === 'string' ? input.detailId : undefined,
    detailType: input.detailType === 'checklist' ? 'checklist' : input.detailType === 'note' ? 'note' : undefined,
  }),
  loader: async ({ context: { queryClient } }) => {
    await Promise.all([
      queryClient.ensureQueryData(notesQueries.all()),
      queryClient.ensureQueryData(checklistQueries.all()),
    ]);
  },
  pendingComponent: NotesSkeleton,
  errorComponent: NotesError,
  component: NotesPageWithKey,
});

// ── Helpers ─────────────────────────────────────────────────────

/**
 * ISO 8601 → human-readable date. Defensively handles null/undefined and
 * invalid dates by returning input as-is.
 */
function formatDate(isoString: string | undefined | null): string {
  if (!isoString) return '';
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString;
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return isoString;
  }
}

/**
 * API note → unified `DisplayNote` shape. Notes have no checklist items.
 */
function fromApiNote(note: ApiNote): DisplayNote {
  return {
    id: note.id,
    title: note.title,
    body: note.content,
    type: 'note',
    checklist: [],
    createdAt: note.createdAt,
    updatedAt: note.lastUpdatedAt,
  };
}

/**
 * API checklist → unified `DisplayNote` shape. Maps `completed` to `done`.
 */
function fromApiChecklist(checklist: ApiChecklist): DisplayNote {
  return {
    id: (checklist as { id?: number }).id ?? 0,
    title: checklist.title ?? '',
    body: '',
    type: 'checklist',
    checklist: (checklist.items ?? []).map((item) => ({
      id: item.id ?? 0,
      text: item.text ?? '',
      done: item.completed ?? false,
    })),
    createdAt: checklist.createdAt ?? '',
    updatedAt: checklist.createdAt ?? '',
  };
}

/**
 * Blank `DisplayNote` for the create-form initial state.
 * Uses `id: 0` as sentinel for "unsaved".
 */
function emptyDisplayNote(): DisplayNote {
  return {
    id: 0,
    title: '',
    body: '',
    type: 'note',
    checklist: [],
    createdAt: '',
    updatedAt: '',
  };
}

// ── Skeleton ────────────────────────────────────────────────────

/**
 * Loading skeleton mirroring the notes grid layout (toolbar + 6 card placeholders).
 */
function NotesSkeleton() {
  return (
    <div className="p-4 sm:p-6 lg:p-8" aria-busy="true" aria-label="Loading notes">
      <div className="flex items-center justify-between mb-6">
        <Skeleton className="h-9 w-24" />
        <Skeleton className="h-9 w-24" />
      </div>
      <div className="flex items-center gap-3 mb-6">
        <Skeleton className="h-9 flex-1 min-w-[200px]" />
        <Skeleton className="h-9 w-[130px]" />
        <Skeleton className="h-9 w-24" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Card key={i}>
            <CardHeader>
              <div className="flex items-start justify-between gap-2">
                <Skeleton className="h-5 flex-1" />
                <Skeleton className="h-5 w-20 shrink-0" />
              </div>
            </CardHeader>
            <CardContent>
              <Skeleton className="h-4 w-full mb-2" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-3 w-20 mt-2" />
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

// ── Error component ────────────────────────────────────────────

/**
 * Error-state fallback. Since the loader fetches two queries in parallel,
 * a failure in either surfaces here. Retry resets both error boundaries.
 */
function NotesError({ error, reset }: { error: Error; reset: () => void }) {
  const { reset: resetQuery } = useQueryErrorResetBoundary();

  const handleRetry = () => {
    resetQuery();
    reset();
  };

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center" role="alert">
      <AlertCircleIcon className="size-10 text-destructive" />
      <h2 className="text-xl font-bold tracking-tight">Failed to load notes</h2>
      <p className="max-w-sm text-sm text-muted-foreground">
        {error.message || 'Something went wrong while loading your notes and checklists.'}
      </p>
      <Button onClick={handleRetry}>Try Again</Button>
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────

/**
 * Search input, type filter dropdown, and create button. Stateless — state
 * lives in `NotesPage`.
 */
function NotesToolbar({
  search,
  onSearchChange,
  typeFilter,
  onTypeFilterChange,
  onCreate,
}: {
  search: string;
  onSearchChange: (v: string) => void;
  typeFilter: string;
  onTypeFilterChange: (v: string) => void;
  onCreate: () => void;
}) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="relative flex-1 min-w-[200px]">
        <SearchIcon className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search notes..."
          value={search}
          onChange={(e) => { onSearchChange(e.target.value); }}
          className="pl-8"
        />
      </div>
      <Select value={typeFilter} onValueChange={(v) => { if (v) onTypeFilterChange(v); }}>
        <SelectTrigger className="w-[130px]">
          <SelectValue>
            {(value: string | null) => {
              const labels: Record<string, string> = { all: 'All Types', note: 'Notes', checklist: 'Checklists' };
              return labels[value ?? 'all'] ?? value;
            }}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">All Types</SelectItem>
          <SelectItem value="note">Notes</SelectItem>
          <SelectItem value="checklist">Checklists</SelectItem>
        </SelectContent>
      </Select>
      <Button onClick={onCreate}>
        <PlusIcon data-icon="inline-start" />
        New Note
      </Button>
    </div>
  );
}

/**
 * Card preview. Notes show body excerpt; checklists show completion summary.
 */
function NoteCard({ note, onClick }: { note: DisplayNote; onClick: () => void }) {
  const doneCount = note.checklist.filter((i) => i.done).length;
  return (
    <Card className="cursor-pointer transition-colors hover:bg-accent hover:border-ring/30" onClick={onClick}>
      <CardHeader>
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="truncate">{note.title}</CardTitle>
          <Badge variant={note.type === 'checklist' ? 'secondary' : 'default'} className="shrink-0">
            {note.type === 'checklist' ? (
              <><ListChecksIcon className="size-3 mr-1" />Checklist</>
            ) : (
              <><FileTextIcon className="size-3 mr-1" />Note</>
            )}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground line-clamp-2">
          {note.type === 'checklist'
            ? `${String(doneCount)}/${String(note.checklist.length)} tasks completed`
            : note.body}
        </p>
        <p className="mt-2 text-xs text-muted-foreground/60">{formatDate(note.updatedAt)}</p>
      </CardContent>
    </Card>
  );
}

/**
 * Full detail view. Notes show body text; checklists show progress bar + items.
 * Actions: Back, Edit, Delete (with confirmation dialog).
 */
function NoteDetail({
  note,
  onBack,
  onEdit,
  onDelete,
  onItemToggle,
}: {
  note: DisplayNote;
  onBack: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onItemToggle: (itemId: string | number, done: boolean) => void;
}) {
  const doneCount = note.checklist.filter((i) => i.done).length;
  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <Button variant="ghost" size="icon-sm" onClick={onBack}>
          <ArrowLeftIcon className="size-4" />
        </Button>
        <Badge variant={note.type === 'checklist' ? 'secondary' : 'default'}>
          {note.type === 'checklist' ? 'Checklist' : 'Note'}
        </Badge>
      </div>

      <h2 className="text-xl font-bold tracking-tight">{note.title}</h2>
      <p className="mt-1 text-xs text-muted-foreground">Created {formatDate(note.createdAt)} &middot; Updated {formatDate(note.updatedAt)}</p>

      {note.type === 'checklist' ? (
        <div className="mt-6">
          <Progress value={note.checklist.length > 0 ? Math.round((doneCount / note.checklist.length) * 100) : 0} className="mb-4">
            <ProgressLabel>Progress</ProgressLabel>
            <ProgressValue />
          </Progress>
          <div className="space-y-2">
            {note.checklist.map((item) => (
              <label key={item.id} className="flex cursor-pointer items-center gap-3 rounded-lg border border-transparent p-2 transition-colors hover:border-border hover:bg-muted/50">
                <Checkbox
                  checked={item.done}
                  onCheckedChange={(checked) => { onItemToggle(item.id, checked); }}
                />
                <span className={`text-sm ${item.done ? 'text-muted-foreground line-through' : ''}`}>
                  {item.text}
                </span>
              </label>
            ))}
          </div>
        </div>
      ) : (
        <div className="mt-6 rounded-lg border bg-card p-4 text-sm whitespace-pre-wrap">
          {note.body}
        </div>
      )}

      <div className="mt-6 flex gap-2">
        <Button variant="outline" size="sm" onClick={onEdit}>
          <PencilIcon data-icon="inline-start" />Edit
        </Button>
        <Dialog>
          <DialogTrigger render={<Button variant="destructive" size="sm"><Trash2Icon data-icon="inline-start" />Delete</Button>} />
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Delete Note</DialogTitle>
              <DialogDescription>
                Are you sure you want to delete "{note.title}"? This action cannot be undone.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <DialogClose render={<Button variant="outline" />}>Cancel</DialogClose>
              <Button variant="destructive" onClick={onDelete}><Trash2Icon data-icon="inline-start" />Delete</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}

/**
 * Create / Edit form. Type is locked when editing (cannot convert note ↔ checklist).
 * New checklist items get `crypto.randomUUID()` string IDs; persisted items have
 * numeric IDs. Enter in "Add item" input triggers `addItem`.
 */
function NoteForm({
  note,
  onSave,
  onCancel,
}: {
  note: DisplayNote;
  onSave: (n: DisplayNote) => void;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState(note.title);
  const [body, setBody] = useState(note.body);
  const [type, setType] = useState<NoteType>(note.type);
  const [items, setItems] = useState<ChecklistItem[]>(note.checklist);
  const [newItemText, setNewItemText] = useState('');

  /** Add a new checklist item with a local UUID string ID. */
  const addItem = () => {
    if (!newItemText.trim()) return;
    setItems([...items, { id: crypto.randomUUID(), text: newItemText.trim(), done: false }]);
    setNewItemText('');
  };

  /** Toggle a checklist item's done state locally. */
  const toggleItem = (id: string | number) => {
    setItems(items.map((i) => (i.id === id ? { ...i, done: !i.done } : i)));
  };

  /** Remove a checklist item by id (works for both local string ids and persisted numeric ids). */
  const removeItem = (id: string | number) => {
    setItems(items.filter((i) => i.id !== id));
  };

  const canSave = title.trim().length > 0;

  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <Button variant="ghost" size="icon-sm" onClick={onCancel}>
          <XIcon className="size-4" />
        </Button>
        <h2 className="text-lg font-bold tracking-tight">{note.id ? 'Edit Note' : 'New Note'}</h2>
      </div>

      <div className="space-y-4">
        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">Title</label>
          <Input value={title} onChange={(e) => { setTitle(e.target.value); }} placeholder="Note title" />
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs font-medium text-muted-foreground">Type:</label>
          {note.id ? (
            // Editing — type is locked (cannot convert note ↔ checklist)
            <Badge variant={type === 'checklist' ? 'secondary' : 'default'}>
              {type === 'checklist' ? 'Checklist' : 'Note'}
            </Badge>
          ) : (
            // Creating — allow type selection
            (['note', 'checklist'] as const).map((t) => (
              <Button key={t} size="xs" variant={type === t ? 'default' : 'outline'} onClick={() => { setType(t); }}>
                {t === 'note' ? 'Note' : 'Checklist'}
              </Button>
            ))
          )}
        </div>

        {type === 'note' ? (
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Body (Markdown)</label>
            <Textarea value={body} onChange={(e) => { setBody(e.target.value); }} placeholder="Write in markdown..." className="min-h-[200px]" />
          </div>
        ) : (
          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">Checklist Items</label>
            <div className="space-y-2">
              {items.map((item) => (
                <div key={item.id} className="flex items-center gap-2">
                  <Checkbox checked={item.done} onCheckedChange={() => { toggleItem(item.id); }} />
                  <span className={`flex-1 text-sm ${item.done ? 'text-muted-foreground line-through' : ''}`}>
                    {item.text}
                  </span>
                  <Button variant="ghost" size="icon-xs" onClick={() => { removeItem(item.id); }}>
                    <XIcon className="size-3" />
                  </Button>
                </div>
              ))}
            </div>
            <div className="mt-2 flex gap-2">
              <Input
                value={newItemText}
                onChange={(e) => { setNewItemText(e.target.value); }}
                placeholder="Add item..."
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addItem(); } }}
                className="flex-1"
              />
              <Button size="sm" variant="outline" onClick={addItem} disabled={!newItemText.trim()}>
                <PlusIcon className="size-4" />
              </Button>
            </div>
          </div>
        )}

        <div className="flex gap-2 pt-2">
          <Button onClick={() => { onSave({ ...note, id: note.id || 0, title, body, type, checklist: items }); }} disabled={!canSave}>
            <CheckIcon data-icon="inline-start" />Save
          </Button>
          <Button variant="outline" onClick={onCancel}>Cancel</Button>
        </div>
      </div>
    </div>
  );
}

// ── Main page component ────────────────────────────────────────

/**
 * Manages four views (`list` / `detail` / `create` / `edit`).
 *
 * Selected note derivation: `selectedNoteKey` stores type+id pair; `selectedNote`
 * is derived via `useMemo` from the live list to avoid stale snapshots.
 *
 * Checklist save: computes diff between original and form items — numeric IDs
 * absent from form are deleted, string IDs (from `crypto.randomUUID()`) are created.
 */
export function NotesPage() {
  const router = useRouter();
  const routeSearch = useSearch({ from: '/_authenticated/notes/' }) as NotesSearch;
  const [view, setView] = useState<ViewMode>(() => {
    if (routeSearch.action === 'create') return 'create';
    if (routeSearch.detailId) return 'detail';
    return 'list';
  });
  const [selectedNoteKey, setSelectedNoteKey] = useState<{ type: NoteType; id: number } | null>(() => {
    if (routeSearch.detailId && routeSearch.detailType) {
      return { type: routeSearch.detailType, id: Number(routeSearch.detailId) };
    }
    return null;
  });
  const [editingNote, setEditingNote] = useState<DisplayNote>(() => {
    if (routeSearch.action === 'create') {
      const prefill = sessionStorage.getItem('chat-quick-note');
      if (prefill) sessionStorage.removeItem('chat-quick-note');
      return { ...emptyDisplayNote(), body: prefill ?? '' };
    }
    return emptyDisplayNote();
  });
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');

  const { data: apiNotes } = useSuspenseQuery(notesQueries.all());
  const { data: apiChecklists } = useSuspenseQuery(checklistQueries.all());

  const createNote = useCreateNote();
  const updateNote = useUpdateNote();
  const deleteNote = useDeleteNote();
  const createChecklist = useCreateChecklist();
  const updateChecklist = useUpdateChecklist();
  const deleteChecklist = useDeleteChecklist();
  const addChecklistItem = useAddChecklistItem();
  const updateChecklistItem = useUpdateChecklistItem();
  const deleteChecklistItem = useDeleteChecklistItem();

  /**
   * Merge notes and checklists into a unified sorted display list.
   * Both arrays are mapped through `fromApiNote` / `fromApiChecklist` to the
   * common `DisplayNote` shape, then sorted ascending by `createdAt`.
   * Items without a createdAt sort to the end.
   */
  const displayList = useMemo(() => {
    const notes = apiNotes.map(fromApiNote);
    const checklists = apiChecklists.map(fromApiChecklist);
    return [...notes, ...checklists].sort((a, b) => {
      if (!a.createdAt) return 1;
      if (!b.createdAt) return -1;
      return a.createdAt.localeCompare(b.createdAt);
    });
  }, [apiNotes, apiChecklists]);

  /**
   * Derive the currently selected note from the live display list by key.
   *
   * This pattern avoids storing a stale copy of the note in state: when a
   * mutation triggers a refetch, `displayList` updates, and this memo
   * re-derives the selected note with the latest server data. No `useEffect`
   * synchronization needed.
   */
  const selectedNote = useMemo(() => {
    if (!selectedNoteKey) return null;
    return displayList.find((n) => n.type === selectedNoteKey.type && n.id === selectedNoteKey.id) ?? null;
  }, [selectedNoteKey, displayList]);

  /**
   * Client-side search and type filter over the display list.
   * Search matches against both title and body (case-insensitive).
   * Type filter is an exact match against `'note'` / `'checklist'` or shows all.
   * Both filters are applied together — a note must pass both to appear.
   */
  const filtered = useMemo(() => {
    return displayList.filter((n) => {
      const matchesSearch = search === '' ||
        n.title.toLowerCase().includes(search.toLowerCase()) ||
        n.body.toLowerCase().includes(search.toLowerCase());
      const matchesType = typeFilter === 'all' || n.type === typeFilter;
      return matchesSearch && matchesType;
    });
  }, [displayList, search, typeFilter]);

  /** Open the create form with a blank note. */
  const handleCreate = () => {
    setEditingNote(emptyDisplayNote());
    setView('create');
  };

/**
 * Select a note by type+id pair and switch to detail view.
 * Memoized to avoid unnecessary re-render of cards on keystroke.
 */
  const handleSelect = useCallback((note: DisplayNote) => {
    setSelectedNoteKey({ type: note.type, id: note.id });
    setView('detail');
  }, []);

  /** Switch to edit view with a copy of the selected note. */
  const handleEdit = () => {
    const note = selectedNote;
    if (note) {
      setEditingNote({ ...note });
      setView('edit');
    }
  };

  /**
   * Delete the currently selected note or checklist.
   * Uses `mutateAsync` (returns a promise) but the `.catch()` is intentionally
   * empty — error toasts are handled by the mutation hooks' `onError` callbacks.
   * Navigating back to the list view optimistically before the API confirms deletion
   * keeps the UI feeling responsive.
   */
  const handleDelete = () => {
    const note = selectedNote;
    if (!note) return;
    setView('list');
    setSelectedNoteKey(null);
    void router.navigate({
      to: '/notes',
      search: {},
    });
    const promise = note.type === 'note'
      ? deleteNote.mutateAsync(note.id)
      : deleteChecklist.mutateAsync(note.id);
    promise.catch(() => { /* Error toast handled by hook */ });
  };

  /** Return to the list view and clear the selection. */
  const handleBack = useCallback(() => {
    setView('list');
    setSelectedNoteKey(null);
    void router.navigate({
      to: '/notes',
      search: {},
    });
  }, [router]);

  /**
   * Save (create or update) a note or checklist with all its items.
   *
   * **Note save**: Simple create/update via the corresponding mutation hook.
   * **New checklist**: Creates the checklist first, then batches all `addChecklistItem`
   * calls in parallel via `Promise.all`.
   * **Existing checklist update**: Computes a diff against the original items:
   *   - Items with numeric IDs in the original but absent from the form → deleted.
   *   - Items with string IDs (local `crypto.randomUUID()` → created via API.
   *   - The checklist title is updated unconditionally.
   * This diff-based approach avoids deleting and recreating unchanged items,
   * preserving their server-side IDs and creation timestamps.
   *
   * On success, navigates back to the list view. Errors are swallowed here —
   * the mutation hooks' `onError` shows a toast.
   */
  const handleSave = async (note: DisplayNote) => {
    const isNew = !note.id;

    try {
      if (note.type === 'note') {
        if (isNew) {
          await createNote.mutateAsync(
            { title: note.title, content: note.body },
          );
        } else {
          await updateNote.mutateAsync(
            { id: note.id, title: note.title, content: note.body },
          );
        }
        setView('list');
        setSelectedNoteKey(null);
        void router.navigate({
          to: '/notes',
          search: {},
        });
      } else {
        if (isNew) {
          const created = await createChecklist.mutateAsync(
            { title: note.title },
          );
          const checklistId = created.id;
          if (checklistId && note.checklist.length > 0) {
            await Promise.all(
              note.checklist.map((item) =>
                addChecklistItem.mutateAsync({ checklistId, text: item.text }),
              ),
            );
          }
          setView('list');
          void router.navigate({
            to: '/notes',
            search: {},
          });
        } else {
          // Editing existing checklist
          const originalNote = displayList.find((n) => n.type === note.type && n.id === note.id);
          const originalItems = originalNote?.checklist ?? [];

          await updateChecklist.mutateAsync(
            { id: note.id, title: note.title },
          );

          const checklistId = note.id;

          // Remove items (in original but not in form)
          const removedItems = originalItems.filter(
            (orig) => typeof orig.id === 'number' && !note.checklist.some((cur) => cur.id === orig.id),
          );
          if (removedItems.length > 0) {
            await Promise.all(
              removedItems.map((item) =>
                deleteChecklistItem.mutateAsync({ checklistId, itemId: item.id as number }),
              ),
            );
          }

          // Add new items (string ids from crypto.randomUUID)
          const newItems = note.checklist.filter((item) => typeof item.id === 'string');
          if (newItems.length > 0) {
            await Promise.all(
              newItems.map((item) =>
                addChecklistItem.mutateAsync({ checklistId, text: item.text }),
              ),
            );
          }

          setView('list');
          setSelectedNoteKey(null);
          void router.navigate({
            to: '/notes',
            search: {},
          });
        }
      }
    } catch {
      // Mutation errors are handled by each hook's onError toast — nothing more needed
    }
  };

  /**
   * Toggle a checklist item's completion state via the API.
   *
   * Only fires for items with **numeric** IDs (already persisted). Items with
   * string IDs are local-only and their toggle is handled in `NoteForm` without
   * an API call. The item's text is included in the mutation payload so the
   * backend doesn't overwrite it with `null` (the API requires text on update).
   */
  const handleItemToggle = (itemId: string | number, done: boolean) => {
    if (!selectedNote) return;
    const checklistId = selectedNote.id;
    // Only toggle items with numeric ids (from API); local items are handled in form
    if (typeof itemId === 'number') {
      // Include text so the backend doesn't overwrite it with null
      const item = selectedNote.checklist.find((i) => i.id === itemId);
      updateChecklistItem.mutate({ checklistId, itemId, completed: done, text: item?.text });
    }
  };

  if (view === 'detail' && selectedNote) {
    return (
      <div className="p-4 sm:p-6 lg:p-8 max-w-2xl">
        <NoteDetail
          note={selectedNote}
          onBack={handleBack}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onItemToggle={handleItemToggle}
        />
      </div>
    );
  }

  if (view === 'create' || view === 'edit') {
    return (
      <div className="p-4 sm:p-6 lg:p-8 max-w-2xl">
        <NoteForm note={editingNote} onSave={(n) => { void handleSave(n); }} onCancel={handleBack} />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Notes</h1>
      </div>

      <NotesToolbar search={search} onSearchChange={setSearch} typeFilter={typeFilter} onTypeFilterChange={setTypeFilter} onCreate={handleCreate} />

      <div className="mt-6">
        {displayList.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <Empty>
              <EmptyMedia>
                <FileTextIcon className="size-10 text-muted-foreground" />
              </EmptyMedia>
              <EmptyContent>
                <EmptyTitle>No notes yet</EmptyTitle>
                <EmptyDescription>Create your first note or checklist to get started.</EmptyDescription>
                <Button className="mt-2" onClick={handleCreate}><PlusIcon data-icon="inline-start" />New Note</Button>
              </EmptyContent>
            </Empty>
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <Empty>
              <EmptyMedia>
                <SearchIcon className="size-10 text-muted-foreground" />
              </EmptyMedia>
              <EmptyContent>
                <EmptyTitle>No matching notes</EmptyTitle>
                <EmptyDescription>Try adjusting your search or filter.</EmptyDescription>
              </EmptyContent>
            </Empty>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((note) => (
              <NoteCard key={`${note.type}-${String(note.id)}`} note={note} onClick={() => { handleSelect(note); }} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
