import { useRef } from 'react'

// a tab activated by focus would navigate again and ask again, so its list's selected tab takes the focus
const focusTarget = (opener: HTMLElement | null) =>
  opener?.getAttribute('role') === 'tab'
    ? (opener
        .closest('[role="tablist"]')
        ?.querySelector<HTMLElement>('[aria-selected="true"]') ?? opener)
    : opener

/**
 * Focus for a dialog that is opened by state and has no trigger, which Radix would return it to: it goes back to the
 * control that had it, unless `afterClosing` was told where it goes instead.
 */
export function useDialogFocus() {
  const opener = useRef<HTMLElement>(null)
  const next = useRef<() => void>(undefined)

  return {
    afterClosing: (run: () => void) => {
      next.current = run
    },
    contentProps: {
      onOpenAutoFocus: () => {
        opener.current = document.activeElement as HTMLElement | null
      },
      onCloseAutoFocus: (event: Event) => {
        event.preventDefault()
        if (next.current) next.current()
        else focusTarget(opener.current)?.focus()
        next.current = undefined
      },
    },
  }
}
