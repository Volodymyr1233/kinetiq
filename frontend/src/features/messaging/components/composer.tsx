import { ArrowUpIcon } from 'lucide-react'
import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { MESSAGE_MAX_LENGTH } from '../api/messaging'

/** The field a message is written in. Enter sends and Shift+Enter adds a line, except while an input method composes. */
export function Composer({
  name,
  hint,
  onSend,
}: {
  name: string
  hint?: string
  onSend: (text: string) => void
}) {
  const { t } = useTranslation()
  const fieldId = useId()
  const field = useRef<HTMLTextAreaElement>(null)
  const [text, setText] = useState('')
  const trimmed = text.trim()

  const send = () => {
    if (trimmed === '') return
    onSend(trimmed)
    setText('')
    field.current?.focus()
  }

  return (
    <div className="bg-background flex shrink-0 flex-col gap-1.5 border-t px-4 py-2">
      <div className="flex items-end gap-2">
        <Label htmlFor={fieldId} className="sr-only">
          {t('messaging.composerLabel', { name })}
        </Label>
        <Textarea
          id={fieldId}
          ref={field}
          rows={1}
          value={text}
          maxLength={MESSAGE_MAX_LENGTH}
          placeholder={t('messaging.composerPlaceholder')}
          // ponytail: browsers without field-sizing keep one line that scrolls; add a resize script if that matters
          className="max-h-32 min-h-12 resize-none rounded-3xl px-4 py-3"
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => {
            if (
              event.key === 'Enter' &&
              !event.shiftKey &&
              !event.nativeEvent.isComposing
            ) {
              event.preventDefault()
              send()
            }
          }}
        />
        <Button
          size="icon"
          className="size-12 shrink-0 rounded-full"
          aria-label={t('messaging.send')}
          disabled={trimmed === ''}
          onClick={send}
        >
          <ArrowUpIcon aria-hidden />
        </Button>
      </div>
      {hint && <p className="text-muted-foreground text-xs">{hint}</p>}
    </div>
  )
}
