import { useQuery } from '@tanstack/react-query'
import { conversationsQuery } from '@/features/messaging/api/messaging'
import { DraftCard } from '@/features/plan/components/draft-card'
import { QuizPage } from '@/features/quiz/pages/quiz-page'
import { ReviewPage } from '@/features/quiz/pages/review-page'
import { WaitingPage } from '@/features/quiz/pages/waiting-page'
import { useWeightUnit } from '@/lib/weight'
import { MessageTrainerSheet } from './message-trainer-sheet'
import { SettingsButton, SettingsSheet } from './settings-sheet'

// the quiz has no client header, so each quiz page shows the Settings button in its own (spec 50, FR-031); a unit
// changed in the sheet remounts the steps and the review, which load the draft converted instead of keeping a number
// typed in the old unit, while the sheet around them stays open

export function QuizStepsRoute() {
  const unit = useWeightUnit()
  return (
    <SettingsSheet>
      <QuizPage key={unit} settings={<SettingsButton />} />
    </SettingsSheet>
  )
}

export function QuizReviewRoute() {
  const unit = useWeightUnit()
  return (
    <SettingsSheet>
      <ReviewPage key={unit} settings={<SettingsButton />} />
    </SettingsSheet>
  )
}

export function QuizWaitingRoute() {
  // a client's one conversation is the one with their trainer; a failed read leaves the page saying "your trainer"
  const { data } = useQuery(conversationsQuery)

  return (
    <WaitingPage
      settings={<SettingsSheet />}
      message={<MessageTrainerSheet />}
      draftCard={<DraftCard />}
      trainerName={data?.conversations[0]?.name}
    />
  )
}
