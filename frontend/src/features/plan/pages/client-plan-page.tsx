import {
  ChevronLeftIcon,
  ChevronRightIcon,
  CircleAlertIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useEffect, useRef } from 'react'
import { Navigate, useSearchParams } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useAcknowledgePlan, useOwnPlan } from '../api/client-plan'
import { useSessionStates } from '../api/session-states'
import { WeekStrip } from '../components/week-strip'

// a plan week is a whole number from 1; anything else in the address means the current week
const parseWeek = (text: string | null) =>
  text !== null && /^[1-9]\d{0,3}$/.test(text) ? Number(text) : undefined

/** The client's Plan tab: their published plan, week by week. */
export function ClientPlanPage() {
  const { t, i18n } = useTranslation()
  const [params, setParams] = useSearchParams()
  const askedWeek = parseWeek(params.get('week'))
  const query = useOwnPlan(askedWeek)
  const statesQuery = useSessionStates(askedWeek)
  const states = statesQuery.data?.sessions
  const plan = query.data
  const acknowledge = useAcknowledgePlan()
  const acknowledged = useRef(false)

  // opening the tab is seeing the plan, whether the client came from the plan-ready screen or by its address
  useEffect(() => {
    if (plan?.seen === false && !acknowledged.current) {
      acknowledged.current = true
      acknowledge.mutate()
    }
  }, [plan?.seen, acknowledge])

  // the week is kept in the address, so a refresh and the back button stay on it; the current week has no parameter
  const goTo = (number: number) =>
    setParams(number === plan?.currentWeek ? {} : { week: String(number) })

  if (plan === undefined) {
    if (query.isPending) {
      return (
        <div aria-hidden className="flex flex-col gap-2 px-4">
          {Array.from({ length: 7 }, (_, row) => (
            <Skeleton key={row} className="h-12 w-full" />
          ))}
        </div>
      )
    }
    const code = problemCode(query.error)
    // an old address names a week the plan does not have
    if (code === 'plan.week-out-of-range') {
      return <Navigate replace to="/app/plan" />
    }
    if (code === 'plan.not-found') {
      return (
        <p className="text-muted-foreground px-4 text-sm">
          {t('plan.client.none')}
        </p>
      )
    }
    return (
      <div className="px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('plan.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  const week = plan.week
  const range =
    week === undefined
      ? ''
      : new Intl.DateTimeFormat(i18n.language, {
          day: 'numeric',
          month: 'short',
          timeZone: 'UTC',
        }).formatRange(dateOf(week.startsOn, 0), dateOf(week.startsOn, 6))

  return (
    <div className="flex flex-col gap-4 px-4">
      {plan.finished && (
        <div className="flex flex-col gap-1">
          <h2 className="font-heading text-xl font-semibold">
            {t('plan.client.finished.title')}
          </h2>
          <p className="text-muted-foreground">
            {t('plan.client.finished.text', { weeks: plan.weeks })}
          </p>
        </div>
      )}
      {week && (
        <>
          <div className="flex items-center justify-between gap-2">
            <Button
              variant="outline"
              size="icon-lg"
              aria-label={t('plan.client.previous')}
              disabled={week.number <= 1}
              onClick={() => goTo(week.number - 1)}
            >
              <ChevronLeftIcon aria-hidden />
            </Button>
            <div className="flex min-w-0 flex-col items-center text-center">
              <p role="status" className="font-medium">
                {t('plan.week', { week: week.number, weeks: plan.weeks })}
              </p>
              <p className="text-muted-foreground num text-sm">{range}</p>
            </div>
            <Button
              variant="outline"
              size="icon-lg"
              aria-label={t('plan.client.next')}
              disabled={week.number >= plan.weeks}
              onClick={() => goTo(week.number + 1)}
            >
              <ChevronRightIcon aria-hidden />
            </Button>
          </div>
          {week.number !== plan.currentWeek && (
            <Button
              variant="secondary"
              size="touch"
              onClick={() => goTo(plan.currentWeek)}
            >
              {t('plan.client.currentWeek')}
            </Button>
          )}
          {states !== undefined && (
            <p className="text-muted-foreground text-sm">
              {t('plan.client.weekDone', {
                done: states.filter((s) => s.state === 'DONE').length,
                count: plan.sessions.length,
              })}
            </p>
          )}
          <WeekStrip
            week={week}
            sessions={plan.sessions}
            states={states}
            today={statesQuery.data?.today}
          />
        </>
      )}
    </div>
  )
}

// a plain date plus days, in UTC so no time zone can move it
function dateOf(isoDate: string, plusDays: number) {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + plusDays))
}
