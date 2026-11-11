import { ArrowLeftIcon, CircleAlertIcon, LockIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Link,
  Navigate,
  useLocation,
  useParams,
  useSearchParams,
} from 'react-router'
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { TargetChange } from '@/components/target-change'
import { ValueList } from '@/components/value-list'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { locksAt as lockMoment } from '@/lib/edit-window'
import { fromKg, useWeightUnit } from '@/lib/weight'
import { useOnline } from '@/lib/online'
import {
  usePendingLogs,
  useSession,
  type LoggedEntry,
  type SessionLog,
  type SyncState,
} from '../api/session-log'
import { isPlace, setPlaceOf } from '../marked-set'
import { FinishFooter } from '../components/finish-footer'
import { OfflineBanner } from '../components/offline-banner'
import { RestTimer } from '../components/rest-timer'
import { SetRow } from '../components/set-row'
import { SyncBadge } from '../components/sync-badge'

// the logged set a message is about, in the shape the conversation shows it; built here from the session on screen
type About = components['schemas']['AboutResponse']

// when the rest after the last logged set ends, by session: kept while the app runs, so the timer outlives a change of
// tab, but not a reload
const restEnds = new Map<string, number>()

// a rest day, a finished plan and no plan leave nothing to log: the client goes back to Today
const NOTHING_TO_LOG = ['session-log.no-session-today', 'plan.not-found']

// the Plan card passes its address, week included, so going back returns to that week; a message about a set passes
// the Messages tab
function planAddress(state: unknown): string | undefined {
  const from =
    typeof state === 'object' && state !== null && 'from' in state
      ? state.from
      : undefined
  return typeof from === 'string' &&
    (from.startsWith('/app/plan') || from === '/app/messages')
    ? from
    : undefined
}

const backLabel = (from: string | undefined) =>
  from === undefined
    ? 'sessionlog.back'
    : from === '/app/messages'
      ? 'nav.messages'
      : 'sessionlog.backToPlan'

/**
 * The session logger: today's session, or the client's session of the id in the address, with one row for each planned
 * set. With `onAsk`, each set the server has stored offers to ask the trainer about it.
 */
export function LoggerPage({ onAsk }: { onAsk?: (about: About) => void }) {
  const { t } = useTranslation()
  const { id } = useParams()
  // a second tab shows what the first stored, but only while nothing here waits to be sent: the answer would lack it
  const query = useSession(id, usePendingLogs().length === 0)
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
  return <Session key={session.id} session={session} onAsk={onAsk} />
}

function Session({
  session,
  onAsk,
}: {
  session: SessionLog
  onAsk?: (about: About) => void
}) {
  const { t, i18n } = useTranslation()
  const plan = planAddress(useLocation().state)
  // a message about a set links here with that set's place
  const marked = setPlaceOf(useSearchParams()[0].get('set'))
  // one entry panel is open at a time
  const [openSet, setOpenSet] = useState<string | null>(null)
  const [restEndsAt, showRest] = useState(
    () => restEnds.get(session.id) ?? null,
  )
  const setRestEndsAt = (endsAt: number | null) => {
    if (endsAt === null) restEnds.delete(session.id)
    else restEnds.set(session.id, endsAt)
    showRest(endsAt)
  }
  const locksAt = lockMoment(session.startedAt)
  const [lockedOnArrival] = useState(() => Date.now() >= locksAt)
  // locked while the page is open, by its timer or by the server; only this is announced
  const [lockedHere, setLockedHere] = useState(false)
  const online = useOnline()
  const pending = usePendingLogs(session.id)
  // a set the server refused as locked turns the page read-only, with the set still shown as not synced
  const refusedAsLocked = pending.some((log) => log.reason === 'locked')
  const locked = lockedOnArrival || lockedHere || refusedAsLocked

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
          {t(backLabel(plan))}
        </Link>
      </Button>
      <header className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold break-words">
          {session.name}
        </h1>
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <p className="text-muted-foreground">{date}</p>
          {/* once for the session: on a recovery day every exercise with a load or an RPE gets lighter */}
          {session.entries.some((entry) => entry.planned) && (
            <Badge variant="adjusted">{t('sessionlog.adjusted')}</Badge>
          )}
        </div>
      </header>
      {locked && (
        // not an alert: the switch is announced once by the status below, and on arrival it is just read
        <Alert variant="info" role={undefined}>
          <LockIcon aria-hidden />
          <AlertTitle>{t('sessionlog.locked.title')}</AlertTitle>
          <AlertDescription>{t('sessionlog.locked.text')}</AlertDescription>
        </Alert>
      )}
      {/* the banners are seen; this says a change once, without moving focus */}
      <p role="status" className="sr-only">
        {lockedHere || refusedAsLocked
          ? t('sessionlog.locked.title')
          : online
            ? ''
            : t('sessionlog.offline.announce')}
      </p>
      {!online && <OfflineBanner />}
      {session.entries.map((entry) => (
        // the same exercise can appear twice in a session, so its place is its key
        <section
          key={entry.position}
          aria-labelledby={`entry-${entry.position}`}
          className="bg-card text-card-foreground rounded-xl border"
        >
          <div className="flex items-start justify-between gap-2 px-4 pt-4 pb-3">
            <h2
              id={`entry-${entry.position}`}
              className="font-heading text-lg font-semibold break-words"
            >
              {entry.exerciseName}
            </h2>
            <ExerciseSync entry={entry} pending={pending} />
          </div>
          {entry.planned && (
            <LighterToday planned={entry.planned} entry={entry} />
          )}
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
                  marked={isPlace(marked, entry.position, set.number)}
                  locked={locked}
                  onToggle={() => setOpenSet(openSet === key ? null : key)}
                  onLogged={(restSeconds) =>
                    setRestEndsAt(
                      restSeconds > 0 ? Date.now() + restSeconds * 1000 : null,
                    )
                  }
                  onAsk={
                    onAsk &&
                    set.log &&
                    (() =>
                      onAsk({
                        setLogId: set.log!.id,
                        sessionLogId: session.id,
                        sessionName: session.name,
                        sessionDate: session.date,
                        exerciseName: entry.exerciseName,
                        entryPosition: entry.position,
                        setNumber: set.number,
                        rpe: set.log!.rpe,
                      }))
                  }
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

/**
 * On a recovery day, each target of the exercise that is lighter than planned, as planned → today; the sets below
 * already suggest today's values.
 */
function LighterToday({
  planned,
  entry,
}: {
  planned: NonNullable<LoggedEntry['planned']>
  entry: LoggedEntry
}) {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const today = entry.sets[0].target
  const amount = (value: number) =>
    t('sessionlog.row.amount', {
      amount: value,
      unit: t(`plan.entries.measures.${entry.measure}`),
    })
  const load = (loadKg: number | undefined) =>
    loadKg === undefined
      ? t('sessionlog.row.noLoad')
      : t('sessionlog.row.load', {
          load: fromKg(loadKg, unit),
          unit: t(`plan.units.${unit}`),
        })
  const rpe = (value: number) => t('sessionlog.row.rpe', { rpe: value })
  const changes = [
    [amount(planned.amount), amount(today.amount)],
    [load(planned.loadKg), load(today.loadKg)],
    [rpe(planned.rpe), rpe(today.rpe)],
  ].filter(([before, after]) => before !== after)

  return (
    <p className="-mt-1 px-4 pb-3">
      <ValueList className="text-muted-foreground text-sm">
        {changes.map(([before, after]) => (
          <TargetChange key={before} planned={before} today={after} />
        ))}
      </ValueList>
    </p>
  )
}

/** The worst state among the rows of an exercise: Not synced, then Saved on this phone, then Synced. */
function ExerciseSync({
  entry,
  pending,
}: {
  entry: LoggedEntry
  pending: ReturnType<typeof usePendingLogs>
}) {
  const states = entry.sets.flatMap((set): SyncState[] => {
    const kept = pending.find(
      (log) =>
        log.entryPosition === entry.position && log.setNumber === set.number,
    )
    if (kept) return [kept.state]
    return set.log === undefined ? [] : ['synced']
  })
  const worst = (['not-synced', 'saved', 'synced'] as const).find((state) =>
    states.includes(state),
  )
  return worst && <SyncBadge state={worst} />
}
