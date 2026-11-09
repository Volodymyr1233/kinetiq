import { useTranslation } from 'react-i18next'
import type { OwnPlan } from '../api/client-plan'
import { DAYS, dayName } from '../plan-draft'
import type { SessionStateEntry } from '../api/session-states'
import { DayMark } from './day-mark'
import { SessionCard } from './session-card'

type Week = NonNullable<OwnPlan['week']>

/** The seven days of a plan week, each with the session on it or a rest-day mark. */
export function WeekStrip({
  week,
  sessions,
  states,
  today,
}: {
  week: Week
  sessions: OwnPlan['sessions']
  // missing while they load and when they cannot be read: no badges and no way to a log then
  states?: SessionStateEntry[]
  // the client's local date, from the same answer as the states
  today?: string
}) {
  const { t, i18n } = useTranslation()
  const [year, month, day] = week.startsOn.split('-').map(Number)
  // plain dates, so no time zone can move a day: the strip is read in UTC
  const date = new Intl.DateTimeFormat(i18n.language, {
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  })

  return (
    <ol className="flex flex-col gap-2">
      {DAYS.map((weekday, offset) => {
        const session = sessions.find((s) => s.day === weekday)
        const entry = states?.find((e) => e.day === weekday)
        return (
          <li key={weekday} className="flex items-start gap-3">
            <div className="flex w-16 shrink-0 flex-col pt-2.5">
              <span className="text-sm font-medium">
                {dayName(weekday, i18n.language, 'short')}
              </span>
              <span className="text-muted-foreground num text-xs">
                {date.format(new Date(Date.UTC(year, month - 1, day + offset)))}
              </span>
              <DayMark state={entry?.state} />
            </div>
            <div className="min-w-0 flex-1">
              {session ? (
                <SessionCard
                  session={session}
                  state={entry?.state}
                  adjusted={entry?.adjusted}
                  summaryId={entry?.sessionLogId}
                  pastLog={
                    // plain ISO dates compare as text; today's session is reached from Today
                    entry?.sessionLogId !== undefined &&
                    entry.startedAt !== undefined &&
                    today !== undefined &&
                    entry.date < today
                      ? { id: entry.sessionLogId, startedAt: entry.startedAt }
                      : undefined
                  }
                />
              ) : (
                <p className="text-muted-foreground flex min-h-12 items-center rounded-xl border border-dashed px-4 text-sm">
                  {t('plan.client.restDay')}
                </p>
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
