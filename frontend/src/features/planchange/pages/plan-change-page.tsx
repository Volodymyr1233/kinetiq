import { ArrowLeftIcon, CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
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
import { formatDay } from '@/lib/dates'
import { usePlanChange, type PlanChange } from '../api/plan-change'
import { PlanChangeTable } from '../components/plan-change-table'
import { RuleChart } from '../components/rule-chart'

const BADGES = {
  PENDING: 'info',
  ACCEPTED_BY_ONE: 'info',
  APPLIED: 'success',
  DECLINED: 'secondary',
  EXPIRED: 'outline',
} as const satisfies Record<PlanChange['state'], string>

/** A plan change of one of the trainer's clients, to read. Deciding on it comes with the approvals. */
export function PlanChangePage() {
  const { t, i18n } = useTranslation()
  const { id } = useParams()
  const query = usePlanChange(id)
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

  const concerned = new Intl.ListFormat(i18n.language, {
    type: 'conjunction',
  }).format(planChange.concernedExercises.map((exercise) => exercise.name))

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <Button variant="ghost" size="sm" className="self-start" asChild>
        <Link to={`/trainer/clients/${planChange.clientId}`}>
          <ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />
          {t('planChange.back', { client: planChange.clientName })}
        </Link>
      </Button>
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
              <Badge variant={BADGES[planChange.state]}>
                {t(`planChange.states.${planChange.state}`)}
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
            <p className="text-sm">
              {t('planChange.effective', {
                date: formatDay(planChange.effectiveWeek, i18n.language),
                client: planChange.clientName,
              })}
            </p>
          </CardContent>
        </Card>
        <RuleChart planChange={planChange} />
      </div>
    </div>
  )
}
