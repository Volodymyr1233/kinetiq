import { CircleAlertIcon } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { errorMessageKey } from '@/lib/api/problem'
import type { Answers } from '../api/check-in'
import { AnswerGroup, type Question } from './answer-group'

const QUESTIONS: Question[] = ['sleep', 'soreness', 'stress']

type CheckInCardProps = {
  /** Sends the answers; called once the third question has an answer. */
  onComplete: (answers: Answers) => void
  onSkip: () => void
  saving: boolean
  error: unknown
  /** Moves focus to the title, when the client brought the card back. */
  focusTitle: boolean
}

/** Three questions on a 1 to 5 scale; the third answer saves the check-in, with no submit button. */
export function CheckInCard({
  onComplete,
  onSkip,
  saving,
  error,
  focusTitle,
}: CheckInCardProps) {
  const { t } = useTranslation()
  const id = useId()
  const title = useRef<HTMLHeadingElement>(null)
  const [answers, setAnswers] = useState<Partial<Answers>>({})

  useEffect(() => {
    if (focusTitle) title.current?.focus()
  }, [focusTitle])

  // saved from the choice itself, never from an effect, so one completed check-in sends one request
  const choose = (question: Question, value: number) => {
    const next = { ...answers, [question]: value }
    setAnswers(next)
    if (complete(next)) onComplete(next)
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
          <Alert variant="destructive">
            <CircleAlertIcon aria-hidden />
            <AlertDescription>{t(errorMessageKey(error))}</AlertDescription>
            <AlertAction>
              <Button variant="outline" onClick={() => onComplete(answers)}>
                {t('readiness.checkIn.retry')}
              </Button>
            </AlertAction>
          </Alert>
        )}
      </CardContent>
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
    </Card>
  )
}

function complete(answers: Partial<Answers>): answers is Answers {
  return QUESTIONS.every((question) => answers[question] !== undefined)
}
