import { CircleAlertIcon } from 'lucide-react'
import { Fragment, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useOutletContext } from 'react-router'
import { SessionStateBadge } from '@/components/session-state-badge'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { locksAt } from '@/lib/edit-window'
import { firstName } from '@/lib/first-name'
import { cn } from '@/lib/utils'
import {
  useClientSessionLog,
  useClientSessions,
  type ClientSession,
  type ClientSessionLog,
} from '../api/client-training'
import { SessionLogTable } from '../components/session-log-table'

type Client = components['schemas']['ClientResponse']

// a session never started has no log, and no log shares its date
const keyOf = (session: ClientSession) => session.sessionLogId ?? session.date

/** The client page's Logs tab: every session, newest first, beside the chosen one's sets. */
export function ClientLogsTab() {
  const client = useOutletContext<Client>()
  // the session chosen for one client is not carried over to the next
  return (
    <Fragment key={client.id}>
      <Logs client={client} />
    </Fragment>
  )
}

function Logs({ client }: { client: Client }) {
  const { t } = useTranslation()
  const query = useClientSessions(client.id)
  const [chosen, setChosen] = useState<string>()

  if (query.isError) {
    return <LoadFailed error={query.error} onRetry={query.refetch} />
  }
  if (query.data === undefined) {
    return (
      <>
        <span className="sr-only" role="status">
          {t('sessionlog.logs.loading')}
        </span>
        <div
          aria-hidden
          className="grid items-start gap-6 pt-4 lg:grid-cols-[20rem_1fr]"
        >
          <Skeleton className="h-72" />
          <Skeleton className="h-96" />
        </div>
      </>
    )
  }

  const sessions = query.data
  if (sessions.length === 0) {
    return (
      <p className="text-muted-foreground py-4 text-sm">
        {t('sessionlog.overview.noSessions')}
      </p>
    )
  }
  const selected =
    sessions.find((session) => keyOf(session) === chosen) ?? sessions[0]

  return (
    <div className="grid items-start gap-6 pt-4 lg:grid-cols-[20rem_1fr]">
      <Card className="py-2">
        <CardContent className="px-2">
          <ul
            aria-label={t('sessionlog.logs.sessions')}
            className="flex flex-col gap-1"
          >
            {sessions.map((session) => (
              <li key={keyOf(session)}>
                <SessionButton
                  session={session}
                  current={session === selected}
                  onChoose={() => setChosen(keyOf(session))}
                />
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
      <ChosenSession client={client} session={selected} />
    </div>
  )
}

function useFormats() {
  const { i18n } = useTranslation()
  return {
    rpe: new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }),
    // plain dates, read in UTC so no time zone can move the day
    date: new Intl.DateTimeFormat(i18n.language, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    }),
    // the moment a log locks, in the trainer's own time zone
    time: new Intl.DateTimeFormat(i18n.language, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: 'numeric',
      minute: '2-digit',
    }),
  }
}

function SessionButton({
  session,
  current,
  onChoose,
}: {
  session: ClientSession
  current: boolean
  onChoose: () => void
}) {
  const { t } = useTranslation()
  const format = useFormats()
  const date = format.date.format(new Date(session.date))
  return (
    <button
      type="button"
      aria-current={current ? 'true' : undefined}
      onClick={onChoose}
      className={cn(
        'hover:bg-muted focus-visible:ring-ring/50 flex w-full items-center justify-between gap-3 rounded-md border-l-2 border-transparent px-3 py-2 text-left text-sm outline-none focus-visible:ring-3',
        current && 'bg-muted border-primary',
      )}
    >
      <span className="flex min-w-0 flex-col">
        <span className="font-semibold break-words">{session.name}</span>
        <span className="text-muted-foreground num text-xs">
          {session.averageRpe === undefined
            ? t('sessionlog.logs.rowNoSets', { date })
            : t('sessionlog.logs.row', {
                date,
                value: format.rpe.format(session.averageRpe),
              })}
        </span>
      </span>
      <SessionStateBadge state={session.state} locked={session.locked} />
    </button>
  )
}

function ChosenSession({
  client,
  session,
}: {
  client: Client
  session: ClientSession
}) {
  const { t } = useTranslation()
  const format = useFormats()
  const query = useClientSessionLog(client.id, session.sessionLogId)
  const log = query.data
  const figures = (log: ClientSessionLog) => {
    const sets = { logged: log.setsLogged, planned: log.setsPlanned }
    // a finished session always has a logged set, so it always has an RPE
    if (log.averageRpe === undefined) return t('sessionlog.logs.figures', sets)
    return t(
      log.minutes === undefined
        ? 'sessionlog.logs.figuresRpe'
        : 'sessionlog.logs.figuresFinished',
      {
        ...sets,
        value: format.rpe.format(log.averageRpe),
        minutes: log.minutes,
      },
    )
  }

  return (
    // stays in view below the app bar (h-14) while a long list scrolls
    <Card className="lg:sticky lg:top-20">
      <CardHeader>
        <CardTitle>
          <h2>
            {t('sessionlog.logs.title', {
              name: session.name,
              date: format.date.format(new Date(session.date)),
            })}
          </h2>
        </CardTitle>
        {log && (
          <CardDescription className="num">{figures(log)}</CardDescription>
        )}
      </CardHeader>
      <CardContent className="gap-4">
        {session.sessionLogId === undefined ? (
          <p className="text-muted-foreground text-sm">
            {t('sessionlog.logs.notStarted')}
          </p>
        ) : query.isError ? (
          <LoadFailed error={query.error} onRetry={query.refetch} />
        ) : log === undefined ? (
          <>
            <span className="sr-only" role="status">
              {t('sessionlog.logs.loadingSession')}
            </span>
            <Skeleton aria-hidden className="h-64" />
          </>
        ) : (
          <>
            {log.note !== undefined && (
              <div className="bg-muted flex flex-col gap-1 rounded-md p-3 text-sm">
                <p className="text-muted-foreground">
                  {t('sessionlog.logs.noteFrom', {
                    name: firstName(client.name),
                  })}
                </p>
                {/* as the client typed it, never as markup */}
                <p className="wrap-anywhere whitespace-pre-wrap">{log.note}</p>
              </div>
            )}
            <SessionLogTable entries={log.entries} />
            {!log.locked && (
              <p className="text-muted-foreground text-sm">
                {t('sessionlog.logs.changeable', {
                  name: firstName(client.name),
                  time: format.time.format(locksAt(log.startedAt)),
                })}
              </p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

function LoadFailed({
  error,
  onRetry,
}: {
  error: unknown
  onRetry: () => unknown
}) {
  const { t } = useTranslation()
  return (
    <Alert variant="destructive" className="mt-4">
      <CircleAlertIcon aria-hidden />
      <AlertDescription>{t(errorMessageKey(error))}</AlertDescription>
      <AlertAction>
        <Button variant="outline" size="sm" onClick={() => void onRetry()}>
          {t('sessionlog.retry')}
        </Button>
      </AlertAction>
    </Alert>
  )
}
