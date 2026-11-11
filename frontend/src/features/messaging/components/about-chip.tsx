import { XIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { About } from '../api/messaging'

// the client's logger leads back to Messages, the conversation's home, also from the sheet over a session; the trainer's
// Logs tab reads no state
function ChipLink(props: {
  to: string
  className: string
  children: ReactNode
}) {
  return <Link {...props} state={{ from: '/app/messages' }} />
}

/**
 * The logged set a message is about: its session and date, exercise, set number and RPE, under the label "About".
 * With `to` the whole chip is a link to that set; in the composer it can be removed with `onRemove` instead.
 */
export function AboutChip({
  about,
  to,
  onRemove,
  className,
}: {
  about: About
  to?: string
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
  const box = cn(
    'bg-card text-card-foreground flex max-w-[80%] items-center gap-1 rounded-lg border py-2 pl-3 text-[13px]/[18px]',
    onRemove === undefined ? 'pr-3' : 'pr-1',
    className,
  )
  // the label and the set are one text, so the link is named by what it shows
  const text = (
    <>
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
    </>
  )

  if (to !== undefined) {
    return (
      <ChipLink
        to={to}
        className={cn(
          box,
          'hover:bg-muted focus-visible:ring-ring/50 min-h-11 outline-none focus-visible:ring-3',
        )}
      >
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">{text}</span>{' '}
        <span className="sr-only">{t('messaging.about.open')}</span>
      </ChipLink>
    )
  }

  return (
    <div className={box}>
      <p className="flex min-w-0 flex-1 flex-col gap-0.5">{text}</p>
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
