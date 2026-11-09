import { CheckIcon, SmartphoneIcon, TriangleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { SyncState } from '../api/session-log'

const LOOK = {
  saved: { variant: 'info', Icon: SmartphoneIcon, key: 'saved' },
  synced: { variant: 'success', Icon: CheckIcon, key: 'synced' },
  'not-synced': {
    variant: 'warning',
    Icon: TriangleAlertIcon,
    key: 'notSynced',
  },
} as const

/** Where a logged set stands, told by an icon and by words, never by colour alone. The words may wrap on a narrow row. */
export function SyncBadge({ state }: { state: SyncState }) {
  const { t } = useTranslation()
  const { variant, Icon, key } = LOOK[state]
  return (
    <Badge
      variant={variant}
      className="h-auto min-h-5 text-left whitespace-normal"
    >
      <Icon aria-hidden />
      {t(`sessionlog.sync.${key}`)}
    </Badge>
  )
}
