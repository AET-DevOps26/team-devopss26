import type { ReactNode } from 'react';
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '#/components/ui/empty';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  children?: ReactNode;
}

function EmptyState({ icon, title, description, children }: EmptyStateProps) {
  return (
    <Empty>
      <EmptyHeader>
        {icon && <EmptyMedia>{icon}</EmptyMedia>}
        <EmptyContent>
          <EmptyTitle>{title}</EmptyTitle>
          {description && (
            <EmptyDescription>{description}</EmptyDescription>
          )}
        </EmptyContent>
      </EmptyHeader>
      {children}
    </Empty>
  );
}

export { EmptyState };
