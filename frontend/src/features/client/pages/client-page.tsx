import { ArrowLeftIcon, CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet, useLocation, useNavigate, useParams } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { errorMessageKey } from '@/lib/api/problem'
import { isNotFound, useClient } from '../api/clients'
import { PlanStateBadge } from '../components/plan-state-badge'

const TABS = ['overview', 'plan', 'logs', 'messages'] as const

export function ClientPage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const query = useClient(id)
  const client = query.data
  // /trainer/clients/<id>/<tab>; the address without a tab is the overview
  const segment = pathname.split('/')[4]
  const active = TABS.find((tab) => tab === segment) ?? 'overview'

  const back = (
    <Button variant="ghost" size="sm" className="self-start" asChild>
      <Link to="/trainer/clients">
        <ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />
        {t('clients.page.back')}
      </Link>
    </Button>
  )

  if (query.isError && isNotFound(query.error)) {
    return (
      <div className="flex flex-col gap-6 px-6 py-8">
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>{t('clients.page.notFound.title')}</EmptyTitle>
            <EmptyDescription>
              {t('clients.page.notFound.body')}
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button variant="outline" size="sm" asChild>
              <Link to="/trainer/clients">
                {t('clients.page.notFound.action')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      {back}
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
      ) : !client ? (
        <div className="flex flex-col gap-2">
          <span className="sr-only" role="status">
            {t('clients.page.loading')}
          </span>
          <Skeleton aria-hidden className="h-8 w-60" />
          <Skeleton aria-hidden className="h-5 w-80" />
        </div>
      ) : (
        <>
          <header className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <h1 className="font-heading text-2xl font-semibold">
              {client.name}
            </h1>
            <PlanStateBadge state={client.planState} />
            <p className="text-muted-foreground w-full text-sm">
              {t('clients.page.goal', {
                goal: client.goal
                  ? t(`quiz.goal.options.${client.goal}.title`)
                  : t('clients.noValue'),
              })}
            </p>
          </header>
          {/* each tab is its own address, so a reload or a shared link lands on the same tab */}
          <Tabs
            value={active}
            onValueChange={(tab) =>
              void navigate(tab === 'overview' ? '.' : tab)
            }
          >
            <TabsList variant="line" aria-label={t('clients.page.tabsLabel')}>
              {TABS.map((tab) => (
                <TabsTrigger key={tab} value={tab}>
                  {t(`clients.page.tabs.${tab}`)}
                </TabsTrigger>
              ))}
            </TabsList>
            <TabsContent value={active}>
              <Outlet />
            </TabsContent>
          </Tabs>
        </>
      )}
    </div>
  )
}

// the four tabs stay empty until their features land
export function ClientTabEmpty() {
  const { t } = useTranslation()
  return (
    <p className="text-muted-foreground py-4 text-sm">
      {t('clients.page.tabEmpty')}
    </p>
  )
}
