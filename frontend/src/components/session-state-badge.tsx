import {
  CheckIcon,
  CircleAlertIcon,
  CircleDotIcon,
  ClockIcon,
  LockIcon,
  type LucideIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { components } from '@/lib/api/schema'

type SessionState = NonNullable<
  components['schemas']['SessionStatesResponse']['sessions'][number]['state']
>

const LOOK: Record<
  SessionState | 'LOCKED',
  {
    variant: 'success' | 'secondary' | 'destructive' | 'default' | 'outline'
    icon?: LucideIcon
    className?: string
  }
> = {
  DONE: { variant: 'success', icon: CheckIcon },
  LOCKED: { variant: 'secondary', icon: LockIcon },
  IN_PROGRESS: { variant: 'secondary', icon: ClockIcon },
  MISSED: { variant: 'destructive', icon: CircleAlertIcon },
  TODAY: {
    variant: 'default',
    icon: CircleDotIcon,
    className: 'bg-brand text-brand-foreground',
  },
  PLANNED: { variant: 'outline' },
}

/**
 * A session's state as a word with an icon, never by colour alone. A Done session whose 24 hours have ended shows
 * Locked. No state shows nothing.
 */
export function SessionStateBadge({
  state,
  locked = false,
}: {
  state?: SessionState
  locked?: boolean
}) {
  const { t } = useTranslation()
  if (state === undefined) return null
  const label = state === 'DONE' && locked ? 'LOCKED' : state
  const { variant, icon: Icon, className } = LOOK[label]
  return (
    <Badge variant={variant} className={className}>
      {Icon && <Icon aria-hidden />}
      {t(`plan.state.${label}`)}
    </Badge>
  )
}
