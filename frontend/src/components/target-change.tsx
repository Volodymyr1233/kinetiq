import { ArrowRightIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'

/**
 * A target as planned and as it is today, both already formatted: the planned value struck through, an arrow and
 * today's value. A screen reader hears both values in words; `past` reads a day gone by as "lighter", not "today".
 * When they are the same, only the value is shown.
 */
export function TargetChange({
  planned,
  today,
  past = false,
}: {
  planned: string
  today: string
  past?: boolean
}) {
  const { t } = useTranslation()

  if (planned === today) return <span className="num">{today}</span>

  return (
    <span className="num">
      <span aria-hidden className="inline-flex flex-wrap items-center gap-1">
        <s className="text-muted-foreground">{planned}</s>
        <ArrowRightIcon className="size-3.5 shrink-0" />
        <span className="text-foreground font-semibold">{today}</span>
      </span>
      <span className="sr-only">
        {t(past ? 'targetChangePast' : 'targetChange', { planned, today })}
      </span>
    </span>
  )
}
