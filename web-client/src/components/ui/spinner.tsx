import { cn } from "src/lib/utils"
import { Loader2Icon } from "lucide-react"

/**
 * Animated loading indicator. Accessible with `role="status"` and
 * `aria-label="Loading"`. Use for inline or button loading states.
 */
function Spinner({ className, ...props }: React.ComponentProps<"svg">) {
  return (
    <Loader2Icon data-slot="spinner" role="status" aria-label="Loading" className={cn("size-4 animate-spin", className)} {...props} />
  )
}

export { Spinner }
