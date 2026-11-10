import { useTranslation } from 'react-i18next'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

const PERIODS = [4, 8, 12] as const
export type Period = (typeof PERIODS)[number]

/** The choice of the last 4, 8 or 12 plan weeks; one is always chosen. */
export function PeriodSwitch({
  value,
  onChange,
}: {
  value: Period
  onChange: (period: Period) => void
}) {
  const { t } = useTranslation()

  return (
    <ToggleGroup
      type="single"
      spacing={1}
      value={String(value)}
      // a single toggle group sends an empty value when the chosen option is chosen again; a period always has one
      onValueChange={(next) => {
        if (next) onChange(Number(next) as Period)
      }}
      aria-label={t('progress.period.label')}
      className="bg-muted rounded-lg p-1"
    >
      {PERIODS.map((period) => (
        <ToggleGroupItem
          key={period}
          value={String(period)}
          // a border, not a ring, so the focus ring still shows on the chosen option
          className="text-muted-foreground data-[state=on]:bg-background data-[state=on]:text-foreground data-[state=on]:border-primary h-12 border border-transparent px-4 font-normal hover:bg-transparent data-[state=on]:font-medium data-[state=on]:shadow-sm"
        >
          {t('progress.period.weeks', { count: period })}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  )
}
