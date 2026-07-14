import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { Card, CardContent, CardHeader, CardTitle } from '#/components/ui/card.tsx';
import { Button } from '#/components/ui/button.tsx';
import { Input } from '#/components/ui/input.tsx';
import { Badge } from '#/components/ui/badge.tsx';
import { Textarea } from '#/components/ui/textarea.tsx';
import { Checkbox } from '#/components/ui/checkbox.tsx';
import { Progress, ProgressLabel, ProgressValue } from '#/components/ui/progress.tsx';
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
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
} from 'lucide-react';

export const Route = createFileRoute('/_authenticated/notes/')({ component: NotesPage });

// ── Types ──────────────────────────────────────────────────────

type NoteType = 'note' | 'checklist';

interface ChecklistItem {
  id: string;
  text: string;
  done: boolean;
}

interface Note {
  id: string;
  title: string;
  body: string;
  type: NoteType;
  checklist: ChecklistItem[];
  createdAt: string;
  updatedAt: string;
}

type ViewMode = 'list' | 'detail' | 'create' | 'edit';

// ── Mock data ──────────────────────────────────────────────────

const mockNotes: Note[] = [
  {
    id: '1',
    title: 'Project setup steps',
    body: '## Prerequisites\n- Node.js 22+\n- pnpm 10+\n\n## Steps\n1. Clone the monorepo\n2. Run `pnpm install`\n3. Run `pnpm dev`\n\nThe project uses Vite + React + TanStack Router.',
    type: 'note',
    checklist: [],
    createdAt: '2026-06-15',
    updatedAt: '2h ago',
  },
  {
    id: '2',
    title: 'Sprint 26 tasks',
    body: 'Items for the current sprint.',
    type: 'checklist',
    checklist: [
      { id: 'c1', text: 'Finalize API contracts', done: true },
      { id: 'c2', text: 'Implement calendar mockup', done: true },
      { id: 'c3', text: 'Wire up notes CRUD', done: false },
      { id: 'c4', text: 'Deploy staging environment', done: false },
    ],
    createdAt: '2026-06-14',
    updatedAt: '5h ago',
  },
  {
    id: '3',
    title: 'Design tokens reference',
    body: 'Primary: oklch(0.531 0.101 153.371)\nSecondary: oklch(0.585 0.085 61.136)\n\nRadius: 1.55rem\nFont: Geist Mono',
    type: 'note',
    checklist: [],
    createdAt: '2026-06-13',
    updatedAt: '1d ago',
  },
  {
    id: '4',
    title: 'Bug bash findings',
    body: 'Issues found during QA session.',
    type: 'checklist',
    checklist: [
      { id: 'c5', text: 'Sidebar not collapsing on mobile', done: false },
      { id: 'c6', text: 'Toast timeout too short', done: true },
    ],
    createdAt: '2026-06-12',
    updatedAt: '2d ago',
  },
];

const emptyNote: Note = {
  id: '',
  title: '',
  body: '',
  type: 'note',
  checklist: [],
  createdAt: '',
  updatedAt: '',
};

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
          <SelectValue />
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

function NoteCard({ note, onClick }: { note: Note; onClick: () => void }) {
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
        <p className="mt-2 text-xs text-muted-foreground/60">{note.updatedAt}</p>
      </CardContent>
    </Card>
  );
}

function NoteDetail({ note, onBack, onEdit, onDelete }: { note: Note; onBack: () => void; onEdit: () => void; onDelete: () => void }) {
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
      <p className="mt-1 text-xs text-muted-foreground">Created {note.createdAt} &middot; Updated {note.updatedAt}</p>

      {note.type === 'checklist' ? (
        <div className="mt-6">
          <Progress value={doneCount} max={note.checklist.length} className="mb-4">
            <ProgressLabel>Progress</ProgressLabel>
            <ProgressValue />
          </Progress>
          <div className="space-y-2">
            {note.checklist.map((item) => (
              <label key={item.id} className="flex cursor-pointer items-center gap-3 rounded-lg border border-transparent p-2 transition-colors hover:border-border hover:bg-muted/50">
                <Checkbox defaultChecked={item.done} />
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
              <Button variant="outline" data-slot="dialog-close">Cancel</Button>
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
  note: Note;
  onSave: (n: Note) => void;
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

  const toggleItem = (id: string) => {
    setItems(items.map((i) => (i.id === id ? { ...i, done: !i.done } : i)));
  };

  const removeItem = (id: string) => {
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
          {(['note', 'checklist'] as const).map((t) => (
            <Button key={t} size="xs" variant={type === t ? 'default' : 'outline'} onClick={() => setType(t)}>
              {t === 'note' ? 'Note' : 'Checklist'}
            </Button>
          ))}
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
          <Button onClick={() => onSave({ ...note, title, body, type, checklist: items })} disabled={!canSave}>
            <CheckIcon data-icon="inline-start" />Save
          </Button>
          <Button variant="outline" onClick={onCancel}>Cancel</Button>
        </div>
      </div>
    </div>
  );
}

// ── Main page component ────────────────────────────────────────

function NotesPage() {
  const [view, setView] = useState<ViewMode>('list');
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [editingNote, setEditingNote] = useState<Note>(emptyNote);
  const [notes] = useState<Note[]>(mockNotes);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');

  const filtered = notes.filter((n) => {
    const matchesSearch = search === '' || n.title.toLowerCase().includes(search.toLowerCase()) || n.body.toLowerCase().includes(search.toLowerCase());
    const matchesType = typeFilter === 'all' || n.type === typeFilter;
    return matchesSearch && matchesType;
  });

  const handleCreate = () => {
    setEditingNote(emptyNote);
    setView('create');
  };

  const handleSelect = (note: Note) => {
    setSelectedNote(note);
    setView('detail');
  };

  const handleEdit = () => {
    if (selectedNote) {
      setEditingNote({ ...selectedNote });
      setView('edit');
    }
  };

  const handleDelete = () => {
    // Mock: go back to list
    setView('list');
    setSelectedNote(null);
  };

  const handleSave = (note: Note) => {
    // Mock: return to detail view
    setSelectedNote(note);
    setView('detail');
  };

  const handleBack = () => {
    setView('list');
    setSelectedNote(null);
  };

  if (view === 'detail' && selectedNote) {
    return (
      <div className="p-4 sm:p-6 lg:p-8 max-w-2xl">
        <NoteDetail note={selectedNote} onBack={handleBack} onEdit={handleEdit} onDelete={handleDelete} />
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
        {notes.length === 0 ? (
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
              <NoteCard key={note.id} note={note} onClick={() => handleSelect(note)} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
