import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

export type Question = 'sleep' | 'soreness' | 'stress'

const VALUES = [1, 2, 3, 4, 5]

type AnswerGroupProps = {
  question: Question
  value: number | undefined
  onChoose: (value: number) => void
  /** While saving, the values keep focus and their state but take no choice. */
  busy: boolean
}

/**
 * One question of the check-in as a radio group of 1 to 5. The arrow keys only move between the values; Space, Enter,
 * a click or a tap chooses one, so moving never saves a value the client passed over.
 */
export function AnswerGroup({
  question,
  value,
  onChoose,
  busy,
}: AnswerGroupProps) {
  const { t } = useTranslation()
  const id = useId()

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-baseline justify-between gap-x-2">
        <span id={`${id}-question`} className="font-medium">
          {t(`readiness.checkIn.${question}.label`)}
        </span>
        <span id={`${id}-scale`} className="text-muted-foreground text-sm">
          {t(`readiness.checkIn.${question}.scale`)}
        </span>
      </div>
      <ToggleGroup
        type="single"
        variant="outline"
        spacing={1}
        role="radiogroup"
        aria-labelledby={`${id}-question`}
        aria-describedby={`${id}-scale`}
        value={value === undefined ? '' : String(value)}
        // choosing the chosen value again sends an empty value; an answer is never cleared that way
        onValueChange={(next) => {
          if (next !== '' && !busy) onChoose(Number(next))
        }}
        className="w-full justify-between"
      >
        {VALUES.map((option) => (
          <ToggleGroupItem
            key={option}
            value={String(option)}
            // not disabled: a disabled button that has focus drops it to the page
            aria-disabled={busy || undefined}
            // the chosen colours hold under hover too, which a tap leaves behind on a phone
            className="num data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:border-primary data-[state=on]:hover:bg-primary data-[state=on]:hover:text-primary-foreground size-12 min-w-12 text-base"
          >
            {option}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </div>
  )
}
