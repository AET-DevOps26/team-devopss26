import { Link, useMatchRoute } from '@tanstack/react-router';
import type { ComponentType } from 'react';
import {
  LayoutDashboard,
  FileText,
  Calendar,
  MessageSquare,
  Palette,
  BookOpen,
} from 'lucide-react';

import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarTrigger,
} from 'src/components/ui/sidebar';

/**
 * Internal links (`to`) use TanStack Router's `<Link />`. External links
 * (`href` + `external: true`) render as `<a>` with target="_blank".
 */
type NavItem = 
  | { to: string; label: string; icon: ComponentType<{ className?: string }> }
  | { href: string; label: string; icon: ComponentType<{ className?: string }>; external: true };

/** Primary navigation items — rendered under the "Navigation" group header. */
const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/notes', label: 'Notes', icon: FileText },
  { to: '/calendar', label: 'Calendar', icon: Calendar },
  { to: '/chat', label: 'Chat', icon: MessageSquare },
  { to: '/demo', label: 'Demo', icon: Palette },
];

/** Secondary navigation items — rendered under the "Developer" group, always external links. */
const developerItems: NavItem[] = [
  { href: '/swagger/', label: 'API Docs', icon: BookOpen, external: true },
];

/**
 * Collapsible sidebar navigation rail. Two groups: Navigation (internal routes,
 * active-detection via useMatchRoute) and Developer (external links, never active).
 * Uses `collapsible="icon"` — shrinks to icon rail on desktop, sheet on mobile.
 */
export function AppSidebar() {
  const matchRoute = useMatchRoute();
  /**
   * Active-link detection: exact match for `/`, fuzzy (prefix) match for others.
   */
  const isLinkActive = (to: string) =>
    to === '/'
      ? !!matchRoute({ to: '/' })
      : !!matchRoute({ to, fuzzy: true });

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton size="lg" className="group-data-[collapsible=icon]:hidden">
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                <LayoutDashboard className="size-4" />
              </div>
              <span className="truncate font-semibold">Life Manager</span>
            </SidebarMenuButton>
            <SidebarTrigger className="hidden group-data-[collapsible=icon]:flex" />
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navItems.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton
                    isActive={'href' in item ? false : isLinkActive(item.to)}
                    tooltip={item.label}
                    render={
                      'href' in item ? (
                        <a href={item.href} target="_blank" rel="noopener noreferrer" />
                      ) : (
                        <Link to={item.to} />
                      )
                    }
                  >
                    <item.icon />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup className="mt-auto">
          <SidebarGroupLabel>Developer</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {developerItems.map((item) => (
                <SidebarMenuItem key={item.label}>
                  <SidebarMenuButton
                    isActive={false}
                    tooltip={item.label}
                    render={
                      'href' in item ? (
                        <a href={item.href} target="_blank" rel="noopener noreferrer" />
                      ) : (
                        <Link to={item.to} />
                      )
                    }
                  >
                    <item.icon />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
