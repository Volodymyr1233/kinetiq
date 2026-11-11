import { CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardAction,
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
import { firstName } from '@/lib/first-name'
import {
  planChangeBadgeVariant,
  planChangeStatus,
} from '@/lib/plan-change-state'
import type { DeclineReason } from '../api/client-plan-change'
import {
  useAcceptPlanChange,
  useKeepCurrentPlanChange,
  usePlanChange,
  useUndoPlanChange,
} from '../api/plan-change'
import { PlanChangeDecisionPanel } from '../components/plan-change-decision-panel'
import { PlanChangePeople } from '../components/plan-change-people'
import { PlanChangeTable } from '../components/plan-change-table'
import { RuleChart } from '../components/rule-chart'

/** A plan change of one of the trainer's clients: what changes and why, who decided what, and the decision. */
export function PlanChangePage() {
  const { t, i18n } = useTranslation()
  const { id } = useParams()
  const query = usePlanChange(id)
  const accept = useAcceptPlanChange()
  const undo = useUndoPlanChange()
  const keep = useKeepCurrentPlanChange()
  // the last refusal; the screen itself keeps the last state the server confirmed
  const [error, setError] = useState<unknown>()
  const planChange = query.data

  // an unknown id, another trainer's client's plan change, and text that is no id at all
  const status = problemStatus(query.error)
  if (query.isError && (status === 404 || status === 400)) {
    return (
      <div className="flex flex-col gap-6 px-6 py-8">
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>{t('planChange.notFound.title')}</EmptyTitle>
            <EmptyDescription>{t('planChange.notFound.body')}</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button variant="outline" size="sm" asChild>
              <Link to="/trainer/clients">
                {t('planChange.notFound.action')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    )
  }

  if (query.isError) {
    return (
      <div className="flex flex-col gap-6 px-6 py-8">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void query.refetch()}
            >
              {t('planChange.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  if (!planChange) {
    return (
      <div className="flex flex-col gap-4 px-6 py-8">
        <span className="sr-only" role="status">
          {t('planChange.loading')}
        </span>
        <Skeleton aria-hidden className="h-6 w-40" />
        <Skeleton aria-hidden className="h-8 w-96" />
        <Skeleton aria-hidden className="h-48 w-full" />
      </div>
    )
  }

  const badge = planChangeStatus(planChange, 'TRAINER')
  const busy = accept.isPending || undo.isPending || keep.isPending
  const run = (
    mutation: {
      mutate: (
        variables: { id: string },
        options: { onSuccess: () => void; onError: (error: unknown) => void },
      ) => void
    },
    variables: { id: string },
  ) =>
    mutation.mutate(variables, {
      onSuccess: () => setError(undefined),
      onError: setError,
    })

  const concerned = new Intl.ListFormat(i18n.language, {
    type: 'conjunction',
  }).format(planChange.concernedExercises.map((exercise) => exercise.name))

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <Breadcrumb aria-label={t('planChange.breadcrumb')}>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link to="/trainer/approvals">{t('nav.approvals')}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link to={`/trainer/clients/${planChange.clientId}`}>
                {planChange.clientName}
              </Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>{t('planChange.title')}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>
      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <Card className="border-l-plan-change border-l-4">
          <CardHeader>
            <CardDescription>
              {t('planChange.forClient', { client: planChange.clientName })}
            </CardDescription>
            <CardTitle>
              <h1 className="font-heading text-2xl font-semibold">
                {planChange.title}
              </h1>
            </CardTitle>
            <CardDescription>{planChange.reason}</CardDescription>
            <CardAction>
              <Badge variant={planChangeBadgeVariant[badge]}>
                {t(`planChange.badge.${badge}`, {
                  name: firstName(planChange.clientName),
                })}
              </Badge>
            </CardAction>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <PlanChangeTable planChange={planChange} />
            <p className="text-muted-foreground text-sm">
              {t(`planChange.notes.${planChange.rule}`, {
                exercises: concerned,
              })}
            </p>
            <PlanChangePeople
              planChange={planChange}
              viewer="TRAINER"
              otherName={planChange.clientName}
            />
            <PlanChangeDecisionPanel
              planChange={planChange}
              viewer="TRAINER"
              otherName={planChange.clientName}
              planLink={`/trainer/clients/${planChange.clientId}/plan`}
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
        <RuleChart planChange={planChange} />
      </div>
    </div>
  )
}
