import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { cn } from '@/lib/utils'

type SettingToggleGroupProps = {
  value: string | undefined
  onValueChange: (value: string) => void
  options: readonly { value: string; label: string }[]
  /** Client screens: full width with 48 px options. */
  touch?: boolean
  disabled?: boolean
  'aria-labelledby': string
  'aria-describedby'?: string
}

/** One setting's options as on the boards: a muted track with the chosen option raised and bordered in ink. */
export function SettingToggleGroup({
  value,
  onValueChange,
  options,
  touch = false,
  ...props
}: SettingToggleGroupProps) {
  // a single toggle group sends an empty value when the chosen option is chosen again; a setting always has one
  const keepValue = (next: string) => {
    if (next) onValueChange(next)
  }

  return (
    <ToggleGroup
      type="single"
      spacing={1}
      value={value}
      onValueChange={keepValue}
      className={cn('bg-muted rounded-lg p-1', touch && 'w-full')}
      {...props}
    >
      {options.map((option) => (
        <ToggleGroupItem
          key={option.value}
          value={option.value}
          // a border, not a ring, so the focus ring still shows on the chosen option
          className={cn(
            'text-muted-foreground data-[state=on]:bg-background data-[state=on]:text-foreground data-[state=on]:border-primary border border-transparent font-normal hover:bg-transparent data-[state=on]:font-medium data-[state=on]:shadow-sm',
            touch && 'h-12 flex-1',
          )}
        >
          {option.label}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  )
}
