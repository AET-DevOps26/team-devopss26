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
  /** Action slot for a call-to-action button (e.g. "Create note"). */
  children?: ReactNode;
}

/**
 * Centered empty-state placeholder. Show **instead of** the data list when
 * empty. Prefer `<Skeleton>` when layout shape matters for loading feedback.
 */
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
