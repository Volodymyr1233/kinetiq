import type { ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { ClientStatus } from '../api/overview'

const VARIANTS: Record<ClientStatus, ComponentProps<typeof Badge>['variant']> =
  {
    AT_RISK: 'destructive',
    WATCH: 'warning',
    PLAN_TO_REVIEW: 'info',
    NO_PLAN: 'outline',
    ON_TRACK: 'secondary',
  }

export function StatusBadge({ status }: { status: ClientStatus }) {
  const { t } = useTranslation()
  // the name carries the status; the colour only backs it up
  return (
    <Badge variant={VARIANTS[status]}>{t(`overview.status.${status}`)}</Badge>
  )
}
