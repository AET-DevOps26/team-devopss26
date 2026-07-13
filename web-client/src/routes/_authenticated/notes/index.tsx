import { useState, useMemo, useEffect } from 'react';
import { createFileRoute } from '@tanstack/react-router';
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
import type { Note as ApiNote } from '#/types/notes';
import type { Checklist as ApiChecklist } from '#/types/checklist';

// ── Types ──────────────────────────────────────────────────────

type NoteType = 'note' | 'checklist';

interface ChecklistItem {
  id: string | number;
  text: string;
  done: boolean;
}

interface DisplayNote {
  id: number;
  title: string;
  body: string;
  type: NoteType;
  checklist: ChecklistItem[];
  createdAt: string;
  updatedAt: string;
}

type ViewMode = 'list' | 'detail' | 'create' | 'edit';

// ── Route config ───────────────────────────────────────────────

export const Route = createFileRoute('/_authenticated/notes/')({
  loader: async ({ context: { queryClient } }) => {
    await Promise.all([
      queryClient.ensureQueryData(notesQueries.all()),
      queryClient.ensureQueryData(checklistQueries.all()),
    ]);
  },
  pendingComponent: NotesSkeleton,
  errorComponent: NotesError,
  component: NotesPage,
});

// ── Helpers ─────────────────────────────────────────────────────

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

function fromApiNote(note: ApiNote): DisplayNote {
  return {
    id: note.id ?? 0,
    title: note.title ?? '',
    body: note.content ?? '',
    type: 'note',
    checklist: [],
    createdAt: note.createdAt ?? '',
    updatedAt: note.lastUpdatedAt ?? '',
  };
}

function fromApiChecklist(checklist: ApiChecklist): DisplayNote {
  return {
    id: checklist.id ?? 0,
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
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-8"
        />
      </div>
      <Select value={typeFilter} onValueChange={(v) => v && onTypeFilterChange(v)}>
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

function NoteCard({ note, onClick }: { note: DisplayNote; onClick: () => void }) {
  const doneCount = note.checklist.filter((i) => i.done).length;
  return (
    <Card className="cursor-pointer transition-colors hover:border-ring/30" onClick={onClick}>
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
            ? `${doneCount}/${note.checklist.length} tasks completed`
            : note.body}
        </p>
        <p className="mt-2 text-xs text-muted-foreground/60">{formatDate(note.updatedAt)}</p>
      </CardContent>
    </Card>
  );
}

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
                  onCheckedChange={(checked) => onItemToggle(item.id, checked)}
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

  const addItem = () => {
    if (!newItemText.trim()) return;
    setItems([...items, { id: crypto.randomUUID(), text: newItemText.trim(), done: false }]);
    setNewItemText('');
  };

  const toggleItem = (id: string | number) => {
    setItems(items.map((i) => (i.id === id ? { ...i, done: !i.done } : i)));
  };

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
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Note title" />
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
              <Button key={t} size="xs" variant={type === t ? 'default' : 'outline'} onClick={() => setType(t)}>
                {t === 'note' ? 'Note' : 'Checklist'}
              </Button>
            ))
          )}
        </div>

        {type === 'note' ? (
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Body (Markdown)</label>
            <Textarea value={body} onChange={(e) => setBody(e.target.value)} placeholder="Write in markdown..." className="min-h-[200px]" />
          </div>
        ) : (
          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">Checklist Items</label>
            <div className="space-y-2">
              {items.map((item) => (
                <div key={item.id} className="flex items-center gap-2">
                  <Checkbox checked={item.done} onCheckedChange={() => toggleItem(item.id)} />
                  <span className={`flex-1 text-sm ${item.done ? 'text-muted-foreground line-through' : ''}`}>
                    {item.text}
                  </span>
                  <Button variant="ghost" size="icon-xs" onClick={() => removeItem(item.id)}>
                    <XIcon className="size-3" />
                  </Button>
                </div>
              ))}
            </div>
            <div className="mt-2 flex gap-2">
              <Input
                value={newItemText}
                onChange={(e) => setNewItemText(e.target.value)}
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
          <Button onClick={() => onSave({ ...note, id: note.id || 0, title, body, type, checklist: items })} disabled={!canSave}>
            <CheckIcon data-icon="inline-start" />Save
          </Button>
          <Button variant="outline" onClick={onCancel}>Cancel</Button>
        </div>
      </div>
    </div>
  );
}

// ── Main page component ────────────────────────────────────────

export function NotesPage() {
  const [view, setView] = useState<ViewMode>('list');
  const [selectedNote, setSelectedNote] = useState<DisplayNote | null>(null);
  const [editingNote, setEditingNote] = useState<DisplayNote>(emptyDisplayNote());
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

  // Merge notes + checklists into unified display list sorted by createdAt
  const displayList = useMemo(() => {
    const notes = (apiNotes ?? []).map(fromApiNote);
    const checklists = (apiChecklists ?? []).map(fromApiChecklist);
    return [...notes, ...checklists].sort((a, b) => {
      if (!a.createdAt) return 1;
      if (!b.createdAt) return -1;
      return a.createdAt.localeCompare(b.createdAt);
    });
  }, [apiNotes, apiChecklists]);

  // Keep selectedNote in sync when query data refetches after mutation
  // Match by both type AND id to avoid collisions (note=1, checklist=1)
  useEffect(() => {
    if (selectedNote && view === 'detail') {
      const fresh = displayList.find((n) => n.type === selectedNote.type && n.id === selectedNote.id);
      if (fresh && fresh !== selectedNote) {
        setSelectedNote(fresh);
      }
    }
  }, [displayList]);

  const filtered = useMemo(() => {
    return displayList.filter((n) => {
      const matchesSearch = search === '' ||
        n.title.toLowerCase().includes(search.toLowerCase()) ||
        n.body.toLowerCase().includes(search.toLowerCase());
      const matchesType = typeFilter === 'all' || n.type === typeFilter;
      return matchesSearch && matchesType;
    });
  }, [displayList, search, typeFilter]);

  const handleCreate = () => {
    setEditingNote(emptyDisplayNote());
    setView('create');
  };

  const handleSelect = (note: DisplayNote) => {
    setSelectedNote(note);
    setView('detail');
  };

  const handleEdit = () => {
    if (selectedNote) {
      setEditingNote({ ...selectedNote });
      setView('edit');
    }
  };

  const handleDelete = async () => {
    if (!selectedNote) return;
    const item = selectedNote;
    setView('list');
    setSelectedNote(null);
    try {
      if (item.type === 'note') {
        await deleteNote.mutateAsync(item.id);
      } else {
        await deleteChecklist.mutateAsync(item.id);
      }
    } catch {
      // Error toast handled by hook
    }
  };

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
        setSelectedNote(null);
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
          setSelectedNote(null);
        }
      }
    } catch {
      // Mutation errors are handled by each hook's onError toast — nothing more needed
    }
  };

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

  const handleBack = () => {
    setView('list');
    setSelectedNote(null);
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
        <NoteForm note={editingNote} onSave={handleSave} onCancel={handleBack} />
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
              <NoteCard key={`${note.type}-${note.id}`} note={note} onClick={() => handleSelect(note)} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
