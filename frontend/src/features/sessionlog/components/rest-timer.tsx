import { TimerIcon, XIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'

const two = (value: number) => String(value).padStart(2, '0')

/**
 * The countdown of the rest after a logged set. It is computed from the moment it ends, so a tab that was in the
 * background shows the right time when it returns. The page keys it by that moment, so a new one restarts it. It never
 * takes focus and never disables anything.
 */
export function RestTimer({
  endsAt,
  onDismiss,
}: {
  endsAt: number
  onDismiss: () => void
}) {
  const { t } = useTranslation()
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const tick = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(tick)
  }, [])

  const left = Math.max(0, Math.ceil((endsAt - now) / 1000))
  const over = left === 0

  return (
    <div className="bg-card text-card-foreground flex items-center gap-3 rounded-xl border px-4 py-2 shadow-md">
      <TimerIcon aria-hidden className="size-5 shrink-0" />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="text-muted-foreground text-xs">
          {t('sessionlog.rest.label')}
        </span>
        {/* a timer role is not announced on every tick; only the end is told to assistive technology below */}
        <span role="timer" className="num text-lg font-semibold">
          {over
            ? t('sessionlog.rest.over')
            : `${Math.floor(left / 60)}:${two(left % 60)}`}
        </span>
      </div>
      <p role="status" className="sr-only">
        {over ? t('sessionlog.rest.over') : ''}
      </p>
      <Button
        type="button"
        variant="ghost"
        className="size-12 shrink-0"
        aria-label={t('sessionlog.rest.dismiss')}
        onClick={onDismiss}
      >
        <XIcon aria-hidden />
      </Button>
    </div>
  )
}
