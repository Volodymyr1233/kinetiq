import {
  CheckIcon,
  CircleAlertIcon,
  CircleDotIcon,
  ClockIcon,
  type LucideIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { SessionState } from '../api/session-states'

const LOOK: Record<
  SessionState,
  {
    variant: 'success' | 'secondary' | 'destructive' | 'default' | 'outline'
    icon?: LucideIcon
    className?: string
  }
> = {
  DONE: { variant: 'success', icon: CheckIcon },
  IN_PROGRESS: { variant: 'secondary', icon: ClockIcon },
  MISSED: { variant: 'destructive', icon: CircleAlertIcon },
  TODAY: {
    variant: 'default',
    icon: CircleDotIcon,
    className: 'bg-brand text-brand-foreground',
  },
  PLANNED: { variant: 'outline' },
}

/** A session's state as a word with an icon, never by colour alone. No state shows nothing. */
export function SessionStateBadge({ state }: { state?: SessionState }) {
  const { t } = useTranslation()
  if (state === undefined) return null
  const { variant, icon: Icon, className } = LOOK[state]
  return (
    <Badge variant={variant} className={className}>
      {Icon && <Icon aria-hidden />}
      {t(`plan.state.${state}`)}
    </Badge>
  )
}
