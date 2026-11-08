import { CheckIcon, CircleAlertIcon, CircleDotIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { SessionState } from '../api/session-states'

// a symbol for scanning the week, with the state in words for assistive technology
const MARKS: Partial<Record<SessionState, typeof CheckIcon>> = {
  DONE: CheckIcon,
  MISSED: CircleAlertIcon,
  TODAY: CircleDotIcon,
}

/** A small symbol under a day of the week strip for a session that is done, missed or today. */
export function DayMark({ state }: { state?: SessionState }) {
  const { t } = useTranslation()
  const Icon = state === undefined ? undefined : MARKS[state]
  return Icon === undefined || state === undefined ? null : (
    <span className="mt-1">
      <Icon aria-hidden className="text-muted-foreground size-4" />
      <span className="sr-only">{t(`plan.state.${state}`)}</span>
    </span>
  )
}
