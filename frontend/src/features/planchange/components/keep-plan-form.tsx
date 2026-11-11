import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'
import { DECLINE_REASONS, type DeclineReason } from '../api/client-plan-change'

/** The most a note holds; the backend refuses a longer one. */
export const NOTE_MAX_LENGTH = 500

/**
 * The reason for keeping the current plan: one of five, none chosen at first, and a note that may stay empty. It goes in
 * a sheet for the client and a dialog for the trainer; `touch` gives the client's 48px controls.
 */
export function KeepPlanForm({
  touch = false,
  busy,
  error,
  onSubmit,
  onBack,
}: {
  touch?: boolean
  busy: boolean
  /** The text of the last refusal, shown where the person is looking. */
  error?: string
  onSubmit: (reason: DeclineReason, note: string | undefined) => void
  onBack: () => void
}) {
  const { t } = useTranslation()
  const [reason, setReason] = useState<DeclineReason | ''>('')
  const [note, setNote] = useState('')
  const reasonLabel = useId()
  const noteId = useId()

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        if (reason) onSubmit(reason, note.trim() || undefined)
      }}
    >
      <div className="flex flex-col gap-2">
        <span id={reasonLabel} className="text-sm font-medium">
          {t('planChange.keep.reason')}
        </span>
        <ToggleGroup
          type="single"
          variant="outline"
          spacing={2}
          value={reason}
          onValueChange={(value) => setReason(value as DeclineReason | '')}
          aria-labelledby={reasonLabel}
          disabled={busy}
          className="flex-wrap"
        >
          {DECLINE_REASONS.map((value) => (
            <ToggleGroupItem
              key={value}
              value={value}
              className={cn(
                'data-[state=on]:border-primary data-[state=on]:bg-background data-[state=on]:font-semibold',
                touch && 'h-12 px-4 text-base',
              )}
            >
              {t(`planChange.reason.${value}`)}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={noteId}>{t('planChange.keep.note')}</Label>
        <Textarea
          id={noteId}
          value={note}
          maxLength={NOTE_MAX_LENGTH}
          readOnly={busy}
          placeholder={t('planChange.keep.notePlaceholder')}
          onChange={(event) => setNote(event.target.value)}
        />
      </div>
      <div role="alert" className="text-destructive text-sm empty:hidden">
        {error}
      </div>
      <div className="flex flex-col gap-2 sm:flex-row-reverse">
        <Button
          type="submit"
          size={touch ? 'touch' : 'default'}
          disabled={!reason || busy}
        >
          {t('planChange.keep.submit')}
        </Button>
        <Button
          type="button"
          variant="outline"
          size={touch ? 'touch' : 'default'}
          disabled={busy}
          onClick={onBack}
        >
          {t('planChange.keep.back')}
        </Button>
      </div>
    </form>
  )
}
