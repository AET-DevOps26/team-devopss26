import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Merge Tailwind classes with conflict resolution (clsx + tailwind-merge).
 *
 * @param inputs - Class values following clsx conventions
 * @returns Merged and deduplicated class string
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Generate a UUID v4 string for local-only entity IDs (e.g. checklist items
 * that haven't been persisted yet).
 *
 * Prefers `crypto.randomUUID()` (concise, native), but falls back to a
 * `crypto.getRandomValues()`-based implementation when running in a
 * non-secure context — `crypto.randomUUID` is only exposed on HTTPS or
 * localhost origins, so it throws on the plain-HTTP Azure deployment
 * (`http://20.91.193.39/`) while the AET cluster behind a TLS-terminating
 * ingress works fine. `crypto.getRandomValues` is available in every
 * context (HTTP, HTTPS, file://), so the fallback keeps the app functional
 * in any deployment.
 */
export function genId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16);
    crypto.getRandomValues(bytes);
    // RFC 4122 §4.4 — set version (4) and variant (10xx) bits.
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    return (
      hex.slice(0, 8) +
      '-' +
      hex.slice(8, 12) +
      '-' +
      hex.slice(12, 16) +
      '-' +
      hex.slice(16, 20) +
      '-' +
      hex.slice(20, 32)
    );
  }
  // Last-resort fallback if `crypto` is somehow not available at all
  // (very old browsers, exotic test envs). Not RFC-compliant but unique
  // enough for local-item IDs in a single tab.
  return `local-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 11)}`;
}
