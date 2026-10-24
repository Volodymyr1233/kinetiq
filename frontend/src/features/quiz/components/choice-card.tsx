import type { ReactNode } from 'react'
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldLabel,
  FieldTitle,
} from '@/components/ui/field'

export type ChoiceOption<T extends string> = {
  value: T
  title: string
  description?: string
}

/** One option of a single- or multiple-choice step: the whole card labels the control passed as `children`. */
export function ChoiceCard({
  htmlFor,
  title,
  description,
  children,
}: {
  htmlFor: string
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <FieldLabel
      htmlFor={htmlFor}
      className="bg-card has-data-checked:border-primary has-data-checked:ring-primary has-data-checked:ring-1"
    >
      <Field
        orientation="horizontal"
        className="min-h-16 has-[>[data-slot=field-content]]:items-center"
      >
        <FieldContent>
          <FieldTitle className="text-base">{title}</FieldTitle>
          {description && <FieldDescription>{description}</FieldDescription>}
        </FieldContent>
        {children}
      </Field>
    </FieldLabel>
  )
}
