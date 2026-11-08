import { TodayPage } from '@/features/plan/pages/today-page'
import { ReadinessSection } from '@/features/readiness/components/readiness-section'

/** The Today tab: the plan's page, with today's check-in from readiness above the session. */
export function TodayRoute() {
  return <TodayPage beforeSession={<ReadinessSection />} />
}
