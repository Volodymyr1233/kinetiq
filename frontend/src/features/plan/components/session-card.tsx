import { ChevronDownIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import type { OwnPlan } from '../api/client-plan'
import type { SessionState } from '../api/session-states'
import { EntryRow } from './entry-row'
import { SessionStateBadge } from './session-state-badge'

type Session = OwnPlan['sessions'][number]

/**
 * One session of the week, closed to its name and state and opened to its exercises and their targets. A finished
 * session also links to its summary; the address is all this feature knows of the logger.
 */
export function SessionCard({
  session,
  state,
  summaryId,
}: {
  session: Session
  state?: SessionState
  // the session log of a finished session, whose summary the opened card links to
  summaryId?: string
}) {
  const { t } = useTranslation()

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
        {state === 'DONE' && summaryId !== undefined && (
          <div className="border-t p-3">
            <Button variant="outline" size="touch" className="w-full" asChild>
              <Link to={`/app/summary/${summaryId}`}>
                {t('plan.client.viewSummary')}
              </Link>
            </Button>
          </div>
        )}
      </CollapsibleContent>
    </Collapsible>
  )
}
