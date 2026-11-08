import { CircleAlertIcon } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { errorMessageKey } from '@/lib/api/problem'
import type { Answers } from '../api/check-in'
import { AnswerGroup, type Question } from './answer-group'

const QUESTIONS: Question[] = ['sleep', 'soreness', 'stress']

type CheckInCardProps = {
  /** Sends the answers: on the third answer of a first check-in, or on Done when changing. */
  onComplete: (answers: Answers) => void
  saving: boolean
  error: unknown
  /** Moves focus to the title, when the client brought the card back. */
  focusTitle: boolean
} & (
  | { change?: undefined; onSkip: () => void }
  | {
      /** The saved answers being changed: they start chosen, and nothing saves before Done. */
      change: { initial: Answers; onCancel: () => void }
      onSkip?: undefined
    }
)

/**
 * Three questions on a 1 to 5 scale. A first check-in saves on its third answer, with no submit button; a change saves
 * on Done and keeps the saved answers on Cancel.
 */
export function CheckInCard({
  onComplete,
  onSkip,
  change,
  saving,
  error,
  focusTitle,
}: CheckInCardProps) {
  const { t } = useTranslation()
  const id = useId()
  const title = useRef<HTMLHeadingElement>(null)
  const [answers, setAnswers] = useState<Partial<Answers>>(
    change?.initial ?? {},
  )

  useEffect(() => {
    if (focusTitle) title.current?.focus()
  }, [focusTitle])

  // saved from the choice itself, never from an effect, so one completed check-in sends one request
  const choose = (question: Question, value: number) => {
    const next = { ...answers, [question]: value }
    setAnswers(next)
    if (!change && complete(next)) onComplete(next)
  }

  return (
    <Card role="region" aria-labelledby={`${id}-title`}>
      <CardHeader>
        <h2
          id={`${id}-title`}
          ref={title}
          tabIndex={-1}
          className="font-heading text-lg font-semibold outline-none"
        >
          {t('readiness.checkIn.title')}
        </h2>
      </CardHeader>
      <CardContent className="gap-5">
        {QUESTIONS.map((question) => (
          <AnswerGroup
            key={question}
            question={question}
            value={answers[question]}
            onChoose={(value) => choose(question, value)}
            busy={saving}
          />
        ))}
        {error !== null && !saving && complete(answers) && (
          <div className="flex flex-col gap-2">
            <Alert variant="destructive">
              <CircleAlertIcon aria-hidden />
              <AlertDescription>{t(errorMessageKey(error))}</AlertDescription>
            </Alert>
            <Button
              variant="outline"
              size="touch"
              onClick={() => onComplete(answers)}
            >
              {t('readiness.checkIn.retry')}
            </Button>
          </div>
        )}
      </CardContent>
      {change ? (
        <CardFooter className="flex-wrap justify-end gap-2">
          <p role="status" className="text-muted-foreground mr-auto text-sm">
            {saving && t('readiness.checkIn.saving')}
          </p>
          <Button
            variant="ghost"
            size="touch"
            aria-disabled={saving || undefined}
            onClick={() => {
              if (!saving) change.onCancel()
            }}
          >
            {t('readiness.checkIn.cancel')}
          </Button>
          <Button
            size="touch"
            aria-disabled={saving || undefined}
            onClick={() => {
              if (!saving && complete(answers)) onComplete(answers)
            }}
          >
            {t('readiness.checkIn.done')}
          </Button>
        </CardFooter>
      ) : (
        <CardFooter className="justify-between gap-2">
          <Button
            variant="ghost"
            size="touch"
            className="-ml-4 px-4"
            aria-disabled={saving || undefined}
            onClick={() => {
              if (!saving) onSkip()
            }}
          >
            {t('readiness.checkIn.skip')}
          </Button>
          <p role="status" className="text-muted-foreground text-right text-sm">
            {saving
              ? t('readiness.checkIn.saving')
              : t('readiness.checkIn.savesAfterThird')}
          </p>
        </CardFooter>
      )}
    </Card>
  )
}

function complete(answers: Partial<Answers>): answers is Answers {
  return QUESTIONS.every((question) => answers[question] !== undefined)
}
