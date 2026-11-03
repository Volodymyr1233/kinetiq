import { useEffect, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { QuizStatusCheck } from '../components/quiz-status-check'

type Slots = { settings?: ReactNode; message?: ReactNode }

/**
 * The waiting state after submitting. The app gives `settings`, shown in a header above the heading, and `message`,
 * shown below the text.
 */
export function WaitingPage({ settings, message }: Slots) {
  return (
    <QuizStatusCheck status="WAITING_FOR_PLAN">
      <Reviewing settings={settings} message={message} />
    </QuizStatusCheck>
  )
}

// the board's trainer avatar and draft summary are not shown yet (spec 54, Clarifications)
function Reviewing({ settings, message }: Slots) {
  const { t } = useTranslation()
  const headingRef = useRef<HTMLHeadingElement>(null)

  // a screen reader reads the new state's heading (FR-017)
  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col px-4">
      <header className="flex min-h-12 justify-end pt-2">{settings}</header>
      <main className="flex flex-col gap-2 py-6">
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
        {message}
      </main>
    </div>
  )
}
