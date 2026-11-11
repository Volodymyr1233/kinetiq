import { useId, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { SessionStateBadge } from '@/components/session-state-badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { BAND_STYLE, useCauseList } from '@/lib/readiness'
import { cn } from '@/lib/utils'
import type { ClientOverview } from '../api/client-training'

type Band = keyof typeof BAND_STYLE

const BANDS: Band[] = ['READY', 'TAKE_IT_EASIER', 'RECOVERY_DAY']

// a score of 0 still shows, so it never reads like a day without a check-in
const MIN_BAR = 3

function useFormats() {
  const { i18n } = useTranslation()
  // plain dates, read in UTC so no time zone can move the day
  return {
    date: new Intl.DateTimeFormat(i18n.language, {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    }),
    dayOfMonth: new Intl.DateTimeFormat(i18n.language, {
      day: 'numeric',
      timeZone: 'UTC',
    }),
  }
}

/**
 * The client's readiness over the last 14 days as their trainer reads it: a bar a day in the readiness scale, and each
 * recovery day with its reason, which targets the client had and the session of that day.
 */
export function ReadinessStrip({ overview }: { overview: ClientOverview }) {
  const { t } = useTranslation()
  const heading = useId()
  const checkedIn = overview.readiness.some((day) => day.score !== undefined)

  return (
    <Card role="region" aria-labelledby={heading}>
      <CardHeader className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
        <CardTitle>
          <h2 id={heading}>{t('sessionlog.overview.readiness.title')}</h2>
        </CardTitle>
        {checkedIn && <Legend />}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {checkedIn ? (
          <>
            <Strip overview={overview} />
            <RecoveryDays days={overview.recoveryDays} />
          </>
        ) : (
          <p className="text-muted-foreground text-sm">
            {t('sessionlog.overview.readiness.noCheckIns')}
          </p>
        )}
      </CardContent>
    </Card>
  )
}

function Legend() {
  const { t } = useTranslation()
  return (
    <ul
      aria-label={t('sessionlog.overview.readiness.legend')}
      className="text-muted-foreground flex flex-wrap gap-x-3 gap-y-1 text-xs"
    >
      {BANDS.map((band) => (
        <li key={band} className="flex items-center gap-1.5">
          <span
            aria-hidden
            className={cn('size-2 rounded-full', BAND_STYLE[band].dot)}
          />
          {t(`readiness.card.band.${band}.name`)}
          {/* the range tells the bands apart without colour, from the score on each bar */}
          <span className="num">
            {t(`sessionlog.overview.readiness.range.${band}`)}
          </span>
        </li>
      ))}
    </ul>
  )
}

function Strip({ overview }: { overview: ClientOverview }) {
  const { t } = useTranslation()
  const format = useFormats()

  return (
    <ol className="grid grid-cols-14 gap-1">
      {overview.readiness.map((day) => {
        const date = format.date.format(new Date(day.date))
        const today = day.date === overview.today
        // the bar's share of the track, which leaves room for the score above it
        const height =
          day.score === undefined
            ? undefined
            : ({
                '--bar': `calc((100% - 1rem) * ${Math.max(day.score, MIN_BAR) / 100})`,
              } as CSSProperties)
        return (
          <li key={day.date} className="flex min-w-0 flex-col items-center">
            <div
              aria-hidden
              className="border-border relative h-28 w-full border-b"
              style={height}
            >
              {day.score !== undefined && day.band !== undefined && (
                <>
                  <span className="num absolute inset-x-0 bottom-[calc(var(--bar)+2px)] text-center text-xs">
                    {day.score}
                  </span>
                  <span
                    className={cn(
                      'absolute inset-x-0 bottom-0 h-(--bar) rounded-t-sm',
                      BAND_STYLE[day.band].dot,
                    )}
                  />
                </>
              )}
            </div>
            <span
              aria-hidden
              className={cn(
                'num mt-1 text-xs',
                today ? 'font-semibold' : 'text-muted-foreground',
              )}
            >
              {format.dayOfMonth.format(new Date(day.date))}
            </span>
            {today && (
              <span aria-hidden className="text-xs font-semibold">
                {t('sessionlog.overview.readiness.today')}
              </span>
            )}
            <span className="sr-only">
              {day.score === undefined || day.band === undefined
                ? t('sessionlog.overview.readiness.noCheckIn', { date })
                : t('sessionlog.overview.readiness.day', {
                    date,
                    score: day.score,
                    band: t(`readiness.card.band.${day.band}.name`),
                  })}
              {today && `, ${t('sessionlog.overview.readiness.today')}`}
            </span>
          </li>
        )
      })}
    </ol>
  )
}

function RecoveryDays({ days }: { days: ClientOverview['recoveryDays'] }) {
  const { t } = useTranslation()
  const format = useFormats()
  const causeList = useCauseList()
  const heading = useId()

  if (days.length === 0) {
    return (
      <p className="text-muted-foreground text-sm">
        {t('sessionlog.overview.readiness.noRecoveryDays')}
      </p>
    )
  }

  return (
    <section aria-labelledby={heading} className="flex flex-col gap-1">
      <h3 id={heading} className="text-sm font-semibold">
        {t('sessionlog.overview.readiness.recoveryDays')}
      </h3>
      <ul className="flex flex-col divide-y border-t">
        {days.map((day) => (
          <li
            key={day.date}
            className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1 py-2.5 text-sm"
          >
            <div className="flex min-w-0 flex-col gap-0.5">
              <span className="flex flex-wrap items-baseline gap-x-2">
                <span className="font-semibold">
                  {format.date.format(new Date(day.date))}
                </span>
                <span className="num text-muted-foreground text-xs">
                  {t('sessionlog.overview.readiness.score', {
                    score: day.score,
                  })}
                </span>
              </span>
              <span className="text-muted-foreground">
                {causeList(day.causes, { opening: true })}
              </span>
            </div>
            <div className="ml-auto flex flex-col items-end gap-1">
              <span className="text-xs font-medium">
                {t(
                  `sessionlog.overview.readiness.targets.${day.targets ?? 'none'}`,
                )}
              </span>
              {day.session && (
                <span className="flex items-center gap-2">
                  {day.session.sessionLogId ? (
                    <Link
                      to={`logs?session=${day.session.sessionLogId}`}
                      className="focus-visible:ring-ring/50 rounded-sm font-medium underline underline-offset-4 focus-visible:ring-3 focus-visible:outline-none"
                    >
                      {day.session.name}
                    </Link>
                  ) : (
                    <span>{day.session.name}</span>
                  )}
                  <SessionStateBadge
                    state={day.session.state}
                    locked={day.session.locked}
                  />
                </span>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
