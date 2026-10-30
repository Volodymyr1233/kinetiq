import { QuizPage } from '@/features/quiz/pages/quiz-page'
import { ReviewPage } from '@/features/quiz/pages/review-page'
import { WaitingPage } from '@/features/quiz/pages/waiting-page'
import { useWeightUnit } from '@/lib/weight'
import { SettingsSheet } from './settings-sheet'

// the quiz has no client header, so each quiz page shows the Settings button in its own (spec 50, FR-031); a unit
// changed in the sheet remounts the steps and the review, which load the draft converted instead of keeping a number
// typed in the old unit

export function QuizStepsRoute() {
  const unit = useWeightUnit()
  return <QuizPage key={unit} settings={<SettingsSheet />} />
}

export function QuizReviewRoute() {
  const unit = useWeightUnit()
  return <ReviewPage key={unit} settings={<SettingsSheet />} />
}

export function QuizWaitingRoute() {
  return <WaitingPage settings={<SettingsSheet />} />
}
