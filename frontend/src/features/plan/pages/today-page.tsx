import { CircleAlertIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useOwnPlan } from '../api/client-plan'
import { useSessionStates, type SessionStateEntry } from '../api/session-states'
import { useToday } from '../api/today'
import { EntryRow } from '../components/entry-row'
import { SessionStateBadge } from '../components/session-state-badge'
import { estimatedMinutes } from '../plan-draft'

/**
 * The client's Today tab: the session their plan has for today, a rest day, or the end of the plan. `beforeSession` is
 * shown above the session, only on a day with one: the app puts today's check-in there, which belongs to another
 * feature.
 */
export function TodayPage({ beforeSession }: { beforeSession?: ReactNode }) {
  const { t, i18n } = useTranslation()
  const query = useToday()
  const states = useSessionStates().data?.sessions
  const today = query.data
  const name = useId()

  if (today === undefined) {
    if (query.isPending) {
      return (
        <div aria-hidden className="flex flex-col gap-3 px-4">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      )
    }
    // without a published plan the client belongs to the quiz or the waiting screen, which the area decides
    if (problemCode(query.error) === 'plan.not-found') {
      return (
        <p className="text-muted-foreground px-4 text-sm">
          {t('plan.client.none')}
        </p>
      )
    }
    return (
      <div className="px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('plan.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  // a plain date, read in UTC so no time zone can move the day
  const format = (isoDate: string, options: Intl.DateTimeFormatOptions) => {
    const [year, month, day] = isoDate.split('-').map(Number)
    return new Intl.DateTimeFormat(i18n.language, {
      ...options,
      timeZone: 'UTC',
    }).format(new Date(Date.UTC(year, month - 1, day)))
  }
  const date = format(today.date, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })

  // the states are for the week, so today's is found by its date
  const entry = states?.find((e) => e.date === today.date)

  return (
    <div className="flex flex-col gap-4 px-4">
      <p className="text-muted-foreground">{date}</p>

      {today.state === 'SESSION' && today.session && (
        <>
          {beforeSession}
          <section
            aria-labelledby={`${name}-name`}
            className="bg-card text-card-foreground rounded-xl border"
          >
            <h2
              id={`${name}-name`}
              className="font-heading px-4 pt-4 pb-3 text-lg font-semibold break-words"
            >
              <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                {today.session.name}
                <SessionStateBadge state={entry?.state} />
              </span>
            </h2>
            <ol className="flex flex-col divide-y border-t">
              {today.session.entries.map((entry, index) => (
                // the same exercise can appear twice in a session, so its place is its key
                <EntryRow key={index} entry={entry} />
              ))}
            </ol>
          </section>
          {entry?.state === 'DONE' && entry.sessionLogId !== undefined ? (
            <div className="flex flex-col gap-2">
              <p className="text-muted-foreground text-sm">
                {t('plan.today.done')}
              </p>
              <Button variant="outline" size="touch" asChild>
                <Link to={`/app/summary/${entry.sessionLogId}`}>
                  {t('plan.client.viewSummary')}
                </Link>
              </Button>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              <Button
                size="touch"
                asChild
                className="bg-brand text-brand-foreground hover:bg-brand/80"
              >
                <Link to="/app/session">{t('plan.today.start')}</Link>
              </Button>
            </div>
          )}
        </>
      )}

      {today.state === 'REST' && (
        <RestDay next={today.next} states={states} format={format} />
      )}

      {today.state === 'FINISHED' && (
        <section className="flex flex-col gap-1 rounded-xl border border-dashed p-4">
          <h2 className="font-heading text-lg font-semibold">
            {t('plan.client.finished.title')}
          </h2>
        </section>
      )}
    </div>
  )
}

type Format = (isoDate: string, options: Intl.DateTimeFormatOptions) => string

/** The rest-day card: what comes next and how the week is going. */
function RestDay({
  next,
  states,
  format,
}: {
  next: NonNullable<ReturnType<typeof useToday>['data']>['next']
  states: SessionStateEntry[] | undefined
  format: Format
}) {
  const { t } = useTranslation()
  // how many sessions the plan has in a week
  const planned = useOwnPlan().data?.sessions.length

  return (
    <>
      <section className="flex flex-col gap-3 rounded-xl border border-dashed p-4">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <h2 className="font-heading text-lg font-semibold">
            {t('plan.today.restDay')}
          </h2>
          <Badge variant="secondary">{t('plan.today.restBadge')}</Badge>
        </div>
        <p className="text-muted-foreground">{t('plan.today.restText')}</p>
        {next && (
          <p className="font-medium break-words">
            {t('plan.today.next', {
              name: next.session.name,
              date: format(next.date, {
                weekday: 'short',
                day: 'numeric',
                month: 'short',
              }),
              minutes: estimatedMinutes(
                next.session.entries.map((e) => ({
                  ...e,
                  rest: e.restSeconds,
                })),
              ),
            })}
          </p>
        )}
        <Button variant="outline" size="touch" asChild>
          <Link to="/app/plan">{t('plan.today.seeWeek')}</Link>
        </Button>
      </section>
      {states !== undefined && planned !== undefined && (
        <p className="text-muted-foreground text-sm">
          {t('plan.today.weekDone', {
            done: states.filter((s) => s.state === 'DONE').length,
            count: planned,
          })}
        </p>
      )}
    </>
  )
}
