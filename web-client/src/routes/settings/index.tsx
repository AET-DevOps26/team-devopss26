import { useState } from 'react';
import { createFileRoute } from '@tanstack/react-router';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '#/components/ui/card.tsx';
import { Button } from '#/components/ui/button.tsx';
import { Input } from '#/components/ui/input.tsx';
import { Avatar, AvatarImage, AvatarFallback } from '#/components/ui/avatar.tsx';
import { Skeleton } from '#/components/ui/skeleton.tsx';
import { Separator } from '#/components/ui/separator.tsx';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '#/components/ui/select.tsx';
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '#/components/ui/dialog.tsx';

import { toast } from 'sonner';
import {
  UserIcon,
  EyeIcon,
  EyeOffIcon,
  SunIcon,
  MoonIcon,
  MonitorIcon,
  TriangleAlertIcon,
  CheckIcon,
  XIcon,
  Loader2Icon,
} from 'lucide-react';

export const Route = createFileRoute('/settings/')({ component: SettingsPage });

// ── Mock data ──────────────────────────────────────────────────

const mockProfile = {
  name: 'Alex Chen',
  email: 'alex.chen@example.com',
  avatar: '',
};

// ── Sub-components ─────────────────────────────────────────────

function ProfileSection() {
  const [name, setName] = useState(mockProfile.name);
  const [email, setEmail] = useState(mockProfile.email);
  const [saving, setSaving] = useState(false);
  const [loading] = useState(false);

  const handleSave = () => {
    setSaving(true);
    setTimeout(() => {
      setSaving(false);
      toast.success('Profile updated successfully');
    }, 1000);
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Profile</CardTitle>
          <CardDescription>Your personal information</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <Skeleton className="size-12 rounded-full" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-48" />
              <Skeleton className="h-4 w-64" />
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Profile</CardTitle>
        <CardDescription>Your personal information</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-4">
          <Avatar size="lg">
            <AvatarImage src="" />
            <AvatarFallback className="bg-primary/10 text-primary">
              <UserIcon className="size-5" />
            </AvatarFallback>
          </Avatar>
          <Button variant="outline" size="sm">Change Photo</Button>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Name</label>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted-foreground">Email</label>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
        </div>
        <Button onClick={handleSave} disabled={saving}>
          {saving ? <><Loader2Icon className="size-4 animate-spin" data-icon="inline-start" />Saving...</> : <><CheckIcon data-icon="inline-start" />Save</>}
        </Button>
      </CardContent>
    </Card>
  );
}

function PasswordSection() {
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [saving, setSaving] = useState(false);

  const requirements = [
    { label: 'At least 8 characters', met: newPw.length >= 8 },
    { label: 'Contains uppercase letter', met: /[A-Z]/.test(newPw) },
    { label: 'Contains a number', met: /\d/.test(newPw) },
    { label: 'Contains a special character', met: /[!@#$%^&*(),.?":{}|<>]/.test(newPw) },
  ];

  const strength = requirements.filter((r) => r.met).length;
  const strengthLabel = ['Weak', 'Fair', 'Good', 'Strong', 'Very Strong'][strength] ?? 'Weak';
  const strengthColor = ['bg-destructive', 'bg-orange-400', 'bg-yellow-400', 'bg-primary', 'bg-primary'][strength] ?? 'bg-destructive';

  const handleSave = () => {
    setSaving(true);
    setTimeout(() => {
      setSaving(false);
      setCurrentPw('');
      setNewPw('');
      setConfirmPw('');
      toast.success('Password changed successfully');
    }, 1000);
  };

  const canSave = currentPw && newPw && newPw === confirmPw && strength >= 3;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Password</CardTitle>
        <CardDescription>Change your account password</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="relative">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">Current Password</label>
          <Input type={showPw ? 'text' : 'password'} value={currentPw} onChange={(e) => setCurrentPw(e.target.value)} placeholder="Enter current password" />
        </div>

        <div className="relative">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">New Password</label>
          <div className="relative">
            <Input type={showPw ? 'text' : 'password'} value={newPw} onChange={(e) => setNewPw(e.target.value)} placeholder="Enter new password" className="pr-9" />
            <Button variant="ghost" size="icon-xs" className="absolute right-1 top-1/2 -translate-y-1/2 text-muted-foreground" onClick={() => setShowPw(!showPw)}>
              {showPw ? <EyeOffIcon className="size-4" /> : <EyeIcon className="size-4" />}
            </Button>
          </div>
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-muted-foreground">Confirm New Password</label>
          <Input type={showPw ? 'text' : 'password'} value={confirmPw} onChange={(e) => setConfirmPw(e.target.value)} placeholder="Confirm new password" />
        </div>

        {/* Strength indicator */}
        {newPw && (
          <div className="rounded-lg border p-3 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium">Strength: {strengthLabel}</span>
              <span className="text-xs text-muted-foreground">{strength}/4</span>
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
              <div className={`h-full rounded-full transition-all ${strengthColor}`} style={{ width: `${(strength / 4) * 100}%` }} />
            </div>
            <ul className="space-y-1">
              {requirements.map((req) => (
                <li key={req.label} className={`flex items-center gap-1.5 text-xs ${req.met ? 'text-primary' : 'text-muted-foreground'}`}>
                  {req.met ? <CheckIcon className="size-3" /> : <XIcon className="size-3" />}
                  {req.label}
                </li>
              ))}
            </ul>
          </div>
        )}

        <Button onClick={handleSave} disabled={!canSave || saving}>
          {saving ? <><Loader2Icon className="size-4 animate-spin" data-icon="inline-start" />Changing...</> : 'Change Password'}
        </Button>
      </CardContent>
    </Card>
  );
}

function ThemeSection() {
  const [theme, setTheme] = useState('system');

  return (
    <Card>
      <CardHeader>
        <CardTitle>Theme</CardTitle>
        <CardDescription>Choose your preferred appearance</CardDescription>
      </CardHeader>
      <CardContent>
        <Select value={theme} onValueChange={(v) => v && setTheme(v)}>
          <SelectTrigger className="w-[180px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="light">
              <span className="flex items-center gap-2"><SunIcon className="size-4" />Light</span>
            </SelectItem>
            <SelectItem value="dark">
              <span className="flex items-center gap-2"><MoonIcon className="size-4" />Dark</span>
            </SelectItem>
            <SelectItem value="system">
              <span className="flex items-center gap-2"><MonitorIcon className="size-4" />System</span>
            </SelectItem>
          </SelectContent>
        </Select>
        <p className="mt-2 text-xs text-muted-foreground">Current selection: {theme}</p>
      </CardContent>
    </Card>
  );
}

function DangerZone() {
  const [confirmText, setConfirmText] = useState('');

  return (
    <Card className="border-destructive/20 ring-1 ring-destructive/10">
      <CardHeader>
        <div className="flex items-center gap-2">
          <TriangleAlertIcon className="size-4 text-destructive" />
          <CardTitle className="text-destructive">Danger Zone</CardTitle>
        </div>
        <CardDescription>
          Irreversible actions. Proceed with caution.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Dialog>
          <DialogTrigger render={<Button variant="destructive">Delete Account</Button>} />
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Delete Account</DialogTitle>
              <DialogDescription>
                This will permanently delete your account and all associated data. This action cannot be undone.
              </DialogDescription>
            </DialogHeader>
            <div className="py-2">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                Type <span className="font-mono text-foreground">DELETE</span> to confirm
              </label>
              <Input
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                placeholder="Type DELETE to confirm"
              />
            </div>
            <DialogFooter>
              <Button variant="outline" data-slot="dialog-close">Cancel</Button>
              <Button variant="destructive" disabled={confirmText !== 'DELETE'} onClick={() => { toast.error('Account deleted (mock)'); }}>
                <TriangleAlertIcon data-icon="inline-start" />Delete My Account
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardContent>
    </Card>
  );
}

// ── Main page component ────────────────────────────────────────

function SettingsPage() {
  return (
    <div className="p-4 sm:p-6 lg:p-8 max-w-2xl">
      <h1 className="text-2xl font-bold tracking-tight sm:text-3xl mb-6">Settings</h1>

      <div className="space-y-6">
        <ProfileSection />
        <PasswordSection />
        <ThemeSection />
        <Separator />
        <DangerZone />
      </div>
    </div>
  );
}
