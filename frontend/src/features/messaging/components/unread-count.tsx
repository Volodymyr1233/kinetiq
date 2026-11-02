import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

const MOST_SHOWN = 99

/**
 * The number of unread messages, nothing at 0. The whole number is in hidden text right before the badge, so it joins
 * the announced name of the link it sits in: "Messages, 120 unread".
 */
export function UnreadCount({
  count,
  className,
}: {
  count: number
  className?: string
}) {
  const { t } = useTranslation()
  if (count <= 0) return null
  return (
    <>
      <span className="sr-only">{t('messaging.unread', { count })}</span>
      <Badge
        aria-hidden
        className={cn('num h-[18px] min-w-[18px] px-[5px]', className)}
      >
        {count > MOST_SHOWN ? `${MOST_SHOWN}+` : count}
      </Badge>
    </>
  )
}
