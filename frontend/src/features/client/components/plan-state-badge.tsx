import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { Client } from '../api/clients'

export function PlanStateBadge({ state }: { state: Client['planState'] }) {
  const { t } = useTranslation()
  // the text carries the state; amber only backs it up
  return (
    <Badge variant={state === 'PLAN_TO_REVIEW' ? 'warning' : 'outline'}>
      {t(`clients.planStates.${state}`)}
    </Badge>
  )
}
