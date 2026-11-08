import { CircleAlertIcon, InfoIcon } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useCheckIn, useSaveCheckIn, type Answers } from '../api/check-in'
import { isSkipped, skip, unskip } from '../skip'
import { CheckInCard } from './check-in-card'
import { ReadinessCard } from './readiness-card'

// where focus goes after a switch the client caused; on first load it is left alone
type Focus = 'checkIn' | 'skipped' | 'card'

/** Today's check-in above the session: the check-in, the line saying it was skipped, the readiness, or nothing. */
export function ReadinessSection() {
  const { t } = useTranslation()
  const query = useCheckIn()
  const save = useSaveCheckIn()
  const [skippedHere, setSkippedHere] = useState<boolean>()
  const [changing, setChanging] = useState(false)
  const [focus, setFocus] = useState<Focus>()
  const today = query.data

  if (today === undefined) {
    if (query.isPending) {
      return <Skeleton aria-hidden className="h-80 w-full rounded-xl" />
    }
    return (
      <div className="flex flex-col gap-2">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        </Alert>
        <Button
          variant="outline"
          size="touch"
          onClick={() => void query.refetch()}
        >
          {t('readiness.checkIn.retry')}
        </Button>
      </div>
    )
  }

  const saveAnswers = (answers: Answers, after?: () => void) => {
    // set before saving, so the card that replaces the check-in takes focus when it appears
    setFocus('card')
    save.mutate(answers, { onSuccess: after })
  }

  // a set was logged meanwhile: the refetched card says the answers are fixed, and this says why the save failed
  const trainingStarted =
    problemCode(save.error) === 'check-in.training-started'

  const checkIn = today.checkIn
  let content: ReactNode = null
  if (checkIn && changing && today.open) {
    content = (
      <CheckInCard
        change={{
          initial: {
            sleep: checkIn.sleep,
            soreness: checkIn.soreness,
            stress: checkIn.stress,
          },
          onCancel: () => {
            save.reset()
            setChanging(false)
            setFocus('card')
          },
        }}
        saving={save.isPending}
        error={trainingStarted ? null : save.error}
        focusTitle={focus === 'checkIn'}
        onComplete={(answers) => {
          if (sameAnswers(answers, checkIn)) {
            setChanging(false)
            setFocus('card')
          } else {
            saveAnswers(answers, () => setChanging(false))
          }
        }}
      />
    )
  } else if (checkIn) {
    content = (
      <ReadinessCard
        checkIn={checkIn}
        open={today.open}
        focusHeading={focus === 'card'}
        onChangeAnswers={() => {
          save.reset()
          setChanging(true)
          setFocus('checkIn')
        }}
      />
    )
  } else if (today.open && (skippedHere ?? isSkipped(today.date))) {
    content = (
      <SkippedLine
        focusAction={focus === 'skipped'}
        onCheckIn={() => {
          unskip()
          setSkippedHere(false)
          setFocus('checkIn')
        }}
      />
    )
  } else if (today.open) {
    content = (
      <CheckInCard
        saving={save.isPending}
        error={trainingStarted ? null : save.error}
        focusTitle={focus === 'checkIn'}
        onComplete={(answers) => saveAnswers(answers)}
        onSkip={() => {
          skip(today.date)
          save.reset()
          setSkippedHere(true)
          setFocus('skipped')
        }}
      />
    )
  }

  if (content === null && !trainingStarted) return null

  return (
    <>
      {trainingStarted && (
        <TrainingStartedNotice
          message={t(errorMessageKey(save.error))}
          // the check-in the client answered is gone and no card replaces it, so the notice takes focus
          focus={content === null}
        />
      )}
      {content}
      {/* always present, so a score that appears is read out */}
      <p role="status" className="sr-only">
        {checkIn &&
          focus === 'card' &&
          t('readiness.card.announcement', {
            score: checkIn.score,
            band: t(`readiness.card.band.${checkIn.band}.name`),
          })}
      </p>
    </>
  )
}

function TrainingStartedNotice({
  message,
  focus,
}: {
  message: string
  focus: boolean
}) {
  const notice = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (focus) notice.current?.focus()
  }, [focus])

  return (
    <Alert ref={notice} tabIndex={-1} className="outline-none">
      <InfoIcon aria-hidden />
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  )
}

function sameAnswers(a: Answers, b: Answers) {
  return (
    a.sleep === b.sleep && a.soreness === b.soreness && a.stress === b.stress
  )
}

function SkippedLine({
  onCheckIn,
  focusAction,
}: {
  onCheckIn: () => void
  focusAction: boolean
}) {
  const { t } = useTranslation()
  const action = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (focusAction) action.current?.focus()
  }, [focusAction])

  return (
    <Card size="sm">
      <CardContent className="flex-row flex-wrap items-center justify-between gap-x-2 gap-y-0">
        <p className="text-muted-foreground">
          {t('readiness.checkIn.skipped')}
        </p>
        <Button
          ref={action}
          variant="link"
          size="touch"
          // no padding, so the link lines up with the text when it wraps; it stays 48 px high
          className="px-0"
          onClick={onCheckIn}
        >
          {t('readiness.checkIn.checkIn')}
        </Button>
      </CardContent>
    </Card>
  )
}
