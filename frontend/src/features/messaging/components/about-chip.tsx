import { XIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { About } from '../api/messaging'

/**
 * The logged set a message is about: its session and date, exercise, set number and RPE, under the label "About".
 * In the composer it can be removed with `onRemove`.
 */
export function AboutChip({
  about,
  onRemove,
  className,
}: {
  about: About
  onRemove?: () => void
  className?: string
}) {
  const { t, i18n } = useTranslation()
  // a plain date, read in UTC so no time zone can move the day
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  }).format(new Date(about.sessionDate))

  return (
    <div
      className={cn(
        'bg-card text-card-foreground flex max-w-[80%] items-center gap-1 rounded-lg border py-2 pr-1 pl-3 text-[13px]/[18px]',
        onRemove === undefined && 'pr-3',
        className,
      )}
    >
      <p className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="num text-muted-foreground text-xs tracking-wider uppercase">
          {t('messaging.about.label')}
        </span>{' '}
        <span className="wrap-anywhere">
          {t('messaging.about.text', {
            session: about.sessionName,
            date,
            exercise: about.exerciseName,
            number: about.setNumber,
            rpe: about.rpe,
          })}
        </span>
      </p>
      {onRemove && (
        <Button
          type="button"
          variant="ghost"
          className="size-12 shrink-0"
          aria-label={t('messaging.about.remove')}
          onClick={onRemove}
        >
          <XIcon aria-hidden />
        </Button>
      )}
    </div>
  )
}
