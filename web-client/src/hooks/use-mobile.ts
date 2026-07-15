import * as React from "react"

/** Tailwind "md" breakpoint in pixels. */
const MOBILE_BREAKPOINT = 768

/** Detect viewport <768px. Uses `matchMedia` for CSS-engine integration.
 * Initial state `undefined` (coerced to `false`) for SSR safety.
 *
 * @returns `true` when viewport width < 768px, `false` otherwise
 */
export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState<boolean | undefined>(undefined)

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`)
    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    }
    mql.addEventListener("change", onChange)
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT)
    return () => mql.removeEventListener("change", onChange)
  }, [])

  return !!isMobile
}
