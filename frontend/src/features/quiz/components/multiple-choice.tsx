import { useId } from 'react'
import { Checkbox } from '@/components/ui/checkbox'
import { ChoiceCard, type ChoiceOption } from './choice-card'

/** A quiz step where any number of options is chosen, named by the step's heading. */
export function MultipleChoice<T extends string>({
  labelledBy,
  options,
  value,
  onChange,
}: {
  labelledBy: string
  options: ChoiceOption<T>[]
  value: T[]
  onChange: (value: T[]) => void
}) {
  const id = useId()

  function toggle(code: T, checked: boolean) {
    // rebuilt from the options so the chosen codes keep the screen's order
    onChange(
      options
        .map((option) => option.value)
        .filter((other) => (other === code ? checked : value.includes(other))),
    )
  }

  return (
    <div
      role="group"
      aria-labelledby={labelledBy}
      className="flex flex-col gap-2"
    >
      {options.map((option) => (
        <ChoiceCard
          key={option.value}
          htmlFor={`${id}-${option.value}`}
          title={option.title}
          description={option.description}
        >
          <Checkbox
            id={`${id}-${option.value}`}
            checked={value.includes(option.value)}
            onCheckedChange={(checked) =>
              toggle(option.value, checked === true)
            }
          />
        </ChoiceCard>
      ))}
    </div>
  )
}
