/** A set's place in its session, as an address names it: `?set=<entry position>-<set number>`. */
export type SetPlace = { entryPosition: number; setNumber: number }

/** Reads the place from `set`; anything else names no set. */
export function setPlaceOf(value: string | null): SetPlace | undefined {
  const match = /^(\d+)-(\d+)$/.exec(value ?? '')
  return match
    ? { entryPosition: Number(match[1]), setNumber: Number(match[2]) }
    : undefined
}

export const isPlace = (
  place: SetPlace | undefined,
  entryPosition: number,
  setNumber: number,
) => place?.entryPosition === entryPosition && place.setNumber === setNumber

/**
 * Brings a set that an address names into view and gives it the focus, so the next Tab goes on from it. A ref callback:
 * it runs when the marked row appears, once.
 */
export function reveal(element: HTMLElement | null) {
  // jsdom has no scrolling
  element?.scrollIntoView?.({ block: 'center' })
  element?.focus({ preventScroll: true })
}

/**
 * The look of a marked set: the light lime of a chosen tab, which flashes in, and a line on its left edge so the mark
 * is not colour alone. `aria-current` tells it in words.
 */
export const MARKED =
  'bg-brand-muted animate-marked shadow-[inset_3px_0_0_var(--brand-strong)] outline-none'
