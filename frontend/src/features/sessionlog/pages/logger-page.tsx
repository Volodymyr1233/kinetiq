import { ArrowLeftIcon, CircleAlertIcon, LockIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation, useParams } from 'react-router'
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { locksAt as lockMoment } from '@/lib/edit-window'
import { useSession, type SessionLog } from '../api/session-log'
import { FinishFooter } from '../components/finish-footer'
import { RestTimer } from '../components/rest-timer'
import { SetRow } from '../components/set-row'

// a rest day, a finished plan and no plan leave nothing to log: the client goes back to Today
const NOTHING_TO_LOG = ['session-log.no-session-today', 'plan.not-found']

// the Plan card passes its address, week included, so going back returns to that week
function planAddress(state: unknown): string | undefined {
  const from =
    typeof state === 'object' && state !== null && 'from' in state
      ? state.from
      : undefined
  return typeof from === 'string' && from.startsWith('/app/plan')
    ? from
    : undefined
}

/**
 * The session logger: today's session, or the client's session of the id in the address, with one row for each planned
 * set.
 */
export function LoggerPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const query = useSession(id)
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
    // an earlier session that is not the client's, or no longer there: back to where such sessions are listed
    if (
      id !== undefined &&
      problemCode(query.error) === 'session-log.not-found'
    ) {
      return <Navigate replace to="/app/plan" />
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

  // keyed, so another session starts with its own lock and panels
  return <Session key={session.id} session={session} />
}

function Session({ session }: { session: SessionLog }) {
  const { t, i18n } = useTranslation()
  const plan = planAddress(useLocation().state)
  // one entry panel is open at a time
  const [openSet, setOpenSet] = useState<string | null>(null)
  // when the rest after the last logged set ends; not kept, so a reload shows none
  const [restEndsAt, setRestEndsAt] = useState<number | null>(null)
  const locksAt = lockMoment(session.startedAt)
  const [lockedOnArrival] = useState(() => Date.now() >= locksAt)
  // locked while the page is open, by its timer or by the server; only this is announced
  const [lockedHere, setLockedHere] = useState(false)
  const locked = lockedOnArrival || lockedHere

  useEffect(() => {
    if (locked) return
    const timer = setTimeout(() => setLockedHere(true), locksAt - Date.now())
    return () => clearTimeout(timer)
  }, [locked, locksAt])

  // a plain date, read in UTC so no time zone can move the day
  const [year, month, day] = session.date.split('-').map(Number)
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)))

  return (
    <main className="mx-auto flex min-h-svh max-w-[640px] flex-col gap-4 px-4 pt-4 pb-0">
      <Button variant="ghost" size="touch" asChild className="self-start px-2">
        <Link to={plan ?? '/app/today'}>
          <ArrowLeftIcon aria-hidden />
          {t(plan === undefined ? 'sessionlog.back' : 'sessionlog.backToPlan')}
        </Link>
      </Button>
      <header className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold break-words">
          {session.name}
        </h1>
        <p className="text-muted-foreground">{date}</p>
      </header>
      {locked && (
        // not an alert: the switch is announced once by the status below, and on arrival it is just read
        <Alert variant="info" role={undefined}>
          <LockIcon aria-hidden />
          <AlertTitle>{t('sessionlog.locked.title')}</AlertTitle>
          <AlertDescription>{t('sessionlog.locked.text')}</AlertDescription>
        </Alert>
      )}
      <p role="status" className="sr-only">
        {lockedHere ? t('sessionlog.locked.title') : ''}
      </p>
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
            {entry.sets.map((set) => {
              const key = `${entry.position}-${set.number}`
              return (
                <SetRow
                  key={key}
                  sessionId={session.id}
                  entry={entry}
                  set={set}
                  open={openSet === key}
                  locked={locked}
                  onToggle={() => setOpenSet(openSet === key ? null : key)}
                  onLogged={(restSeconds) =>
                    setRestEndsAt(
                      restSeconds > 0 ? Date.now() + restSeconds * 1000 : null,
                    )
                  }
                  onLocked={() => setLockedHere(true)}
                />
              )
            })}
          </ol>
        </section>
      ))}
      {/* one sticky block, so the rest timer sits above the footer instead of over it */}
      <div className="sticky bottom-0 -mx-4 mt-auto flex flex-col gap-2 px-4">
        {!locked && restEndsAt !== null && (
          <RestTimer
            key={restEndsAt}
            endsAt={restEndsAt}
            onDismiss={() => setRestEndsAt(null)}
          />
        )}
        <FinishFooter
          session={session}
          locked={locked}
          onLocked={() => setLockedHere(true)}
        />
      </div>
    </main>
  )
}
