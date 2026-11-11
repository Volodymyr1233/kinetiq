import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { BAND_STYLE } from '@/lib/readiness'
import { cn } from '@/lib/utils'
import type { CheckIn } from '../api/check-in'

type ReadinessCardProps = {
  checkIn: CheckIn
  /** Whether the answers can still be changed: no set of today's session is logged. */
  open: boolean
  onChangeAnswers: () => void
  /** Moves focus to the band, when the client's action brought the card. */
  focusHeading: boolean
}

/** Today's readiness: the score, its band and what it means for today, with the answers it came from. */
export function ReadinessCard({
  checkIn,
  open,
  onChangeAnswers,
  focusHeading,
}: ReadinessCardProps) {
  const { t } = useTranslation()
  const id = useId()
  const heading = useRef<HTMLHeadingElement>(null)
  const style = BAND_STYLE[checkIn.band]

  useEffect(() => {
    if (focusHeading) heading.current?.focus()
  }, [focusHeading])

  return (
    <Card role="region" aria-labelledby={`${id}-label ${id}-band`}>
      <CardHeader className="flex items-center justify-between gap-2">
        <p
          id={`${id}-label`}
          className="num text-muted-foreground text-xs tracking-wider uppercase"
        >
          {t('readiness.card.label')}
        </p>
        {open && (
          <Button
            variant="link"
            size="touch"
            className="-my-3 px-0"
            onClick={onChangeAnswers}
          >
            {t('readiness.card.change')}
          </Button>
        )}
      </CardHeader>
      <CardContent className="gap-2">
        <div
          className={cn(
            'flex items-center gap-3 rounded-lg p-3',
            style.surface,
          )}
        >
          <span
            aria-hidden
            className={cn('size-3 shrink-0 rounded-full', style.dot)}
          />
          <span className="num text-metric-lg font-semibold">
            {t('readiness.card.score', { score: checkIn.score })}
          </span>
          <div className="flex min-w-0 flex-col">
            <h2
              id={`${id}-band`}
              ref={heading}
              tabIndex={-1}
              className="font-heading text-lg font-semibold outline-none"
            >
              {t(`readiness.card.band.${checkIn.band}.name`)}
            </h2>
            <p>
              {checkIn.adjustment?.inUse
                ? t('readiness.card.lighter')
                : t(`readiness.card.band.${checkIn.band}.line`)}
            </p>
          </div>
        </div>
        <p className="text-muted-foreground">
          {t('readiness.card.answers', checkIn)}
        </p>
        {!open && (
          <p className="text-muted-foreground">
            {checkIn.adjustment
              ? t('readiness.card.fixedWithTargets')
              : t('readiness.card.fixed')}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
