import { CircleAlertIcon } from 'lucide-react'
import { useEffect, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useOwnPlan } from '../api/client-plan'

/**
 * The one-time "your plan is ready" screen, shown before a client's first visit to a published plan. `settings` is
 * shown in a header above the heading, given by the app, as on the quiz pages.
 */
export function PlanReadyPage({ settings }: { settings?: ReactNode }) {
  const { t } = useTranslation()
  const query = useOwnPlan()
  const plan = query.data
  const loaded = plan !== undefined
  const headingRef = useRef<HTMLHeadingElement>(null)

  // a screen reader reads the new screen's heading, as on the waiting page
  useEffect(() => {
    headingRef.current?.focus()
  }, [loaded])

  if (plan === undefined) {
    if (query.isPending) {
      return (
        <p role="status" className="sr-only">
          {t('plan.loading')}
        </p>
      )
    }
    // without a published plan the client belongs to the quiz or the waiting screen, which the app decides
    if (problemCode(query.error) === 'plan.not-found') {
      return <Navigate replace to="/app" />
    }
    return (
      <main className="bg-background mx-auto flex min-h-svh w-full max-w-[400px] flex-col justify-center gap-4 px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('plan.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </main>
    )
  }

  // the screen shows once per published plan
  if (plan.seen) {
    return <Navigate replace to="/app" />
  }

  return (
    <div className="mx-auto flex min-h-svh max-w-[640px] flex-col px-4">
      <header className="flex min-h-12 justify-end pt-2">{settings}</header>
      <main className="flex flex-col gap-4 py-6">
        <div className="flex flex-col gap-2">
          <h1
            ref={headingRef}
            tabIndex={-1}
            className="font-heading text-3xl font-semibold outline-none"
          >
            {t('plan.client.ready.title')}
          </h1>
          <p className="text-muted-foreground">
            {t('plan.client.ready.text', {
              goal: t(`quiz.goal.options.${plan.goal}.title`),
              weeks: t('plan.client.ready.weeks', { count: plan.weeks }),
              sessions: t('plan.client.ready.sessionsPerWeek', {
                count: plan.sessions.length,
              }),
            })}
          </p>
        </div>
        <Button size="touch" asChild>
          <Link to="/app/plan">{t('plan.client.ready.view')}</Link>
        </Button>
      </main>
    </div>
  )
}
