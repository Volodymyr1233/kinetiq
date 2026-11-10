import { ArrowUpIcon } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { MESSAGE_MAX_LENGTH, type About } from '../api/messaging'
import { loadMessageDraft, saveMessageDraft } from '../message-draft'
import { AboutChip } from './about-chip'

const IME_KEY_CODE = 229

/**
 * The field a message is written in. Enter sends and Shift+Enter adds a line, except while an input method composes.
 * What is not sent yet, text and set, is kept per conversation (`draftKey`) until it is sent or cleared.
 */
export function Composer({
  name,
  hint,
  draftKey,
  ask,
  onSend,
}: {
  name: string
  hint?: string
  draftKey: string
  // a set the person just asked about: it goes on the next message in place of any other, and the field takes focus
  ask?: About
  onSend: (text: string, about?: About) => void
}) {
  const { t } = useTranslation()
  const fieldId = useId()
  const field = useRef<HTMLTextAreaElement>(null)
  const [draft, setDraft] = useState(() => {
    const kept = loadMessageDraft(draftKey)
    return ask === undefined ? kept : { ...kept, about: ask }
  })
  const askedSet = ask?.setLogId
  const [taken, setTaken] = useState(askedSet)
  if (askedSet !== taken) {
    setTaken(askedSet)
    if (ask !== undefined) setDraft({ ...draft, about: ask })
  }
  const { text, about } = draft
  const trimmed = text.trim()

  useEffect(() => saveMessageDraft(draftKey, draft), [draftKey, draft])

  // asking about a set is asking to write: the field is ready, on arrival and for each next set
  useEffect(() => {
    if (askedSet !== undefined) field.current?.focus()
  }, [askedSet])

  const send = () => {
    if (trimmed === '') return
    onSend(trimmed, about)
    setDraft({ text: '' })
    field.current?.focus()
  }

  return (
    <div className="bg-background flex shrink-0 flex-col gap-1.5 border-t px-4 py-2">
      {about && (
        <AboutChip
          about={about}
          className="max-w-full"
          onRemove={() => {
            setDraft({ text })
            // the button goes away, and keyboard focus must not fall out of the page
            field.current?.focus()
          }}
        />
      )}
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
          onChange={(event) => setDraft({ ...draft, text: event.target.value })}
          onKeyDown={(event) => {
            if (
              event.key === 'Enter' &&
              !event.shiftKey &&
              !event.nativeEvent.isComposing &&
              // Safari ends the composition first, so only this code gives that Enter away
              event.keyCode !== IME_KEY_CODE
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
