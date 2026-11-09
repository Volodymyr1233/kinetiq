import { TodayPage } from '@/features/plan/pages/today-page'
import { useCheckIn } from '@/features/readiness/api/check-in'
import {
  AdjustmentMark,
  AdjustmentNote,
  AdjustmentReason,
} from '@/features/readiness/components/adjustment'
import { ReadinessSection } from '@/features/readiness/components/readiness-section'

/**
 * The Today tab: the plan's page, with today's check-in from readiness above the session and, on a recovery day,
 * readiness's lighter targets in it.
 */
export function TodayRoute() {
  const adjustment = useCheckIn().data?.checkIn?.adjustment
  const lighter = adjustment?.inUse ? adjustment : undefined

  return (
    <TodayPage
      beforeSession={<ReadinessSection />}
      adjusted={
        lighter && {
          targets: lighter.entries,
          mark: <AdjustmentMark />,
          reason: <AdjustmentReason causes={lighter.causes} />,
          note: <AdjustmentNote />,
        }
      }
    />
  )
}
