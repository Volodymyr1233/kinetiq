import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { QuizStatusCheck } from '../components/quiz-status-check'

export function WaitingPage() {
  return (
    <QuizStatusCheck status="WAITING_FOR_PLAN">
      <Reviewing />
    </QuizStatusCheck>
  )
}

// the board's trainer avatar, draft summary and message button wait for the trainer's name, plans and messaging
// (spec 54, Clarifications)
function Reviewing() {
  const { t } = useTranslation()
  const headingRef = useRef<HTMLHeadingElement>(null)

  // a screen reader reads the new state's heading (FR-017)
  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  return (
    <main className="mx-auto flex min-h-svh max-w-[640px] flex-col gap-2 px-4 py-12">
      <h1
        ref={headingRef}
        tabIndex={-1}
        className="font-heading text-3xl font-semibold outline-none"
      >
        {t('quiz.waiting.reviewing.title')}
      </h1>
      <p className="text-muted-foreground">
        {t('quiz.waiting.reviewing.text')}
      </p>
    </main>
  )
}
