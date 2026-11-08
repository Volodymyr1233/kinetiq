import { ChevronDownIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router'
import { SessionStateBadge } from '@/components/session-state-badge'
import { Button } from '@/components/ui/button'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import { locksAt } from '@/lib/edit-window'
import type { OwnPlan } from '../api/client-plan'
import type { SessionState } from '../api/session-states'
import { EntryRow } from './entry-row'

type Session = OwnPlan['sessions'][number]

/**
 * The way from a past session's card to its log: "Edit log" while its sets can still change, "View log" after. The
 * Plan's address goes along, so the logger's back link returns to this week.
 */
function PastLogLink({ id, startedAt }: { id: string; startedAt: string }) {
  const { t } = useTranslation()
  const { pathname, search } = useLocation()
  // read once: the logger itself turns read-only when the 24 hours end
  const [open] = useState(() => Date.now() < locksAt(startedAt))

  return (
    <Button variant="outline" size="touch" className="w-full" asChild>
      <Link to={`/app/session/${id}`} state={{ from: pathname + search }}>
        {t(open ? 'plan.client.editLog' : 'plan.client.viewLog')}
      </Link>
    </Button>
  )
}

/**
 * One session of the week, closed to its name and state and opened to its exercises and their targets. A past session
 * with a log links to it, and a finished session to its summary; the addresses are all this feature knows of the
 * logger.
 */
export function SessionCard({
  session,
  state,
  summaryId,
  pastLog,
}: {
  session: Session
  state?: SessionState
  // the session log of a finished session, whose summary the opened card links to
  summaryId?: string
  // the session log of an earlier day, started, finished or not
  pastLog?: { id: string; startedAt: string }
}) {
  const { t } = useTranslation()
  const withSummary = state === 'DONE' && summaryId !== undefined

  return (
    <Collapsible className="bg-card text-card-foreground rounded-xl border">
      <CollapsibleTrigger className="group focus-visible:ring-ring/50 flex min-h-12 w-full items-center justify-between gap-3 rounded-xl px-4 py-2 text-left outline-none focus-visible:ring-3">
        <span className="flex min-w-0 flex-col">
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-medium break-words">{session.name}</span>
            <SessionStateBadge state={state} />
          </span>
          <span className="text-muted-foreground text-sm">
            {t('plan.client.exercises', { count: session.entries.length })}
          </span>
        </span>
        <ChevronDownIcon
          aria-hidden
          className="text-muted-foreground size-5 shrink-0 transition-transform group-data-[state=open]:rotate-180 motion-reduce:transition-none"
        />
      </CollapsibleTrigger>
      <CollapsibleContent>
        <ol className="flex flex-col divide-y border-t">
          {session.entries.map((entry, index) => (
            // the same exercise can appear twice in a session, so its place is its key
            <EntryRow key={index} entry={entry} />
          ))}
        </ol>
        {(pastLog !== undefined || withSummary) && (
          <div className="flex flex-col gap-2 border-t p-3">
            {pastLog !== undefined && <PastLogLink {...pastLog} />}
            {withSummary && (
              <Button variant="outline" size="touch" className="w-full" asChild>
                <Link to={`/app/summary/${summaryId}`}>
                  {t('plan.client.viewSummary')}
                </Link>
              </Button>
            )}
          </div>
        )}
      </CollapsibleContent>
    </Collapsible>
  )
}
