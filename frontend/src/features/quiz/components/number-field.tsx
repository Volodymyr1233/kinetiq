import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from '@/components/ui/input-group'

/** A number with its unit inside the field; shows the message it is given, the form decides when. */
export function NumberField({
  id,
  label,
  unit,
  placeholder,
  inputMode,
  value,
  onChange,
  onBlur,
  message,
  note,
}: {
  id: string
  label: string
  unit: string
  placeholder: string
  inputMode: 'numeric' | 'decimal'
  value: string
  onChange: (value: string) => void
  onBlur: () => void
  message?: string
  note?: string
}) {
  const unitId = `${id}-unit`
  const hintId = `${id}-hint`
  const hasHint = Boolean(message ?? note)

  return (
    <Field data-invalid={Boolean(message)}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <InputGroup className="h-(--control-h-touch)">
        {/* text, not number: a Polish decimal comma stays as typed and scrolling never changes the value */}
        <InputGroupInput
          id={id}
          type="text"
          inputMode={inputMode}
          autoComplete="off"
          placeholder={placeholder}
          className="num h-full text-base placeholder:font-sans md:text-base"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onBlur={onBlur}
          aria-invalid={message ? true : undefined}
          aria-describedby={hasHint ? `${unitId} ${hintId}` : unitId}
        />
        <InputGroupAddon align="inline-end" id={unitId}>
          {unit}
        </InputGroupAddon>
      </InputGroup>
      {message ? (
        <FieldError id={hintId}>{message}</FieldError>
      ) : (
        note && <FieldDescription id={hintId}>{note}</FieldDescription>
      )}
    </Field>
  )
}
