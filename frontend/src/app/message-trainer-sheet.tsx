import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import type { About } from '@/features/messaging/api/messaging'
import { ClientMessagesPage } from '@/features/messaging/pages/client-messages-page'

/**
 * What a sheet with the client's conversation with their trainer shows: the conversation, as in Messages. With
 * `ask`, the next message is about that logged set.
 */
export function TrainerConversationSheetContent({
  ask,
  onCloseAutoFocus,
}: {
  ask?: About
  // for a sheet without a trigger, which has nothing to give the focus back to by itself
  onCloseAutoFocus?: (event: Event) => void
}) {
  const { t } = useTranslation()
  const content = useRef<HTMLDivElement>(null)

  return (
    <SheetContent
      ref={content}
      side="right"
      className="outline-none data-[side=right]:w-full data-[side=right]:sm:max-w-sm"
      // the sheet takes focus: its first control changes while loading, and the message field opens a phone's keyboard.
      // Asking about a set is the exception: the composer has taken focus by then, or takes it when it is there
      onOpenAutoFocus={(event) => {
        event.preventDefault()
        if (!content.current?.contains(document.activeElement)) {
          content.current?.focus()
        }
      }}
      onCloseAutoFocus={onCloseAutoFocus}
    >
      <SheetHeader>
        <SheetTitle>{t('nav.messages')}</SheetTitle>
      </SheetHeader>
      <ClientMessagesPage ask={ask} />
    </SheetContent>
  )
}

/**
 * The waiting state's "Message your trainer" button and the sheet it opens: the client's conversation with their
 * trainer, as in Messages.
 */
export function MessageTrainerSheet() {
  const { t } = useTranslation()

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="outline" size="touch" className="mt-4 w-full">
          {t('quiz.waiting.reviewing.message')}
        </Button>
      </SheetTrigger>
      <TrainerConversationSheetContent />
    </Sheet>
  )
}
