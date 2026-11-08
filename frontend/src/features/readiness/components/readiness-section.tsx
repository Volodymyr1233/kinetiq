import { CircleAlertIcon } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import { useCheckIn, useSaveCheckIn, type CheckIn } from '../api/check-in'
import { isSkipped, skip, unskip } from '../skip'
import { CheckInCard } from './check-in-card'

// where focus goes after a switch the client caused; on first load it is left alone
type Focus = 'checkIn' | 'skipped' | 'card'

/** Today's check-in above the session: the check-in, the line saying it was skipped, the readiness, or nothing. */
export function ReadinessSection() {
  const { t } = useTranslation()
  const query = useCheckIn()
  const save = useSaveCheckIn()
  const [skippedHere, setSkippedHere] = useState<boolean>()
  const [focus, setFocus] = useState<Focus>()
  const today = query.data

  if (today === undefined) {
    if (query.isPending) {
      return <Skeleton aria-hidden className="h-80 w-full rounded-xl" />
    }
    return (
      <Alert variant="destructive">
        <CircleAlertIcon aria-hidden />
        <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        <AlertAction>
          <Button variant="outline" onClick={() => void query.refetch()}>
            {t('readiness.checkIn.retry')}
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  if (today.checkIn) {
    return (
      <ReadinessSummary
        checkIn={today.checkIn}
        focusHeading={focus === 'card'}
      />
    )
  }
  if (!today.open) return null

  if (skippedHere ?? isSkipped(today.date)) {
    return (
      <SkippedLine
        focusAction={focus === 'skipped'}
        onCheckIn={() => {
          unskip()
          setSkippedHere(false)
          setFocus('checkIn')
        }}
      />
    )
  }

  return (
    <CheckInCard
      saving={save.isPending}
      error={save.error}
      focusTitle={focus === 'checkIn'}
      onComplete={(answers) => {
        // set before saving, so the card that replaces the check-in takes focus when it appears
        setFocus('card')
        save.mutate(answers)
      }}
      onSkip={() => {
        skip(today.date)
        save.reset()
        setSkippedHere(true)
        setFocus('skipped')
      }}
    />
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

// replaced by the readiness card of task #113
function ReadinessSummary({
  checkIn,
  focusHeading,
}: {
  checkIn: CheckIn
  focusHeading: boolean
}) {
  const { t } = useTranslation()
  const heading = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (focusHeading) heading.current?.focus()
  }, [focusHeading])

  return (
    <Card aria-live="polite">
      <CardContent>
        <h2
          ref={heading}
          tabIndex={-1}
          className="font-heading text-lg font-semibold outline-none"
        >
          {t('readiness.card.summary', {
            score: checkIn.score,
            band: t(`readiness.card.band.${checkIn.band}.name`),
          })}
        </h2>
        <p className="text-muted-foreground">
          {t('readiness.card.answers', checkIn)}
        </p>
      </CardContent>
    </Card>
  )
}
