import { CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useOutletContext } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { usePlan } from '../api/plans'
import { PlanEditor } from '../components/plan-editor'

type Client = components['schemas']['ClientResponse']

export function PlanPage() {
  const client = useOutletContext<Client>()
  // nothing of one client's editor may stay for the next
  return <ClientPlan key={client.id} client={client} />
}

function ClientPlan({ client }: { client: Client }) {
  const { t } = useTranslation()
  const noQuiz = client.planState === 'NO_QUIZ'
  const query = usePlan(client.id, !noQuiz)
  // an untouched new plan is not stored, so leaving the tab shows "Build plan" again
  const [building, setBuilding] = useState(false)

  if (noQuiz) {
    return (
      <p className="text-muted-foreground py-4 text-sm">
        {t('plan.empty.noQuiz', { name: client.name })}
      </p>
    )
  }

  // a refresh that fails while the editor is open leaves the editor alone
  if (query.data === undefined) {
    return query.isError ? (
      <Alert variant="destructive" className="mt-4">
        <CircleAlertIcon aria-hidden />
        <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        <AlertAction>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void query.refetch()}
          >
            {t('plan.retry')}
          </Button>
        </AlertAction>
      </Alert>
    ) : (
      <PlanSkeleton />
    )
  }

  if (query.data === null && !building) {
    return (
      <Empty className="mt-4 border">
        <EmptyHeader>
          <EmptyTitle>{t('plan.empty.noPlan')}</EmptyTitle>
        </EmptyHeader>
        <EmptyContent>
          <Button onClick={() => setBuilding(true)}>{t('plan.build')}</Button>
        </EmptyContent>
      </Empty>
    )
  }

  return <PlanEditor client={client} plan={query.data} />
}

function PlanSkeleton() {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col gap-6 pt-4">
      <span className="sr-only" role="status">
        {t('plan.loading')}
      </span>
      <div aria-hidden className="flex flex-wrap items-end gap-4">
        <Skeleton className="h-14 w-56" />
        <Skeleton className="h-14 w-32" />
        <Skeleton className="ml-auto h-9 w-48" />
      </div>
      <Skeleton aria-hidden className="h-9 w-96 max-w-full" />
      <div aria-hidden className="flex flex-wrap items-start gap-6">
        <div className="flex min-w-0 flex-[999_1_600px] flex-col gap-2">
          {Array.from({ length: 5 }, (_, row) => (
            <Skeleton key={row} className="h-9 w-full" />
          ))}
        </div>
        <Skeleton className="h-40 max-w-full flex-[1_1_280px]" />
      </div>
    </div>
  )
}
