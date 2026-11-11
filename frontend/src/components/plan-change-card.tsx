import { ArrowRightIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
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
import { formatDay } from '@/lib/dates'
import type { PlanChangeStatus } from '@/lib/plan-change-state'

/**
 * A plan change waiting for a decision, as a client page shows it: what changes, why, when, and a link to its detail.
 * `effectiveWeek` is the Monday it would take effect.
 */
export function PlanChangeCard({
  title,
  reason,
  effectiveWeek,
  to,
  status,
  name,
}: {
  title: string
  reason: string
  effectiveWeek: string
  to: string
  /** What it waits for; without it the badge only names the card. */
  status?: PlanChangeStatus
  /** The other person, for "Waiting for {{name}}". */
  name?: string
}) {
  const { t, i18n } = useTranslation()

  return (
    <Card className="border-l-plan-change border-l-4">
      <CardHeader>
        <CardTitle>
          <h2>{title}</h2>
        </CardTitle>
        <CardDescription>{reason}</CardDescription>
        <CardAction>
          <Badge variant="info">
            {status
              ? t(`planChange.badge.${status}`, { name })
              : t('planChange.card.label')}
          </Badge>
        </CardAction>
      </CardHeader>
      <CardContent className="flex flex-col items-start gap-3">
        <p className="text-muted-foreground text-sm">
          {t('planChange.card.effective', {
            date: formatDay(effectiveWeek, i18n.language),
          })}
        </p>
        <Button variant="outline" size="sm" asChild>
          <Link to={to}>
            {t('planChange.card.open')}
            <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
          </Link>
        </Button>
      </CardContent>
    </Card>
  )
}
