import { useId } from 'react'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { ChoiceCard, type ChoiceOption } from './choice-card'

/** A quiz step where exactly one option is chosen, named by the step's heading. */
export function SingleChoice<T extends string>({
  labelledBy,
  options,
  value,
  onChange,
}: {
  labelledBy: string
  options: ChoiceOption<T>[]
  value?: T
  onChange: (value: T) => void
}) {
  const id = useId()

  return (
    <RadioGroup
      aria-labelledby={labelledBy}
      // an empty value keeps the group controlled while nothing is chosen
      value={value ?? ''}
      onValueChange={(next) => onChange(next as T)}
      className="gap-2"
    >
      {options.map((option) => (
        <ChoiceCard
          key={option.value}
          htmlFor={`${id}-${option.value}`}
          title={option.title}
          description={option.description}
        >
          <RadioGroupItem value={option.value} id={`${id}-${option.value}`} />
        </ChoiceCard>
      ))}
    </RadioGroup>
  )
}
