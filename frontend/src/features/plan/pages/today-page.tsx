import { CircleAlertIcon } from 'lucide-react'
import { Link } from 'react-router'
import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { SessionStateBadge } from '@/components/session-state-badge'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useSessionStates } from '../api/session-states'
import { useToday } from '../api/today'
import { EntryRow } from '../components/entry-row'

/** The client's Today tab: the session their plan has for today, a rest day, or the end of the plan. */
export function TodayPage() {
  const { t, i18n } = useTranslation()
  const query = useToday()
  const states = useSessionStates().data?.sessions
  const why = useId()
  const today = query.data

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
  const [year, month, day] = today.date.split('-').map(Number)
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)))

  // the states are for the week, so today's is found by its date
  const entry = states?.find((e) => e.date === today.date)

  return (
    <div className="flex flex-col gap-4 px-4">
      <p className="text-muted-foreground">{date}</p>

      {today.state === 'SESSION' && today.session && (
        <>
          <section
            aria-labelledby={`${why}-name`}
            className="bg-card text-card-foreground rounded-xl border"
          >
            <h2
              id={`${why}-name`}
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
              {/* the logger arrives with story #92; until then the action is shown and cannot be used */}
              <Button
                size="touch"
                disabled
                aria-describedby={why}
                className="bg-brand text-brand-foreground hover:bg-brand/80"
              >
                {t('plan.today.start')}
              </Button>
              <p id={why} className="text-muted-foreground text-sm">
                {t('plan.today.startSoon')}
              </p>
            </div>
          )}
        </>
      )}

      {today.state === 'REST' && (
        <section className="flex flex-col gap-1 rounded-xl border border-dashed p-4">
          <h2 className="font-heading text-lg font-semibold">
            {t('plan.today.restDay')}
          </h2>
          <p className="text-muted-foreground">{t('plan.today.restText')}</p>
        </section>
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
