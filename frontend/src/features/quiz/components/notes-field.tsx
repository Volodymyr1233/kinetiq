import { useTranslation } from 'react-i18next'
import { Field, FieldDescription, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'

/** Free text with a live character count; no more than `maxLength` is ever accepted. */
export function NotesField({
  id,
  label,
  placeholder,
  value,
  maxLength,
  onChange,
}: {
  id: string
  label: string
  placeholder: string
  value: string
  maxLength: number
  onChange: (value: string) => void
}) {
  const { t } = useTranslation()
  const countId = `${id}-count`

  return (
    <Field>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Textarea
        id={id}
        placeholder={placeholder}
        className="min-h-24 text-base md:text-base"
        value={value}
        maxLength={maxLength}
        // the browser already stops at maxLength; the cut also covers values it does not police
        onChange={(event) => onChange(event.target.value.slice(0, maxLength))}
        aria-describedby={countId}
      />
      <FieldDescription id={countId}>
        {t('quiz.characterCount', { count: value.length, max: maxLength })}
      </FieldDescription>
    </Field>
  )
}
