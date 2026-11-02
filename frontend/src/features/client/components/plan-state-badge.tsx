import type { ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { Client } from '../api/clients'

const VARIANTS: Record<
  Client['planState'],
  ComponentProps<typeof Badge>['variant']
> = {
  NO_QUIZ: 'outline',
  PLAN_TO_REVIEW: 'warning',
  DRAFT: 'outline',
  PUBLISHED: 'success',
}

export function PlanStateBadge({ state }: { state: Client['planState'] }) {
  const { t } = useTranslation()
  // the text carries the state; the colour only backs it up
  return (
    <Badge variant={VARIANTS[state]}>{t(`clients.planStates.${state}`)}</Badge>
  )
}
