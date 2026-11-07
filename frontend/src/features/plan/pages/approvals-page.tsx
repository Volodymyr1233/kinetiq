import { CircleAlertIcon } from 'lucide-react'
import type { ComponentProps } from 'react'
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
import { errorMessageKey } from '@/lib/api/problem'
import { useDraftedPlans, type DraftedPlan } from '../api/drafted-plans'

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

export function ApprovalsPage() {
  const { t } = useTranslation()
  const query = useDraftedPlans()
  const drafts = query.data?.items ?? []

  return (
    <div className="flex max-w-240 flex-col gap-6 px-6 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">
          {t('approvals.title')}
        </h1>
        <p className="text-muted-foreground text-sm">{t('approvals.intro')}</p>
      </div>

      <section aria-labelledby="drafted-plans" className="flex flex-col gap-3">
        <h2 id="drafted-plans" className="font-heading text-lg font-medium">
          {t('approvals.draftedPlans')}
        </h2>
        {query.isError ? (
          <Alert variant="destructive">
            <CircleAlertIcon aria-hidden />
            <AlertDescription>
              {t(errorMessageKey(query.error))}
            </AlertDescription>
            <AlertAction>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void query.refetch()}
              >
                {t('approvals.retry')}
              </Button>
            </AlertAction>
          </Alert>
        ) : query.isPending ? (
          <div role="status" className="flex flex-col gap-2">
            <span className="sr-only">{t('approvals.loading')}</span>
            {Array.from({ length: 3 }, (_, row) => (
              <Skeleton key={row} aria-hidden className="h-18 w-full" />
            ))}
          </div>
        ) : drafts.length === 0 ? (
          <Empty className="bg-muted border border-dashed">
            <EmptyHeader>
              <EmptyTitle>{t('approvals.empty.title')}</EmptyTitle>
              <EmptyDescription>{t('approvals.empty.body')}</EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <Button variant="outline" size="sm" asChild>
                <Link to="/trainer/clients">{t('approvals.empty.action')}</Link>
              </Button>
            </EmptyContent>
          </Empty>
        ) : (
          <ul className="flex flex-col gap-2">
            {drafts.map((draft) => (
              <DraftedPlanRow key={draft.clientId} draft={draft} />
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
