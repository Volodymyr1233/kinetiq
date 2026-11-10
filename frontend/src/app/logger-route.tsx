import { useRef, useState } from 'react'
import { Sheet } from '@/components/ui/sheet'
import type { About } from '@/features/messaging/api/messaging'
import { LoggerPage } from '@/features/sessionlog/pages/logger-page'
import { TrainerConversationSheetContent } from './message-trainer-sheet'

/**
 * The session logger, with the client's conversation with their trainer in a sheet over it: asking about a logged set
 * opens the conversation with that set on the next message, and closing it leaves the session as it was. What was not
 * sent stays in the composer for the next time.
 */
export function LoggerRoute() {
  const [open, setOpen] = useState(false)
  const [ask, setAsk] = useState<About>()
  // the row's button that opened the sheet, which gets the focus back when it closes
  const asker = useRef<Element | null>(null)

  return (
    <>
      <LoggerPage
        onAsk={(set) => {
          asker.current = document.activeElement
          setAsk(set)
          setOpen(true)
        }}
      />
      <Sheet open={open} onOpenChange={setOpen}>
        <TrainerConversationSheetContent
          ask={ask}
          onCloseAutoFocus={(event) => {
            if (asker.current instanceof HTMLElement) {
              event.preventDefault()
              asker.current.focus()
            }
          }}
        />
      </Sheet>
    </>
  )
}
