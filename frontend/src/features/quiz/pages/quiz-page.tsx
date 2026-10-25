import { zodResolver } from '@hookform/resolvers/zod'
import { ChevronLeftIcon } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation, useNavigate, useParams } from 'react-router'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useWeightUnit } from '@/lib/weight'
import { MultipleChoice } from '../components/multiple-choice'
import { NotesField } from '../components/notes-field'
import { NumberField } from '../components/number-field'
import { SingleChoice } from '../components/single-choice'
import { Stepper } from '../components/stepper'
import { loadDraft, saveDraft } from '../quiz-draft'
import {
  allowedStep,
  answersSchemaFor,
  DAYS_MAX,
  DAYS_MIN,
  EXPERIENCES,
  GOALS,
  isStepValid,
  LEVELS,
  LIKES,
  NOTES_MAX_LENGTH,
  STEP_COUNT,
  type QuizAnswers,
} from '../quiz-schema'

const HEADING_ID = 'quiz-step-heading'
const STEP_KEYS = [
  'goal',
  'experience',
  'level',
  'about',
  'likes',
  'days',
  'notes',
] as const

type OptionStep = 'goal' | 'experience' | 'level' | 'likes'

export function QuizPage() {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const params = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [draft] = useState(loadDraft)
  const [reached, setReached] = useState(draft.reached)
  const form = useForm<QuizAnswers, unknown, QuizAnswers>({
    mode: 'onTouched',
    resolver: zodResolver(answersSchemaFor(unit)),
    defaultValues: draft.answers,
  })
  const answers = useWatch({ control: form.control })
  const headingRef = useRef<HTMLHeadingElement>(null)
  const step = allowedStep(params.step, { answers, reached }, unit)
  const state = location.state as {
    fromStep?: number
    fromReview?: boolean
  } | null

  // the tab keeps every change, so a reload loses nothing (FR-019)
  useEffect(() => {
    saveDraft({ answers: form.getValues(), reached })
    return form.subscribe({
      formState: { values: true },
      callback: ({ values }) => saveDraft({ answers: values, reached }),
    })
  }, [form, reached])

  // a screen reader reads the new question after every step change (FR-015)
  useEffect(() => {
    headingRef.current?.focus()
  }, [location.key])

  if (params.step !== String(step)) {
    return <Navigate replace to={`/app/quiz/${step}`} />
  }

  const stepKey = STEP_KEYS[step - 1]
  const notesEmpty = !answers.notes?.trim()

  function options<T extends string>(
    key: OptionStep,
    codes: readonly T[],
    described: boolean,
  ) {
    return codes.map((value) => ({
      value,
      title: t(`quiz.${key}.options.${value}.title`),
      description: described
        ? t(`quiz.${key}.options.${value}.description`)
        : undefined,
    }))
  }

  function goNext() {
    // a step opened with Change returns to the review entry, so no second review lands in history
    if (state?.fromReview) {
      navigate(-1)
      return
    }
    setReached((current) => Math.max(current, Math.min(step + 1, STEP_COUNT)))
    const next = step < STEP_COUNT ? String(step + 1) : 'review'
    navigate(`/app/quiz/${next}`, { state: { fromStep: step } })
  }

  function goBack() {
    // back through history when the client came from the previous step or the review, so the device's back stays in step
    if (state?.fromReview || state?.fromStep === step - 1) navigate(-1)
    else navigate(`/app/quiz/${step - 1}`, { replace: true })
  }

  function numberField(
    name: 'age' | 'height' | 'weight',
    inputMode: 'numeric' | 'decimal',
  ) {
    // the weight is asked in the person's unit, so its unit, example and note follow it
    const texts =
      name === 'weight' ? `quiz.about.weight.${unit}` : `quiz.about.${name}`
    return (
      <Controller
        control={form.control}
        name={name}
        render={({ field, fieldState }) => (
          <NumberField
            id={`quiz-${name}`}
            label={t(`quiz.about.${name}.label`)}
            unit={t(`${texts}.unit`)}
            placeholder={t(`${texts}.placeholder`)}
            inputMode={inputMode}
            value={field.value}
            onChange={field.onChange}
            onBlur={field.onBlur}
            // onTouched also validates a field left empty; an empty field shows no message (spec, Edge Cases)
            message={
              field.value.trim() && fieldState.error?.message
                ? t(fieldState.error.message)
                : undefined
            }
            note={name === 'weight' ? t(`${texts}.note`) : undefined}
          />
        )}
      />
    )
  }

  function answer() {
    switch (stepKey) {
      case 'goal':
      case 'experience':
      case 'level': {
        const codes = { goal: GOALS, experience: EXPERIENCES, level: LEVELS }
        return (
          <Controller
            control={form.control}
            name={stepKey}
            render={({ field }) => (
              <SingleChoice
                labelledBy={HEADING_ID}
                options={options(
                  stepKey,
                  codes[stepKey],
                  stepKey !== 'experience',
                )}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
        )
      }
      case 'about':
        return (
          <div className="flex flex-col gap-5">
            {numberField('age', 'numeric')}
            {numberField('height', 'numeric')}
            {numberField('weight', 'decimal')}
          </div>
        )
      case 'likes':
        return (
          <Controller
            control={form.control}
            name="likes"
            render={({ field }) => (
              <MultipleChoice
                labelledBy={HEADING_ID}
                options={options('likes', LIKES, true)}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
        )
      case 'days':
        return (
          <div className="flex flex-col gap-4 pt-4">
            <Controller
              control={form.control}
              name="days"
              render={({ field }) => (
                <Stepper
                  labelledBy={HEADING_ID}
                  value={field.value}
                  min={DAYS_MIN}
                  max={DAYS_MAX}
                  unit={t('quiz.days.unit')}
                  decreaseLabel={t('quiz.days.fewer')}
                  increaseLabel={t('quiz.days.more')}
                  onChange={field.onChange}
                />
              )}
            />
            <p className="text-muted-foreground text-center text-xs">
              {t('quiz.days.range')}
            </p>
          </div>
        )
      case 'notes':
        return (
          <Controller
            control={form.control}
            name="notes"
            render={({ field }) => (
              <NotesField
                id="quiz-notes"
                label={t('quiz.notes.label')}
                placeholder={t('quiz.notes.placeholder')}
                value={field.value}
                maxLength={NOTES_MAX_LENGTH}
                onChange={field.onChange}
              />
            )}
          />
        )
    }
  }

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col px-4">
      <header className="flex flex-col gap-2 pt-2">
        <div className="flex min-h-12 items-center justify-between">
          <p className="num text-muted-foreground text-xs tracking-wider uppercase">
            {t('quiz.stepOf', { step, total: STEP_COUNT })}
          </p>
          {(step > 1 || state?.fromReview) && (
            <Button variant="ghost" className="h-12" onClick={goBack}>
              <ChevronLeftIcon data-icon="inline-start" aria-hidden />
              {t('quiz.back')}
            </Button>
          )}
        </div>
        <div aria-hidden className="flex gap-1.5">
          {Array.from({ length: STEP_COUNT }, (_, index) => (
            <span
              key={index}
              className={cn(
                'h-1 flex-1 rounded-full',
                index + 1 < step && 'bg-primary',
                index + 1 === step && 'bg-brand',
                index + 1 > step && 'bg-muted',
              )}
            />
          ))}
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-6 py-6">
        <div className="flex flex-col gap-2">
          <h1
            id={HEADING_ID}
            ref={headingRef}
            tabIndex={-1}
            className="font-heading text-3xl font-semibold outline-none"
          >
            {t(`quiz.${stepKey}.title`)}
          </h1>
          <p className="text-muted-foreground text-sm">
            {t(`quiz.${stepKey}.hint`)}
          </p>
        </div>
        {answer()}
      </main>
      <footer className="bg-background sticky bottom-0 flex flex-col gap-2 pt-3 pb-[max(1rem,env(safe-area-inset-bottom))]">
        <Button
          size="touch"
          className="w-full"
          disabled={!isStepValid(step, answers, unit)}
          onClick={goNext}
        >
          {step === STEP_COUNT && notesEmpty
            ? t('quiz.skipAndContinue')
            : t('quiz.continue')}
        </Button>
        <p className="text-muted-foreground text-center text-xs">
          {t('quiz.answersStay')}
        </p>
      </footer>
    </div>
  )
}
