import { cn } from "src/lib/utils"

/**
 * Loading placeholder. Use for layout with predictable shape; prefer over
 * Spinner when content shape is known.
 */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("animate-pulse rounded-md bg-muted", className)}
      {...props}
    />
  )
}

export { Skeleton }
