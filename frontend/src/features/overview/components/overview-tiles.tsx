import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Card, CardContent, CardDescription } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'
import type { Overview } from '../api/overview'

/** The four KPI tiles across all clients: active clients, this week's sessions and RPE, and what waits for the trainer. */
export function OverviewTiles({ overview }: { overview: Overview }) {
  const { t, i18n } = useTranslation()
  const number = (value: number, digits = 0) =>
    new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    }).format(value)
  const { sessionsDone: done, sessionsPlanned: planned } = overview
  const waiting = overview.draftsWaiting + overview.planChangesWaiting

  return (
    <section
      aria-label={t('overview.tiles.label')}
      className="grid grid-cols-[repeat(auto-fit,minmax(13rem,1fr))] gap-4"
    >
      <Tile label={t('overview.tiles.active')}>
        <Figure value={number(overview.activeClients)} />
        <Meaning>
          {t('overview.tiles.ofClients', { count: overview.totalClients })}
        </Meaning>
      </Tile>
      <Tile label={t('overview.tiles.sessions')}>
        <Figure
          value={number(done)}
          unit={t('overview.tiles.ofPlanned', { planned: number(planned) })}
        />
        <Progress
          value={planned > 0 ? (done / planned) * 100 : 0}
          aria-label={t('overview.tiles.sessions')}
          getValueLabel={() =>
            t('overview.tiles.sessionsValue', { done, count: planned })
          }
          className="*:data-[slot=progress-indicator]:bg-chart-1 mt-1 h-2"
        />
      </Tile>
      <Tile label={t('overview.tiles.rpe')}>
        {overview.averageRpe === undefined ? (
          <>
            <Figure value="—" />
            <Meaning>{t('overview.tiles.noSets')}</Meaning>
          </>
        ) : (
          <>
            <Figure value={number(overview.averageRpe, 1)} />
            <Change value={overview.averageRpeChange} format={number} />
          </>
        )}
      </Tile>
      <Tile label={t('overview.tiles.waiting')} to="/trainer/approvals">
        <Figure value={number(waiting)} />
        <Meaning>
          {t('overview.tiles.waitingParts', {
            changes: t('overview.tiles.planChanges', {
              count: overview.planChangesWaiting,
            }),
            plans: t('overview.tiles.newPlans', {
              count: overview.draftsWaiting,
            }),
          })}
        </Meaning>
      </Tile>
    </section>
  )
}

// a tile with `to` is one link as a whole; its label names the link, and the focus ring goes round the tile
function Tile({
  label,
  to,
  children,
}: {
  label: string
  to?: string
  children: ReactNode
}) {
  return (
    <Card
      size="sm"
      variant="metric"
      className={cn(
        to &&
          'hover:border-ring has-focus-visible:border-ring has-focus-visible:ring-ring/50 relative has-focus-visible:ring-3',
      )}
    >
      <CardContent className="flex flex-col gap-1.5">
        <CardDescription>
          {to ? (
            <Link
              to={to}
              className="after:absolute after:inset-0 focus-visible:outline-none"
            >
              {label}
            </Link>
          ) : (
            label
          )}
        </CardDescription>
        {children}
      </CardContent>
    </Card>
  )
}

function Figure({ value, unit }: { value: string; unit?: string }) {
  return (
    <p className="flex flex-wrap items-baseline gap-x-1.5">
      <span className="num text-metric font-medium">{value}</span>
      {unit !== undefined && (
        <span className="text-muted-foreground text-sm">{unit}</span>
      )}
    </p>
  )
}

function Meaning({ children }: { children: ReactNode }) {
  return <p className="text-muted-foreground text-sm">{children}</p>
}

/** The design system's KPI delta: an arrow, the size and what is compared; no line when there is nothing to compare. */
function Change({
  value,
  format,
}: {
  value: number | undefined
  format: (size: number, digits?: number) => string
}) {
  const { t } = useTranslation()
  if (value === undefined) return null
  if (value === 0) return <Meaning>{t('overview.tiles.same')}</Meaning>
  const up = value > 0
  return (
    <Meaning>
      {/* the arrow is drawn; its direction is read out in words */}
      <span aria-hidden="true">{up ? '▲' : '▼'} </span>
      <span className="sr-only">
        {up ? t('overview.tiles.up') : t('overview.tiles.down')}{' '}
      </span>
      {t('overview.tiles.vsLastWeek', { change: format(Math.abs(value), 1) })}
    </Meaning>
  )
}
