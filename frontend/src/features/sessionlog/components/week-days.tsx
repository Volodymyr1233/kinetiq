import { CheckIcon, XIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'
import type { ClientOverview, ClientSession } from '../api/client-training'

type Week = NonNullable<ClientOverview['week']>

// the plan editor letters a week's sessions the same way
const letter = (index: number) => String.fromCharCode(65 + index)

function Mark({
  session,
  index,
  today,
}: {
  session?: ClientSession
  index: number
  today: boolean
}) {
  const circle = 'flex size-8 items-center justify-center rounded-full'
  if (session === undefined) {
    return (
      <span className={cn(circle, 'bg-muted text-muted-foreground')}>–</span>
    )
  }
  if (session.state === 'DONE') {
    return (
      <span className={cn(circle, 'bg-primary text-primary-foreground')}>
        <CheckIcon className="size-4" />
      </span>
    )
  }
  if (session.state === 'MISSED') {
    return (
      <span className={cn(circle, 'bg-destructive-muted text-destructive')}>
        <XIcon className="size-4" />
      </span>
    )
  }
  return (
    <span
      className={cn(
        circle,
        'num text-sm font-medium',
        today ? 'ring-primary ring-2' : 'border',
      )}
    >
      {letter(index)}
    </span>
  )
}

/**
 * The seven days of a plan week, each with its session's mark or a rest-day dash. The marks are for scanning; each day
 * also says its session and state in words.
 */
export function WeekDays({ week, today }: { week: Week; today: string }) {
  const { t, i18n } = useTranslation()
  const [year, month, day] = week.startsOn.split('-').map(Number)
  // plain dates, so no time zone can move a day: the strip is read in UTC
  const weekday = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'short',
    timeZone: 'UTC',
  })

  return (
    <ol className="grid grid-cols-7 gap-1">
      {Array.from({ length: 7 }, (_, offset) => {
        const date = new Date(Date.UTC(year, month - 1, day + offset))
        const iso = date.toISOString().slice(0, 10)
        const index = week.sessions.findIndex((s) => s.date === iso)
        const session = index < 0 ? undefined : week.sessions[index]
        const isToday = iso === today
        const state =
          session?.state === 'DONE' && session.locked
            ? 'LOCKED'
            : session?.state
        return (
          <li
            key={iso}
            aria-current={isToday ? 'date' : undefined}
            className={cn(
              'flex flex-col items-center gap-2 rounded-lg py-2',
              isToday && 'bg-brand-muted',
            )}
          >
            <span
              className={cn(
                'text-xs',
                isToday ? 'font-semibold' : 'text-muted-foreground',
              )}
            >
              {weekday.format(date)}
            </span>
            <span aria-hidden>
              <Mark session={session} index={index} today={isToday} />
            </span>
            <span className="sr-only">
              {session === undefined
                ? t('sessionlog.overview.restDay')
                : state === undefined
                  ? session.name
                  : t('sessionlog.overview.dayState', {
                      name: session.name,
                      state: t(`plan.state.${state}`),
                    })}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
