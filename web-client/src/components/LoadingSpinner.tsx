import { cn } from '#/lib/utils';
import { Spinner } from '#/components/ui/spinner';

const sizeClasses = {
  sm: 'size-3',
  md: 'size-4',
  lg: 'size-6',
} as const;

interface LoadingSpinnerProps {
  size?: keyof typeof sizeClasses;
  className?: string;
}

/**
 * Accessible loading spinner. Use when content shape is unknown or changes on load.
 * Prefer Skeleton when layout is predictable.
 */
function LoadingSpinner({ size = 'md', className }: LoadingSpinnerProps) {
  return (
    <Spinner
      className={cn(sizeClasses[size], className)}
      aria-label="Loading"
    />
  );
}

export { LoadingSpinner };
