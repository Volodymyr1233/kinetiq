import { CircleAlertIcon } from 'lucide-react'
import { useState, type ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { AccountAvatar } from '@/components/account-avatar'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
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
import { formatDay } from '@/lib/dates'
import { firstName } from '@/lib/first-name'
import {
  planChangeBadgeVariant,
  planChangeStatus,
  waitsForDecisionOf,
} from '@/lib/plan-change-state'
import { useDraftedPlans, type DraftedPlan } from '../api/drafted-plans'
import {
  usePlanChangeApprovals,
  type PlanChangeApproval,
} from '../api/plan-change-approvals'

const KIND_VARIANTS: Record<
  DraftedPlan['kind'],
  ComponentProps<typeof Badge>['variant']
> = {
  BUILT: 'warning',
  FAILED: 'warning',
  SAVED: 'outline',
}

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

// the largest unit that fits, so a draft of 26 hours reads "1 day ago"
function age(createdAt: string, language: string) {
  const waited = Math.max(0, Date.now() - Date.parse(createdAt))
  const format = new Intl.RelativeTimeFormat(language, {
    numeric: 'auto',
    style: 'short',
  })
  if (waited < HOUR)
    return format.format(-Math.floor(waited / MINUTE), 'minute')
  if (waited < DAY) return format.format(-Math.floor(waited / HOUR), 'hour')
  return format.format(-Math.floor(waited / DAY), 'day')
}

function DraftedPlanRow({ draft }: { draft: DraftedPlan }) {
  const { t, i18n } = useTranslation()
  return (
    <li>
      <Link
        to={`/trainer/clients/${draft.clientId}/plan`}
        className="bg-card hover:bg-muted focus-visible:border-ring focus-visible:ring-ring/50 flex min-w-0 items-center gap-4 rounded-lg border px-5 py-4 outline-none focus-visible:ring-[3px]"
      >
        <AccountAvatar name={draft.clientName} />
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          {/* the age wraps under a long name, so a narrow screen still shows most of the name */}
          <p className="flex min-w-0 flex-wrap gap-x-2 text-sm">
            <span className="max-w-full truncate font-medium">
              {draft.clientName}
            </span>
            <span className="text-muted-foreground">
              {age(draft.createdAt, i18n.language)}
            </span>
          </p>
          <p className="text-muted-foreground text-sm">
            {t('approvals.meta', {
              weeks: t('approvals.weeks', { count: draft.weeks }),
              sessions: t('approvals.sessionsPerWeek', {
                count: draft.sessionsPerWeek,
              }),
            })}
          </p>
        </div>
        <Badge variant={KIND_VARIANTS[draft.kind]}>
          {t(`approvals.kinds.${draft.kind}`)}
        </Badge>
      </Link>
    </li>
  )
}

const TABS = ['you', 'client', 'decided'] as const
type Tab = (typeof TABS)[number]

type Item =
  | { kind: 'draft'; at: number; draft: DraftedPlan }
  | { kind: 'change'; at: number; change: PlanChangeApproval }

// where a plan change stands for the trainer: it waits for them, for the client, or it is decided
function tabOf(change: PlanChangeApproval): Tab {
  if (
    change.state === 'APPLIED' ||
    change.state === 'DECLINED' ||
    change.state === 'EXPIRED'
  ) {
    return 'decided'
  }
  return waitsForDecisionOf(change, 'TRAINER') ? 'you' : 'client'
}

function PlanChangeRow({ change }: { change: PlanChangeApproval }) {
  const { t, i18n } = useTranslation()
  const status = planChangeStatus(change, 'TRAINER')
  const name = firstName(change.clientName)
  const date = formatDay(change.effectiveWeek, i18n.language)
  let when: string
  if (change.state === 'DECLINED' && change.declineReason) {
    const reason = t(`planChange.reason.${change.declineReason}`)
    when =
      change.declinedBy === 'TRAINER'
        ? t('approvals.planChange.declinedByYou', { reason })
        : t('approvals.planChange.declinedByClient', { name, reason })
  } else if (change.state === 'EXPIRED') {
    when = t('approvals.planChange.expired', { date })
  } else {
    when = t('approvals.planChange.effective', { date })
  }

  return (
    <li>
      <Link
        to={`/trainer/plan-changes/${change.id}`}
        className="bg-card border-l-plan-change hover:bg-muted focus-visible:border-ring focus-visible:ring-ring/50 flex min-w-0 items-center gap-4 rounded-lg border border-l-4 px-5 py-4 outline-none focus-visible:ring-[3px]"
      >
        <AccountAvatar name={change.clientName} />
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <p className="flex min-w-0 flex-wrap gap-x-2 text-sm">
            <span className="max-w-full truncate font-medium">
              {change.clientName}
            </span>
            <span className="text-muted-foreground">
              {age(change.endedAt ?? change.createdAt, i18n.language)}
            </span>
          </p>
          <p className="font-medium">{change.title}</p>
          <p className="text-muted-foreground text-sm">{change.reason}</p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-2">
          <Badge variant={planChangeBadgeVariant[status]}>
            {t(`planChange.badge.${status}`, { name })}
          </Badge>
          <span className="text-muted-foreground text-xs">{when}</span>
        </div>
      </Link>
    </li>
  )
}

export function ApprovalsPage() {
  const { t } = useTranslation()
  const [tab, setTab] = useState<Tab>('you')
  const drafts = useDraftedPlans()
  const changes = usePlanChangeApprovals()
  const failed = [drafts, changes].find((query) => query.isError)
  const pending = drafts.isPending || changes.isPending

  // newest first; a plan change counts from when it ended, once it has
  const items: Item[] = [
    ...(drafts.data?.items ?? []).map((draft): Item => ({
      kind: 'draft',
      at: Date.parse(draft.createdAt),
      draft,
    })),
    ...(changes.data?.items ?? []).map((change): Item => ({
      kind: 'change',
      at: Date.parse(change.endedAt ?? change.createdAt),
      change,
    })),
  ].sort((a, b) => b.at - a.at)
  const inTab = (which: Tab) =>
    items.filter((item) =>
      item.kind === 'draft' ? which === 'you' : tabOf(item.change) === which,
    )

  return (
    <div className="flex max-w-240 flex-col gap-6 px-6 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">
          {t('approvals.title')}
        </h1>
        <p className="text-muted-foreground text-sm">{t('approvals.intro')}</p>
      </div>

      {failed && (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>
            {t(errorMessageKey(failed.error))}
          </AlertDescription>
          <AlertAction>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                void drafts.refetch()
                void changes.refetch()
              }}
            >
              {t('approvals.retry')}
            </Button>
          </AlertAction>
        </Alert>
      )}

      {pending && !failed ? (
        <div role="status" className="flex flex-col gap-2">
          <span className="sr-only">{t('approvals.loading')}</span>
          {Array.from({ length: 3 }, (_, row) => (
            <Skeleton key={row} aria-hidden className="h-18 w-full" />
          ))}
        </div>
      ) : (
        <Tabs value={tab} onValueChange={(next) => setTab(next as Tab)}>
          <TabsList
            variant="line"
            aria-label={t('approvals.tabs.label')}
            className="h-auto! max-w-full flex-wrap justify-start"
          >
            {TABS.map((which) => (
              <TabsTrigger key={which} value={which} className="flex-none">
                {t(`approvals.tabs.${which}`)}
                <span className="num text-muted-foreground">
                  {inTab(which).length}
                </span>
              </TabsTrigger>
            ))}
          </TabsList>
          {TABS.map((which) => (
            <TabsContent key={which} value={which} className="pt-3">
              {inTab(which).length === 0 ? (
                <Empty className="bg-muted border border-dashed">
                  <EmptyHeader>
                    <EmptyTitle>
                      {t(`approvals.empty.${which}.title`)}
                    </EmptyTitle>
                    <EmptyDescription>
                      {t(`approvals.empty.${which}.body`)}
                    </EmptyDescription>
                  </EmptyHeader>
                  {which === 'you' && (
                    <EmptyContent>
                      <Button variant="outline" size="sm" asChild>
                        <Link to="/trainer/clients">
                          {t('approvals.empty.action')}
                        </Link>
                      </Button>
                    </EmptyContent>
                  )}
                </Empty>
              ) : (
                <ul className="flex flex-col gap-2">
                  {inTab(which).map((item) =>
                    item.kind === 'draft' ? (
                      <DraftedPlanRow
                        key={`draft-${item.draft.clientId}`}
                        draft={item.draft}
                      />
                    ) : (
                      <PlanChangeRow
                        key={item.change.id}
                        change={item.change}
                      />
                    ),
                  )}
                </ul>
              )}
            </TabsContent>
          ))}
        </Tabs>
      )}
    </div>
  )
}
