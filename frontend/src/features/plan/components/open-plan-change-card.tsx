import { PlanChangeCard } from '@/components/plan-change-card'
import { planChangeStatus } from '@/lib/plan-change-state'
import { useOpenPlanChange } from '../api/client-plan-change'

/** The client's open plan change, as a card at the top of Plan and Today; nothing while there is none. */
export function OpenPlanChangeCard() {
  const planChange = useOpenPlanChange().data
  if (!planChange) return null

  return (
    <PlanChangeCard
      title={planChange.title}
      reason={planChange.reason}
      effectiveWeek={planChange.effectiveWeek}
      to={`/app/plan/change/${planChange.id}`}
      status={planChangeStatus(planChange, 'CLIENT')}
      name={planChange.counterpartName}
    />
  )
}
