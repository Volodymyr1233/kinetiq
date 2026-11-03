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
import { ClientMessagesPage } from '@/features/messaging/pages/client-messages-page'

/**
 * The waiting state's "Message your trainer" button and the sheet it opens: the client's conversation with their
 * trainer, as in Messages.
 */
export function MessageTrainerSheet() {
  const { t } = useTranslation()
  const content = useRef<HTMLDivElement>(null)

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="outline" size="touch" className="mt-4 w-full">
          {t('quiz.waiting.reviewing.message')}
        </Button>
      </SheetTrigger>
      <SheetContent
        ref={content}
        side="right"
        className="outline-none data-[side=right]:w-full data-[side=right]:sm:max-w-sm"
        // the sheet takes focus: its first control changes while loading, and the message field opens a phone's keyboard
        onOpenAutoFocus={(event) => {
          event.preventDefault()
          content.current?.focus()
        }}
      >
        <SheetHeader>
          <SheetTitle>{t('nav.messages')}</SheetTitle>
        </SheetHeader>
        <ClientMessagesPage />
      </SheetContent>
    </Sheet>
  )
}
