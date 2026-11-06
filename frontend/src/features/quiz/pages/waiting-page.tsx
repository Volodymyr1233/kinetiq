import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Spinner } from '@/components/ui/spinner'
import { useQuizStatusWhileDrafting } from '../api/quiz'
import { QuizStatusCheck } from '../components/quiz-status-check'

type Slots = { settings?: ReactNode; message?: ReactNode }

/**
 * The waiting state after submitting: a plan being built, then a trainer reviewing it. The app gives `settings`, shown
 * in a header above the heading, and `message`, shown below the text.
 */
export function WaitingPage({ settings, message }: Slots) {
  return (
    <QuizStatusCheck status={['DRAFTING_PLAN', 'WAITING_FOR_PLAN']}>
      <Waiting settings={settings} message={message} />
    </QuizStatusCheck>
  )
}

// the board's trainer avatar and draft summary are not shown (spec 54, Clarifications; spec 79, Departures)
function Waiting({ settings, message }: Slots) {
  const { t } = useTranslation()
  const { data: status } = useQuizStatusWhileDrafting()
  const building = status === 'DRAFTING_PLAN'
  const [startedBuilding] = useState(building)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const state = building ? 'building' : 'reviewing'

  // a screen reader reads the heading on arrival; later changes are announced, and focus stays where the client has it
  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col px-4">
      <header className="flex min-h-12 justify-end pt-2">{settings}</header>
      <main className="flex flex-col gap-2 py-6">
        {building && (
          <Spinner
            aria-hidden="true"
            role="presentation"
            className="mb-2 size-7"
          />
        )}
        <h1
          ref={headingRef}
          tabIndex={-1}
          className="font-heading text-3xl font-semibold outline-none"
        >
          {t(`quiz.waiting.${state}.title`)}
        </h1>
        <p className="text-muted-foreground">
          {t(`quiz.waiting.${state}.text`)}
        </p>
        {/* always there, because a live region announces changes only once it exists */}
        <p role="status" className="sr-only">
          {startedBuilding && !building && t('quiz.waiting.reviewing.title')}
        </p>
        {building && <PlanSkeleton />}
        {message}
      </main>
    </div>
  )
}

function PlanSkeleton() {
  return (
    <Card aria-hidden="true" className="my-4">
      <CardContent className="flex flex-col gap-3">
        <Skeleton className="h-3.5 w-2/5" />
        <Skeleton className="h-7 w-3/4" />
        <Skeleton className="h-12" />
        <Skeleton className="h-12" />
        <Skeleton className="h-12" />
      </CardContent>
    </Card>
  )
}
