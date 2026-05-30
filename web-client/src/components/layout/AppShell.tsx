import { Link, Outlet, useRouterState } from '@tanstack/react-router';
import { User } from 'lucide-react';
import { SidebarInset, SidebarProvider, SidebarTrigger, useSidebar } from 'src/components/ui/sidebar';
import { TooltipProvider } from 'src/components/ui/tooltip';
import { Avatar, AvatarFallback } from 'src/components/ui/avatar';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from 'src/components/ui/breadcrumb';
import { AppSidebar } from './AppSidebar';

function HeaderBreadcrumb() {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const segments = pathname.split('/').filter(Boolean);

  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          {pathname === '/' ? (
            <BreadcrumbPage>Dashboard</BreadcrumbPage>
          ) : (
            <BreadcrumbLink render={<Link to="/" />}>Dashboard</BreadcrumbLink>
          )}
        </BreadcrumbItem>
        {segments.map((segment, i) => {
          const isLast = i === segments.length - 1;
          const label = segment.charAt(0).toUpperCase() + segment.slice(1);
          const to = `/${segments.slice(0, i + 1).join('/')}`;

          return (
            <BreadcrumbItem key={to}>
              <BreadcrumbSeparator />
              {isLast ? (
                <BreadcrumbPage>{label}</BreadcrumbPage>
              ) : (
                <BreadcrumbLink render={<Link to={to} />}>{label}</BreadcrumbLink>
              )}
            </BreadcrumbItem>
          );
        })}
      </BreadcrumbList>
    </Breadcrumb>
  );
}

function HeaderContent() {
  const { open } = useSidebar();

  return (
    <>
      {open && <SidebarTrigger className="-ml-1 mr-4" />}
      <div className="flex flex-1 items-center justify-between">
        <HeaderBreadcrumb />
        <Avatar>
          <AvatarFallback>
            <User className="size-4" />
          </AvatarFallback>
        </Avatar>
      </div>
    </>
  );
}

export function AppShell() {
  return (
    <TooltipProvider delay={300}>
      <SidebarProvider>
        <AppSidebar />

        <SidebarInset>
          <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
            <HeaderContent />
          </header>

          <main className="flex-1">
            <Outlet />
          </main>
        </SidebarInset>
      </SidebarProvider>
    </TooltipProvider>
  );
}
