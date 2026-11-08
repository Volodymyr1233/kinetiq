import { CheckIcon, CircleAlertIcon, CircleDotIcon } from 'lucide-react'
import type { SessionState } from '../api/session-states'

// the card beside it names the state in words, so the mark is a symbol for scanning the week
const MARKS: Partial<Record<SessionState, typeof CheckIcon>> = {
  DONE: CheckIcon,
  MISSED: CircleAlertIcon,
  TODAY: CircleDotIcon,
}

/** A small symbol under a day of the week strip for a session that is done, missed or today. */
export function DayMark({ state }: { state?: SessionState }) {
  const Icon = state === undefined ? undefined : MARKS[state]
  return Icon === undefined ? null : (
    <Icon aria-hidden className="text-muted-foreground mt-1 size-4" />
  )
}
