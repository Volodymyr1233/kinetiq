import { CircleAlertIcon } from 'lucide-react'
import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useOutletContext } from 'react-router'
import { QuizAnswersList } from '@/components/quiz-answers'
import { SessionStateBadge } from '@/components/session-state-badge'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import {
  useClientOverview,
  type ClientOverview,
  type ClientSession,
} from '../api/client-training'
import { WeekDays } from '../components/week-days'

type Client = components['schemas']['ClientResponse']

/** The client page's Overview tab: this week, the recent sessions and the quiz answers. */
export function ClientOverviewTab() {
  const { t } = useTranslation()
  const client = useOutletContext<Client>()
  const query = useClientOverview(client.id)

  if (query.isError) {
    return (
      <Alert variant="destructive" className="mt-4">
        <CircleAlertIcon aria-hidden />
        <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        <AlertAction>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void query.refetch()}
          >
            {t('sessionlog.retry')}
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  if (query.data === undefined) return <OverviewLoading />

  const overview = query.data
  // a published plan never goes back to a draft, so one without a week has finished
  const published = client.planState === 'PUBLISHED'
  return (
    <div className="grid items-start gap-6 pt-4 lg:grid-cols-2">
      <OverviewCard title={t('sessionlog.overview.thisWeek')}>
        {!published ? (
          <Empty>{t('sessionlog.overview.noPlan')}</Empty>
        ) : overview.week === undefined ? (
          <Empty>{t('sessionlog.overview.finished')}</Empty>
        ) : (
          <ThisWeek week={overview.week} today={overview.today} />
        )}
      </OverviewCard>
      <OverviewCard
        title={t('sessionlog.overview.recentSessions')}
        action={
          <Button variant="outline" size="sm" asChild>
            <Link to="logs">{t('sessionlog.overview.allLogs')}</Link>
          </Button>
        }
      >
        {!published ? (
          <Empty>{t('sessionlog.overview.noPlan')}</Empty>
        ) : overview.recentSessions.length === 0 ? (
          <Empty>{t('sessionlog.overview.noSessions')}</Empty>
        ) : (
          <RecentSessions sessions={overview.recentSessions} />
        )}
      </OverviewCard>
      <OverviewCard title={t('sessionlog.overview.quizAnswers')}>
        {overview.quizAnswers === undefined ? (
          <Empty>{t('sessionlog.overview.noAnswers')}</Empty>
        ) : (
          <QuizAnswersList answers={overview.quizAnswers} name={client.name} />
        )}
      </OverviewCard>
    </div>
  )
}

function OverviewCard({
  title,
  action,
  children,
}: {
  title: string
  action?: ReactNode
  children: ReactNode
}) {
  const heading = useId()
  // a named region, so assistive technology can move from card to card
  return (
    <Card role="region" aria-labelledby={heading}>
      <CardHeader>
        <CardTitle>
          <h2 id={heading}>{title}</h2>
        </CardTitle>
        {action && <CardAction>{action}</CardAction>}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

function Empty({ children }: { children: ReactNode }) {
  return <p className="text-muted-foreground text-sm">{children}</p>
}

function useFormats() {
  const { i18n } = useTranslation()
  // plain dates, read in UTC so no time zone can move the day
  return {
    rpe: new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }),
    weekday: new Intl.DateTimeFormat(i18n.language, {
      weekday: 'short',
      timeZone: 'UTC',
    }),
    date: new Intl.DateTimeFormat(i18n.language, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    }),
  }
}

function ThisWeek({
  week,
  today,
}: {
  week: NonNullable<ClientOverview['week']>
  today: string
}) {
  const { t } = useTranslation()
  const format = useFormats()
  return (
    <>
      <WeekDays week={week} today={today} />
      <ul className="flex flex-col divide-y border-t">
        {week.sessions.map((session) => (
          <li
            key={session.date}
            className="flex items-center justify-between gap-4 py-2.5 text-sm"
          >
            <span className="min-w-0 break-words">
              {t('sessionlog.overview.sessionDay', {
                name: session.name,
                day: format.weekday.format(new Date(session.date)),
              })}
            </span>
            <span className="flex shrink-0 items-center gap-3">
              {session.averageRpe !== undefined && (
                <span className="text-muted-foreground num text-xs">
                  {t('sessionlog.overview.rpe', {
                    value: format.rpe.format(session.averageRpe),
                  })}
                </span>
              )}
              <SessionStateBadge
                state={session.state}
                locked={session.locked}
              />
            </span>
          </li>
        ))}
      </ul>
    </>
  )
}

function RecentSessions({ sessions }: { sessions: ClientSession[] }) {
  const { t } = useTranslation()
  const format = useFormats()
  return (
    <ul className="flex flex-col divide-y">
      {sessions.map((session) => (
        <li
          // a session never started has no log, and no log shares its date
          key={session.sessionLogId ?? session.date}
          className="flex items-center justify-between gap-4 py-2.5 text-sm"
        >
          <span className="flex min-w-0 flex-wrap items-baseline gap-x-2">
            <span className="font-semibold break-words">{session.name}</span>
            <span className="text-muted-foreground text-xs">
              {format.date.format(new Date(session.date))}
            </span>
          </span>
          <span className="flex shrink-0 items-center gap-3">
            <span className="text-muted-foreground num text-xs">
              {session.averageRpe === undefined
                ? t('sessionlog.overview.sets', {
                    logged: session.setsLogged,
                    planned: session.setsPlanned,
                  })
                : t('sessionlog.overview.setsAndRpe', {
                    logged: session.setsLogged,
                    planned: session.setsPlanned,
                    value: format.rpe.format(session.averageRpe),
                  })}
            </span>
            <SessionStateBadge state={session.state} locked={session.locked} />
          </span>
        </li>
      ))}
    </ul>
  )
}

function OverviewLoading() {
  const { t } = useTranslation()
  return (
    <>
      <span className="sr-only" role="status">
        {t('sessionlog.overview.loading')}
      </span>
      <div aria-hidden className="grid items-start gap-6 pt-4 lg:grid-cols-2">
        <Skeleton className="h-64" />
        <Skeleton className="h-64" />
        <Skeleton className="h-72" />
      </div>
    </>
  )
}
