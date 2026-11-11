import { ArrowLeftIcon, CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemStatus } from '@/lib/api/problem'
import { planChangeStatus } from '@/lib/plan-change-state'
import {
  useAcceptPlanChange,
  useClientPlanChange,
  useKeepCurrentPlan,
  useUndoPlanChange,
  type DeclineReason,
} from '../api/client-plan-change'
import { PlanChangeDecisionPanel } from '../components/plan-change-decision-panel'
import { PlanChangePeople } from '../components/plan-change-people'
import { PlanChangeTable } from '../components/plan-change-table'
import { RuleChart } from '../components/rule-chart'

const BADGES = {
  waitingForBoth: 'info',
  waitingForYou: 'info',
  waitingFor: 'info',
  applied: 'success',
  declined: 'secondary',
  expired: 'outline',
} as const

/** A plan change of the client, at phone width: what changes and why, who decided what, and the decision. */
export function ClientPlanChangePage() {
  const { t } = useTranslation()
  const { id } = useParams()
  const query = useClientPlanChange(id)
  const accept = useAcceptPlanChange()
  const undo = useUndoPlanChange()
  const keep = useKeepCurrentPlan()
  // the last refusal; the screen itself keeps the last state the server confirmed
  const [error, setError] = useState<unknown>()
  const planChange = query.data

  // an unknown id, another client's plan change, and text that is no id at all
  const status = problemStatus(query.error)
  if (query.isError && (status === 404 || status === 400)) {
    return (
      <div className="px-4">
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>{t('planChange.client.notFound.title')}</EmptyTitle>
            <EmptyDescription>
              {t('planChange.client.notFound.body')}
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button variant="outline" size="touch" asChild>
              <Link to="/app/plan">
                {t('planChange.client.notFound.action')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    )
  }

  if (query.isError && !planChange) {
    return (
      <div className="px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('planChange.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  if (!planChange) {
    return (
      <div className="flex flex-col gap-4 px-4">
        <span className="sr-only" role="status">
          {t('planChange.loading')}
        </span>
        <Skeleton aria-hidden className="h-6 w-32" />
        <Skeleton aria-hidden className="h-8 w-full" />
        <Skeleton aria-hidden className="h-48 w-full" />
      </div>
    )
  }

  const badge = planChangeStatus(planChange, 'CLIENT')
  const busy = accept.isPending || undo.isPending || keep.isPending
  const run = <Variables,>(
    mutation: {
      mutate: (
        variables: Variables,
        options: { onSuccess: () => void; onError: (error: unknown) => void },
      ) => void
    },
    variables: Variables,
  ) =>
    mutation.mutate(variables, {
      onSuccess: () => setError(undefined),
      onError: setError,
    })

  return (
    <div className="flex flex-col gap-4 px-4">
      <Button variant="ghost" className="-ms-2 self-start" asChild>
        <Link to="/app/plan">
          <ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />
          {t('planChange.client.back')}
        </Link>
      </Button>
      <Card className="border-l-plan-change border-l-4">
        <CardHeader>
          <div>
            <Badge variant={BADGES[badge]}>
              {t(`planChange.badge.${badge}`, {
                name: planChange.counterpartName,
              })}
            </Badge>
          </div>
          <CardTitle>
            <h2 className="font-heading text-xl font-semibold">
              {planChange.title}
            </h2>
          </CardTitle>
          <CardDescription>{planChange.reason}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <RuleChart planChange={planChange} />
          <PlanChangeTable list planChange={planChange} />
          <p className="text-muted-foreground text-sm">
            {t('planChange.client.stays')}
          </p>
          <PlanChangePeople
            planChange={planChange}
            viewer="CLIENT"
            otherName={planChange.counterpartName}
          />
          <PlanChangeDecisionPanel
            planChange={planChange}
            viewer="CLIENT"
            otherName={planChange.counterpartName}
            planLink="/app/plan"
            busy={busy}
            error={error}
            onAccept={() => run(accept, { id: planChange.id })}
            onUndo={() => run(undo, { id: planChange.id })}
            onKeep={(reason: DeclineReason, note: string | undefined) =>
              keep
                .mutateAsync({ id: planChange.id, reason, note })
                .then(() => {
                  setError(undefined)
                  return true
                })
                .catch((failure: unknown) => {
                  setError(failure)
                  return false
                })
            }
          />
        </CardContent>
      </Card>
    </div>
  )
}
