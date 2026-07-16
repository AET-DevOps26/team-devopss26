import { Link, useMatchRoute } from '@tanstack/react-router';
import type { ComponentType } from 'react';
import {
  LayoutDashboard,
  FileText,
  Calendar,
  MessageSquare,
  Palette,
  BookOpen,
  ChartNoAxesCombined,
  HeartPulse,
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

type NavItem = 
  | { to: string; label: string; icon: ComponentType<{ className?: string }> }
  | { href: string; label: string; icon: ComponentType<{ className?: string }>; external: true };

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/notes', label: 'Notes', icon: FileText },
  { to: '/calendar', label: 'Calendar', icon: Calendar },
  { to: '/chat', label: 'Chat', icon: MessageSquare },
  { to: '/demo', label: 'Demo', icon: Palette },
];

const developerItems: NavItem[] = [
  { href: '/swagger', label: 'Swagger UI', icon: BookOpen, external: true },
  { href: '/grafana', label: 'Grafana', icon: ChartNoAxesCombined, external: true },
  { href: '/prometheus/query', label: 'Prometheus', icon: HeartPulse, external: true },
];

export function AppSidebar() {
  const matchRoute = useMatchRoute();
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
