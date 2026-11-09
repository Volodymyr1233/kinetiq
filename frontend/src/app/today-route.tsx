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
 * readiness's lighter targets in it, or the way back to them once the client chose the original ones.
 */
export function TodayRoute() {
  const today = useCheckIn().data
  const adjustment = today?.checkIn?.adjustment
  // with the original targets and nothing left to switch, the session reads as planned
  const shown = adjustment && (adjustment.inUse || today.open)

  return (
    <TodayPage
      beforeSession={<ReadinessSection />}
      adjusted={
        shown
          ? {
              mark: (
                <AdjustmentMark adjustment={adjustment} open={today.open} />
              ),
              ...(adjustment.inUse && {
                targets: adjustment.entries,
                reason: <AdjustmentReason causes={adjustment.causes} />,
                note: <AdjustmentNote />,
              }),
            }
          : undefined
      }
    />
  )
}
