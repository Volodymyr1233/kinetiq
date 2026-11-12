import { CircleAlertIcon, UserPlusIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import { STATUSES, useOverview, type OverviewClient } from '../api/overview'
import { AttentionList } from '../components/attention-list'
import { ClientTable } from '../components/client-table'
import { OverviewTiles } from '../components/overview-tiles'

const TOP_ROWS = 8

// the most urgent first, then by name
function mostUrgent(clients: OverviewClient[], locale: string) {
  return [...clients]
    .sort(
      (a, b) =>
        STATUSES.indexOf(a.status) - STATUSES.indexOf(b.status) ||
        a.name.localeCompare(b.name, locale),
    )
    .slice(0, TOP_ROWS)
}

/** Today and its Monday-to-Sunday week, such as "Thursday, 12 November · week of 9–15 November". */
function useDateLine() {
  const { t, i18n } = useTranslation()
  const today = new Date()
  const monday = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate() - ((today.getDay() + 6) % 7),
  )
  const sunday = new Date(
    monday.getFullYear(),
    monday.getMonth(),
    monday.getDate() + 6,
  )
  const day = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(today)
  const week = new Intl.DateTimeFormat(i18n.language, {
    day: 'numeric',
    month: 'long',
  }).formatRange(monday, sunday)
  const line = t('overview.dateLine', { day, week })
  // Polish writes weekdays in lower case; the line starts a sentence
  return line.charAt(0).toLocaleUpperCase(i18n.language) + line.slice(1)
}

function Loading() {
  const { t } = useTranslation()
  return (
    <div aria-busy="true" className="flex flex-col gap-8">
      <span className="sr-only" role="status">
        {t('overview.loading')}
      </span>
      <div
        aria-hidden
        className="grid grid-cols-[repeat(auto-fit,minmax(13rem,1fr))] gap-4"
      >
        {Array.from({ length: 4 }, (_, tile) => (
          <Card key={tile} size="sm">
            <CardContent className="flex flex-col gap-2.5">
              <Skeleton className="h-3.5 w-1/2" />
              <Skeleton className="h-9 w-2/5" />
              <Skeleton className="h-3 w-3/4" />
            </CardContent>
          </Card>
        ))}
      </div>
      <div aria-hidden className="flex flex-col gap-2">
        <Skeleton className="h-5 w-40" />
        {Array.from({ length: 3 }, (_, alert) => (
          <Skeleton key={alert} className="h-16 w-full" />
        ))}
      </div>
      <div aria-hidden className="flex flex-col gap-2">
        {Array.from({ length: 5 }, (_, row) => (
          <Skeleton key={row} className="h-9 w-full" />
        ))}
      </div>
    </div>
  )
}

export function OverviewPage() {
  const { t, i18n } = useTranslation()
  const query = useOverview()
  const dateLine = useDateLine()
  const overview = query.data

  return (
    <div className="flex flex-col gap-8 px-6 py-8">
      <div className="flex flex-col gap-1">
        <p className="text-muted-foreground text-sm">{dateLine}</p>
        <h1 className="font-heading text-2xl font-semibold">
          {t('clients.title')}
        </h1>
      </div>

      {query.isError ? (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void query.refetch()}
            >
              {t('clients.retry')}
            </Button>
          </AlertAction>
        </Alert>
      ) : overview === undefined ? (
        <Loading />
      ) : overview.clients.length === 0 ? (
        <Empty className="bg-muted border border-dashed">
          <EmptyHeader>
            <EmptyTitle>{t('clients.empty.title')}</EmptyTitle>
            <EmptyDescription>{t('clients.empty.body')}</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button asChild>
              <Link to="/trainer/clients/invites">
                <UserPlusIcon data-icon="inline-start" aria-hidden="true" />
                {t('invites.inviteClient')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <>
          <OverviewTiles overview={overview} />
          <AttentionList alerts={overview.alerts} clients={overview.clients} />
          <section
            aria-labelledby="clients-top"
            className="flex flex-col gap-3"
          >
            <div className="flex items-center justify-between gap-4">
              <h2
                id="clients-top"
                className="font-heading text-lg font-semibold"
              >
                {t('overview.table.title')}
              </h2>
              <Button variant="link" size="sm" asChild>
                <Link to="/trainer/clients/all">
                  {t('overview.table.viewAll', {
                    count: overview.clients.length,
                  })}
                </Link>
              </Button>
            </div>
            <div className="rounded-lg border">
              <ClientTable
                clients={mostUrgent(overview.clients, i18n.language)}
              />
            </div>
          </section>
        </>
      )}
    </div>
  )
}
