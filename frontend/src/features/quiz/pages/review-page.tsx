import { ChevronLeftIcon, CircleAlertIcon } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { cn } from '@/lib/utils'
import { parseWeight, useWeightUnit } from '@/lib/weight'
import { useSubmitQuiz } from '../api/quiz'
import { QuizStatusCheck } from '../components/quiz-status-check'
import { loadDraft } from '../quiz-draft'
import {
  allowedStep,
  isComplete,
  LIKES,
  parseWholeNumber,
  STEP_COUNT,
} from '../quiz-schema'

/** The review of the seven answers; `settings` is shown at the header's right, given by the app. */
export function ReviewPage({ settings }: { settings?: ReactNode }) {
  return (
    <QuizStatusCheck status="NOT_SUBMITTED">
      <Review settings={settings} />
    </QuizStatusCheck>
  )
}

function Review({ settings }: { settings?: ReactNode }) {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const location = useLocation()
  const navigate = useNavigate()
  const [draft] = useState(() => loadDraft(unit))
  const submit = useSubmitQuiz()
  const headingRef = useRef<HTMLHeadingElement>(null)

  // a screen reader starts at the review's heading, as on every step (spec 50, FR-015)
  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  // stored before, so the status check moves on to the waiting state as after a successful submit
  const alreadySubmitted =
    problemCode(submit.error) === 'quiz.already-submitted'
  if (submit.isPending || submit.isSuccess || alreadySubmitted) {
    return <Building days={draft.answers.days} />
  }

  if (!isComplete(draft, unit)) {
    return (
      <Navigate
        replace
        to={`/app/quiz/${allowedStep(undefined, draft, unit)}`}
      />
    )
  }

  const { answers } = draft
  const notes = answers.notes.trim()
  const rows = [
    { key: 'goal', value: t(`quiz.goal.options.${answers.goal}.title`) },
    {
      key: 'experience',
      value: t(`quiz.experience.options.${answers.experience}.title`),
    },
    { key: 'level', value: t(`quiz.level.options.${answers.level}.title`) },
    {
      key: 'about',
      value: t('quiz.review.about', {
        count: parseWholeNumber(answers.age),
        height: parseWholeNumber(answers.height),
        // loadDraft gives the weight in the person's unit, converted if it was typed in the other one
        weight: parseWeight(answers.weight),
        // not `unit`: i18next hands its options to the number format, where `unit` means something else
        weightUnit: t(`quiz.about.weight.${unit}.unit`),
      }),
      measured: true,
    },
    {
      key: 'likes',
      // option titles hold "and" themselves, so a comma keeps the list readable (research R4)
      value: LIKES.filter((like) => answers.likes.includes(like))
        .map((like) => t(`quiz.likes.options.${like}.title`))
        .join(', '),
    },
    { key: 'days', value: String(answers.days), measured: true },
    { key: 'notes', value: notes || t('quiz.review.noNotes'), muted: !notes },
  ]

  function goBack() {
    // back through history when the client came from step 7, so the device's back stays in step
    const fromStep = (location.state as { fromStep?: number } | null)?.fromStep
    if (fromStep === STEP_COUNT) navigate(-1)
    else navigate(`/app/quiz/${STEP_COUNT}`, { replace: true })
  }

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col px-4">
      <header className="flex flex-col gap-2 pt-2">
        <div className="flex min-h-12 items-center justify-between">
          <p className="num text-muted-foreground text-xs tracking-wider uppercase">
            {t('quiz.review.label')}
          </p>
          <div className="flex items-center">
            <Button variant="ghost" className="h-12" onClick={goBack}>
              <ChevronLeftIcon data-icon="inline-start" aria-hidden />
              {t('quiz.back')}
            </Button>
            {settings}
          </div>
        </div>
        <div aria-hidden className="flex gap-1.5">
          {Array.from({ length: STEP_COUNT }, (_, index) => (
            <span key={index} className="bg-primary h-1 flex-1 rounded-full" />
          ))}
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-6 py-6">
        <div className="flex flex-col gap-2">
          <h1
            ref={headingRef}
            tabIndex={-1}
            className="font-heading text-3xl font-semibold outline-none"
          >
            {t('quiz.review.title')}
          </h1>
          <p className="text-muted-foreground text-sm">
            {t('quiz.review.hint')}
          </p>
        </div>
        <dl>
          {rows.map(({ key, value, measured, muted }, index) => (
            <div
              key={key}
              className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1 border-b py-2"
            >
              <dt className="text-muted-foreground font-mono text-xs tracking-wider uppercase">
                {t(`quiz.review.rows.${key}.label`)}
              </dt>
              <dd
                className={cn(
                  'min-w-0 wrap-break-word whitespace-pre-line',
                  measured && 'num',
                  muted && 'text-muted-foreground',
                )}
              >
                {value}
              </dd>
              <dd className="col-start-2 row-span-2 row-start-1">
                <Button
                  asChild
                  variant="link"
                  className="-mx-1 h-12 px-1 underline"
                >
                  <Link
                    to={`/app/quiz/${index + 1}`}
                    state={{ fromReview: true }}
                    aria-label={t(`quiz.review.rows.${key}.change`)}
                  >
                    {t('quiz.review.change')}
                  </Link>
                </Button>
              </dd>
            </div>
          ))}
        </dl>
      </main>
      <footer className="bg-background sticky bottom-0 flex flex-col gap-2 pt-3 pb-[max(1rem,env(safe-area-inset-bottom))]">
        {submit.isError && (
          <Alert variant="destructive">
            <CircleAlertIcon aria-hidden="true" />
            <AlertDescription>
              {t(errorMessageKey(submit.error))}
            </AlertDescription>
          </Alert>
        )}
        <Button
          size="touch"
          className="bg-brand text-brand-foreground hover:bg-brand/80 w-full"
          onClick={() => submit.mutate(draft)}
        >
          {submit.isError ? t('quiz.retry') : t('quiz.review.build')}
        </Button>
        <p className="text-muted-foreground text-center text-xs">
          {t('quiz.review.later')}
        </p>
      </footer>
    </div>
  )
}

// shown while the answers are on their way; nothing drafts a plan yet, so the reviewing state follows at once
function Building({ days }: { days: number }) {
  const { t } = useTranslation()
  const headingRef = useRef<HTMLHeadingElement>(null)

  // a screen reader reads the new state's heading (FR-017)
  useEffect(() => {
    headingRef.current?.focus()
  }, [])

  return (
    <main className="mx-auto flex min-h-svh max-w-[640px] flex-col gap-6 px-4 py-12">
      <div className="flex flex-col gap-2">
        <h1
          ref={headingRef}
          tabIndex={-1}
          className="font-heading text-3xl font-semibold outline-none"
        >
          {t('quiz.waiting.building.title')}
        </h1>
        <p className="text-muted-foreground">
          {t('quiz.waiting.building.text', { count: days })}
        </p>
      </div>
      {/* the plan summary's place, in the card's loading state (design system 6.1) */}
      <Card aria-hidden="true">
        <CardContent className="flex flex-col gap-3">
          {Array.from({ length: 3 }, (_, row) => (
            <Skeleton key={row} className="h-5 w-full" />
          ))}
        </CardContent>
      </Card>
    </main>
  )
}
