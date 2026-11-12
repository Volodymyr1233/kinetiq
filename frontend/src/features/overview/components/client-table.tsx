import { ArrowDownIcon, ArrowUpDownIcon, ArrowUpIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { AccountAvatar } from '@/components/account-avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { timeAgo } from '@/lib/dates'
import { BAND_STYLE } from '@/lib/readiness'
import { cn } from '@/lib/utils'
import type { OverviewClient } from '../api/overview'
import type { Sort, SortKey } from '../client-sort'
import { StatusBadge } from './status-badge'

// the RPE scale around the usual 7–8 target, decided on the value shown; never red
function rpeDot(rpe: number) {
  if (rpe < 7) return 'bg-rpe-easy'
  return rpe <= 8 ? 'bg-rpe-target' : 'bg-rpe-hard'
}

// a value that is not there yet reads as "None yet", not as a bare dash
function NoValue() {
  const { t } = useTranslation()
  return (
    <>
      <span aria-hidden>—</span>
      <span className="sr-only">{t('clients.noValue')}</span>
    </>
  )
}

function Dot({ className }: { className: string }) {
  return (
    <span
      aria-hidden
      className={cn('size-2 shrink-0 rounded-full', className)}
    />
  )
}

function SortableHead({
  label,
  column,
  sort,
  onSort,
  className,
}: {
  label: string
  column: SortKey
  sort?: Sort
  onSort?: (key: SortKey) => void
  className?: string
}) {
  if (onSort === undefined) {
    return <TableHead className={className}>{label}</TableHead>
  }
  const active = sort?.key === column
  const Icon = !active
    ? ArrowUpDownIcon
    : sort.direction === 'ascending'
      ? ArrowUpIcon
      : ArrowDownIcon
  return (
    <TableHead
      aria-sort={active ? sort.direction : 'none'}
      className={className}
    >
      <Button
        variant="ghost"
        size="sm"
        className={cn('-ml-2.5', !active && 'text-muted-foreground')}
        onClick={() => onSort(column)}
      >
        {label}
        <Icon data-icon="inline-end" aria-hidden />
      </Button>
    </TableHead>
  )
}

/**
 * The trainer's clients with their status and figures. `full` adds the goal and plan columns of All clients;
 * `onSort` makes the headers sort the rows.
 */
export function ClientTable({
  clients,
  full = false,
  sort,
  onSort,
}: {
  clients: OverviewClient[]
  full?: boolean
  sort?: Sort
  onSort?: (key: SortKey) => void
}) {
  const { t, i18n } = useTranslation()
  const now = new Date()
  const rpe = new Intl.NumberFormat(i18n.language, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })
  const sorting = { sort, onSort }

  return (
    <Table data-density="compact">
      <TableHeader>
        <TableRow>
          <SortableHead
            label={t('overview.columns.client')}
            column="name"
            className="bg-background sticky left-0"
            {...sorting}
          />
          {full && <TableHead>{t('clients.columns.goal')}</TableHead>}
          {full && <TableHead>{t('clients.columns.plan')}</TableHead>}
          <SortableHead
            label={t('clients.columns.lastLog')}
            column="lastLog"
            {...sorting}
          />
          <SortableHead
            label={t('overview.columns.sessions')}
            column="sessions"
            {...sorting}
          />
          <SortableHead
            label={t('overview.columns.rpe')}
            column="rpe"
            {...sorting}
          />
          <TableHead>{t('overview.columns.readiness')}</TableHead>
          <SortableHead
            label={t('overview.columns.status')}
            column="status"
            {...sorting}
          />
        </TableRow>
      </TableHeader>
      <TableBody>
        {clients.map((client) => (
          <TableRow key={client.id}>
            <TableCell className="bg-background sticky left-0 font-medium">
              <Link
                to={`/trainer/clients/${client.id}`}
                className="focus-visible:ring-ring/50 flex items-center gap-2.5 rounded-sm underline-offset-4 hover:underline focus-visible:ring-3 focus-visible:outline-none"
              >
                <AccountAvatar name={client.name} />
                {client.name}
              </Link>
            </TableCell>
            {full && (
              <TableCell className="text-muted-foreground">
                {client.goal ? (
                  t(`quiz.goal.options.${client.goal}.title`)
                ) : (
                  <NoValue />
                )}
              </TableCell>
            )}
            {full && (
              <TableCell className={cn(client.planWeek && 'num')}>
                {client.planWeek !== undefined
                  ? t('overview.plan.week', {
                      week: client.planWeek,
                      weeks: client.planWeeks,
                    })
                  : client.planWeeks !== undefined
                    ? t('overview.plan.finished')
                    : t(`clients.planStates.${client.planState}`)}
              </TableCell>
            )}
            <TableCell className="text-muted-foreground">
              {client.lastLoggedAt === undefined
                ? t('overview.noLogs')
                : timeAgo(client.lastLoggedAt, now, i18n.language)}
            </TableCell>
            <TableCell>
              {client.sessionsDue === undefined ? (
                <NoValue />
              ) : (
                <span className="flex items-center gap-2.5">
                  <Progress
                    aria-hidden
                    value={
                      ((client.sessionsDone ?? 0) / client.sessionsDue) * 100
                    }
                    className="*:data-[slot=progress-indicator]:bg-chart-1 h-2 w-18"
                  />
                  <span className="num">
                    {t('overview.sessions', {
                      done: client.sessionsDone,
                      due: client.sessionsDue,
                    })}
                  </span>
                </span>
              )}
            </TableCell>
            <TableCell>
              {client.averageRpe === undefined ? (
                <NoValue />
              ) : (
                <span className="flex items-center gap-1.5">
                  <Dot className={rpeDot(client.averageRpe)} />
                  <span className="num">{rpe.format(client.averageRpe)}</span>
                </span>
              )}
            </TableCell>
            <TableCell>
              {client.readiness === undefined ||
              client.readinessBand === undefined ? (
                <span className="text-muted-foreground">
                  {t('overview.noCheckIn')}
                </span>
              ) : (
                <span className="flex items-center gap-1.5">
                  <Dot className={BAND_STYLE[client.readinessBand].dot} />
                  <span className="num">{client.readiness}</span>
                  {client.lighterTargets && (
                    <Badge variant="adjusted">
                      {t('overview.lighterTargets')}
                    </Badge>
                  )}
                </span>
              )}
            </TableCell>
            <TableCell>
              <StatusBadge status={client.status} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
