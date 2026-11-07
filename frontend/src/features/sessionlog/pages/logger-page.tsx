import { ArrowLeftIcon, CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useSession } from '../api/session-log'
import { SetRow } from '../components/set-row'

// a rest day, a finished plan and no plan leave nothing to log: the client goes back to Today
const NOTHING_TO_LOG = ['session-log.no-session-today', 'plan.not-found']

/** The session logger: today's session with one row for each planned set. */
export function LoggerPage() {
  const { t, i18n } = useTranslation()
  const query = useSession()
  const session = query.data

  if (session === undefined) {
    if (query.isPending) {
      return (
        <main className="mx-auto flex max-w-[640px] flex-col gap-3 px-4 py-6">
          <p role="status" className="sr-only">
            {t('sessionlog.loading')}
          </p>
          <div aria-hidden className="flex flex-col gap-3">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        </main>
      )
    }
    if (NOTHING_TO_LOG.includes(problemCode(query.error) ?? '')) {
      return <Navigate replace to="/app/today" />
    }
    return (
      <main className="mx-auto flex min-h-svh max-w-[640px] flex-col justify-center gap-4 px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('sessionlog.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </main>
    )
  }

  // a plain date, read in UTC so no time zone can move the day
  const [year, month, day] = session.date.split('-').map(Number)
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)))

  return (
    <main className="mx-auto flex max-w-[640px] flex-col gap-4 px-4 pt-4 pb-10">
      <Button variant="ghost" size="touch" asChild className="self-start px-2">
        <Link to="/app/today">
          <ArrowLeftIcon aria-hidden />
          {t('sessionlog.back')}
        </Link>
      </Button>
      <header className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold break-words">
          {session.name}
        </h1>
        <p className="text-muted-foreground">{date}</p>
      </header>
      {session.entries.map((entry) => (
        // the same exercise can appear twice in a session, so its place is its key
        <section
          key={entry.position}
          aria-labelledby={`entry-${entry.position}`}
          className="bg-card text-card-foreground rounded-xl border"
        >
          <h2
            id={`entry-${entry.position}`}
            className="font-heading px-4 pt-4 pb-3 text-lg font-semibold break-words"
          >
            {entry.exerciseName}
          </h2>
          <ol className="flex flex-col divide-y border-t">
            {entry.sets.map((set) => (
              <SetRow key={set.number} entry={entry} set={set} />
            ))}
          </ol>
        </section>
      ))}
    </main>
  )
}
