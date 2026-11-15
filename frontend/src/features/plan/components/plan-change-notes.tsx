import { CalendarClockIcon, HistoryIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { TargetChange } from '@/components/target-change'
import { formatDay, weekdayName } from '@/lib/dates'
import { usePlanChangeValue } from '@/lib/plan-change-value'
import {
  useLatestAppliedPlanChange,
  useUpcomingPlanChange,
} from '../api/trainer-plan-change'

const DAY_MS = 24 * 60 * 60 * 1000
// as long as the Approvals list keeps an ended plan change
const NOTE_DAYS = 30

/**
 * Above the trainer's editor: the plan change both people accepted, with the old and new values until its Monday, and
 * for 30 days after it took effect a note that the plan changed. The plan below is the live one either way, so an
 * ordinary save stays an ordinary save.
 */
export function PlanChangeNotes({
  clientId,
  clientName,
}: {
  clientId: string
  clientName: string
}) {
  const { t, i18n } = useTranslation()
  const value = usePlanChangeValue()
  // read once: the note does not need to disappear while the page is open
  const [now] = useState(() => Date.now())
  const upcoming = useUpcomingPlanChange(clientId).data
  const applied = useLatestAppliedPlanChange(clientId).data
  const recent =
    applied?.tookEffectAt !== undefined &&
    now - Date.parse(applied.tookEffectAt) < NOTE_DAYS * DAY_MS

  return (
    <>
      {upcoming && (
        <Alert className="mt-4">
          <CalendarClockIcon aria-hidden />
          <AlertTitle>
            {t('plan.upcomingChange.title', {
              date: formatDay(upcoming.effectiveWeek, i18n.language),
            })}
          </AlertTitle>
          <AlertDescription>
            <p>{t('plan.upcomingChange.body', { name: clientName })}</p>
            <ul className="mt-2 flex flex-col gap-1">
              {upcoming.targets.map((target) => (
                <li
                  key={`${target.sessionDay}-${target.position}-${target.target}`}
                  className="text-foreground"
                >
                  <span className="font-medium">{target.exerciseName}</span>
                  <span className="text-muted-foreground">
                    {' · '}
                    {t('planChange.table.sessionOn', {
                      name: target.sessionName,
                      day: weekdayName(target.sessionDay, i18n.language),
                    })}
                    {': '}
                  </span>
                  <TargetChange
                    planned={value(target, target.oldValue)}
                    today={value(target, target.newValue)}
                    upcoming
                  />
                </li>
              ))}
              {upcoming.removedSession && (
                <li className="text-foreground">
                  <span className="font-medium">
                    {upcoming.removedSession.name}
                  </span>
                  <span className="text-muted-foreground">
                    {' · '}
                    {weekdayName(upcoming.removedSession.day, i18n.language)}
                    {': '}
                  </span>
                  {t('planChange.table.removed')}
                </li>
              )}
            </ul>
            <Link
              to={`/trainer/plan-changes/${upcoming.id}`}
              className="mt-2 inline-block underline underline-offset-2"
            >
              {t('plan.upcomingChange.open')}
            </Link>
          </AlertDescription>
        </Alert>
      )}
      {applied && recent && !upcoming && (
        <p className="text-muted-foreground mt-4 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
          <HistoryIcon aria-hidden className="size-4" />
          {t('plan.appliedChange.note', {
            date: formatDay(applied.effectiveWeek, i18n.language),
          })}
          <Link
            to={`/trainer/plan-changes/${applied.id}`}
            className="text-foreground underline underline-offset-2"
          >
            {t('plan.appliedChange.open')}
          </Link>
        </p>
      )}
    </>
  )
}
