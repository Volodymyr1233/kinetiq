import { MinusIcon, PlusIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'

/** A whole number changed one at a time with − and +, named by the step's heading. */
export function Stepper({
  labelledBy,
  value,
  min,
  max,
  unit,
  decreaseLabel,
  increaseLabel,
  onChange,
}: {
  labelledBy: string
  value: number
  min: number
  max: number
  unit: string
  decreaseLabel: string
  increaseLabel: string
  onChange: (value: number) => void
}) {
  const atMin = value <= min
  const atMax = value >= max
  // aria-disabled rather than disabled: a button that disables itself under the cursor would drop keyboard focus
  const stepButton =
    'size-12 aria-disabled:pointer-events-none aria-disabled:opacity-50'

  return (
    <div
      role="group"
      aria-labelledby={labelledBy}
      className="flex items-center justify-between gap-4"
    >
      <Button
        type="button"
        variant="outline"
        className={stepButton}
        aria-label={decreaseLabel}
        aria-disabled={atMin}
        onClick={() => !atMin && onChange(value - 1)}
      >
        <MinusIcon aria-hidden />
      </Button>
      <output
        aria-live="polite"
        className="flex flex-col items-center gap-1 text-center"
      >
        <span className="num text-5xl font-semibold">{value}</span>{' '}
        <span className="text-muted-foreground text-sm">{unit}</span>
      </output>
      <Button
        type="button"
        variant="outline"
        className={stepButton}
        aria-label={increaseLabel}
        aria-disabled={atMax}
        onClick={() => !atMax && onChange(value + 1)}
      >
        <PlusIcon aria-hidden />
      </Button>
    </div>
  )
}
