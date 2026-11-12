import {
  ArrowLeftRightIcon,
  CalendarClockIcon,
  ClockIcon,
  FileTextIcon,
  MoonIcon,
  TriangleAlertIcon,
  type LucideIcon,
} from 'lucide-react'
import type { ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { formatDay } from '@/lib/dates'
import type { OverviewAlert, OverviewClient } from '../api/overview'

type Kind = OverviewAlert['kind']

const LOOK: Record<
  Kind,
  { variant: ComponentProps<typeof Alert>['variant']; Icon: LucideIcon }
> = {
  AT_RISK: { variant: 'destructive', Icon: TriangleAlertIcon },
  WATCH: { variant: 'warning', Icon: TriangleAlertIcon },
  NO_LOG: { variant: 'warning', Icon: ClockIcon },
  PLAN_CHANGE: { variant: 'info', Icon: ArrowLeftRightIcon },
  DRAFT: { variant: 'info', Icon: FileTextIcon },
  PLAN_ENDING: { variant: 'info', Icon: CalendarClockIcon },
  RECOVERY_DAY: { variant: 'low', Icon: MoonIcon },
}

/** What a status alert says first: the reason with its number, low completion before high RPE. */
function useReasonLines(client: OverviewClient) {
  const { t, i18n } = useTranslation()
  // the whole name, since two clients can share a first name
  const name = client.name
  return client.reasons.map((reason) =>
    reason === 'LOW_COMPLETION'
      ? t('overview.alerts.lowCompletion', {
          name,
          done: client.sessionsDone,
          count: client.sessionsDue,
        })
      : t('overview.alerts.highRpe', {
          name,
          rpe: new Intl.NumberFormat(i18n.language, {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1,
          }).format(client.averageRpe ?? 0),
        }),
  )
}

function AttentionItem({
  alert,
  client,
}: {
  alert: OverviewAlert
  client: OverviewClient
}) {
  const { t, i18n } = useTranslation()
  const name = client.name
  const reasons = useReasonLines(client)
  const { variant, Icon } = LOOK[alert.kind]
  const waiting =
    alert.planChangeTitle !== undefined && alert.waitingFor !== undefined
      ? t(`overview.alerts.waiting.${alert.waitingFor}`, {
          title: alert.planChangeTitle,
          name,
        })
      : undefined
  const clientPage = `/trainer/clients/${client.id}`
  const planChangePage = `/trainer/plan-changes/${alert.planChangeId}`

  let title: string
  let lines: string[] = []
  let action: { label: string; to: string }
  switch (alert.kind) {
    case 'AT_RISK':
    case 'WATCH':
      title = reasons[0] ?? ''
      // a reason after the first reads as a sentence of its own
      lines = [
        ...reasons
          .slice(1)
          .map((text) => t('overview.alerts.sentence', { text })),
        ...(waiting ? [waiting] : []),
      ]
      action = alert.planChangeId
        ? { label: t('overview.actions.planChange'), to: planChangePage }
        : { label: t('overview.actions.open'), to: clientPage }
      break
    case 'PLAN_CHANGE':
      title = t('overview.alerts.planChange', { name })
      lines = waiting ? [waiting] : []
      action = { label: t('overview.actions.planChange'), to: planChangePage }
      break
    case 'DRAFT':
      title = t(`overview.alerts.draft.${alert.draft ?? 'SAVED'}`, { name })
      lines = [t('overview.alerts.draftSeen', { name })]
      action = {
        label: t('overview.actions.reviewPlan'),
        to: `${clientPage}/plan`,
      }
      break
    case 'NO_LOG':
      title = t('overview.alerts.noLog', {
        name,
        count: alert.daysWithoutLog,
      })
      lines = [
        alert.lastSessionName === undefined
          ? t('overview.alerts.noLogSincePublished', {
              date: formatDay(alert.since!, i18n.language),
            })
          : t('overview.alerts.lastSession', {
              session: alert.lastSessionName,
              date: formatDay(alert.since!, i18n.language),
            }),
      ]
      action = {
        label: t('overview.actions.message'),
        to: `/trainer/messages/${client.id}`,
      }
      break
    case 'PLAN_ENDING':
      title = t(
        client.planWeek === undefined
          ? 'overview.alerts.planEnded'
          : 'overview.alerts.planEnds',
        { name, date: formatDay(client.planEndsOn!, i18n.language) },
      )
      lines = [t('overview.alerts.planNext', { name })]
      action = {
        label: t('overview.actions.openPlan'),
        to: `${clientPage}/plan`,
      }
      break
    case 'RECOVERY_DAY':
      title = t('overview.alerts.recoveryDay', { name })
      lines = [
        t(
          client.lighterTargets
            ? 'overview.alerts.lighter'
            : 'overview.alerts.original',
          { name, score: client.readiness },
        ),
      ]
      action = { label: t('overview.actions.open'), to: clientPage }
      break
  }

  return (
    // one finding of a list read on arrival, so not a live alert
    <Alert role={undefined} variant={variant}>
      <Icon aria-hidden />
      <AlertTitle>{title}</AlertTitle>
      {lines.length > 0 && (
        <AlertDescription>
          {lines.map((line) => (
            <p key={line}>{line}</p>
          ))}
        </AlertDescription>
      )}
      <AlertAction>
        <Button variant="outline" size="sm" asChild>
          <Link
            to={action.to}
            aria-label={t('overview.actions.labelled', {
              action: action.label,
              name: client.name,
            })}
          >
            {action.label}
          </Link>
        </Button>
      </AlertAction>
    </Alert>
  )
}

/** "Needs attention": every alert, most severe first, each with one action; one line when there is none. */
export function AttentionList({
  alerts,
  clients,
}: {
  alerts: OverviewAlert[]
  clients: OverviewClient[]
}) {
  const { t } = useTranslation()
  const byId = new Map(clients.map((client) => [client.id, client]))
  const shown = alerts.filter((alert) => byId.has(alert.clientId))

  return (
    <section aria-labelledby="needs-attention" className="flex flex-col gap-3">
      <h2 id="needs-attention" className="font-heading text-lg font-semibold">
        {t('overview.alerts.title')}
      </h2>
      {shown.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          {t('overview.alerts.none')}
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {shown.map((alert) => (
            <li key={`${alert.kind}-${alert.clientId}`}>
              <AttentionItem alert={alert} client={byId.get(alert.clientId)!} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
