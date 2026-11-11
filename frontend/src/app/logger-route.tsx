import { useRef, useState } from 'react'
import { useLocation } from 'react-router'
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
  // a link in the conversation, to a set of this session too, starts afresh: the sheet is gone and the set has the focus
  return <Logger key={useLocation().key} />
}

function Logger() {
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
            // the sheet has no trigger to go back to; after a link the button is gone and the set has the focus
            event.preventDefault()
            if (
              asker.current instanceof HTMLElement &&
              asker.current.isConnected
            )
              asker.current.focus()
          }}
        />
      </Sheet>
    </>
  )
}
