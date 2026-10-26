import { zodResolver } from '@hookform/resolvers/zod'
import { useId, useState, type ReactNode } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import type { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
  InputGroupText,
} from '@/components/ui/input-group'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import { Textarea } from '@/components/ui/textarea'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import {
  CATEGORIES,
  useCreateExercise,
  useUpdateExercise,
  type Exercise,
} from '../api/exercises'
import {
  exerciseDefaults,
  exerciseSchema,
  MEASURES,
  toValues,
  type ExerciseValues,
} from '../exercise-schema'

type FieldName = keyof ExerciseValues

// the dialog's own message for each field the server may refuse, so both say the same
const SERVER_MESSAGES: Partial<Record<FieldName, (code: string) => string>> = {
  name: (code) =>
    code === 'size' ? 'exercises.errors.nameLength' : 'exercises.errors.name',
  equipment: () => 'exercises.errors.equipmentLength',
  sets: () => 'exercises.errors.sets',
  amount: () => 'exercises.errors.amount',
  restSeconds: () => 'exercises.errors.rest',
  cues: () => 'exercises.errors.cuesLength',
}

const FIELD_ORDER = Object.keys(SERVER_MESSAGES)

// the field errors of a validation.failed problem detail, in the order the form shows the fields
function fieldErrorsOf(error: unknown) {
  const listed =
    typeof error === 'object' && error !== null && 'errors' in error
      ? (error.errors as { field: string; code: string }[])
      : []
  return [...listed].sort(
    (a, b) => FIELD_ORDER.indexOf(a.field) - FIELD_ORDER.indexOf(b.field),
  )
}

/**
 * Adds an exercise, or edits `exercise` when given. `children` is the button that opens it; `onSaved` gets the saved
 * exercise and whether it was added.
 */
export function ExerciseDialog({
  exercise,
  onSaved,
  children,
}: {
  exercise?: Exercise
  onSaved: (exercise: Exercise, created: boolean) => void
  children: ReactNode
}) {
  const [open, setOpen] = useState(false)
  // a fresh form every time the dialog opens
  const [session, setSession] = useState(0)

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        if (next) setSession((count) => count + 1)
      }}
    >
      <DialogTrigger asChild>{children}</DialogTrigger>
      {open && (
        <ExerciseDialogContent
          key={session}
          exercise={exercise}
          onClose={() => setOpen(false)}
          onSaved={(saved) => {
            setOpen(false)
            onSaved(saved, !exercise)
          }}
        />
      )}
    </Dialog>
  )
}

function ExerciseDialogContent({
  exercise,
  onClose,
  onSaved,
}: {
  exercise?: Exercise
  onClose: () => void
  onSaved: (exercise: Exercise) => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const createExercise = useCreateExercise()
  const updateExercise = useUpdateExercise()
  const [failure, setFailure] = useState<string>()
  const form = useForm<
    ExerciseValues,
    unknown,
    z.output<typeof exerciseSchema>
  >({
    resolver: zodResolver(exerciseSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: exercise ? toValues(exercise) : exerciseDefaults,
  })
  const { errors, isDirty } = form.formState
  const measure = useWatch({ control: form.control, name: 'measure' })
  const busy = createExercise.isPending || updateExercise.isPending

  function refuse(error: unknown) {
    const code = problemCode(error)
    if (code === 'exercise.not-found') {
      onClose()
      toast.error(t('errors.exercise.not-found'))
      return
    }
    if (code === 'exercise.name-taken') {
      form.setError(
        'name',
        { type: 'server', message: 'errors.exercise.name-taken' },
        { shouldFocus: true },
      )
      return
    }
    const fieldErrors = code === 'validation.failed' ? fieldErrorsOf(error) : []
    if (
      fieldErrors.length &&
      fieldErrors.every((fieldError) => fieldError.field in SERVER_MESSAGES)
    ) {
      fieldErrors.forEach((fieldError, index) => {
        const field = fieldError.field as FieldName
        form.setError(
          field,
          {
            type: 'server',
            message: SERVER_MESSAGES[field]?.(fieldError.code),
          },
          { shouldFocus: index === 0 },
        )
      })
      return
    }
    setFailure(errorMessageKey(error))
  }

  const onSubmit = form.handleSubmit(({ measure: chosen, ...values }) => {
    if (busy) return
    setFailure(undefined)
    const callbacks = { onSuccess: onSaved, onError: refuse }
    if (exercise) {
      updateExercise.mutate({ id: exercise.id, body: values }, callbacks)
    } else {
      createExercise.mutate({ ...values, measure: chosen }, callbacks)
    }
  })

  // the server's verdict no longer applies once the field changes
  function control(name: FieldName) {
    const invalid = !!errors[name]
    return {
      id: `${id}-${name}`,
      'aria-invalid': invalid,
      'aria-describedby': invalid ? `${id}-${name}-error` : undefined,
      ...form.register(name, {
        onChange: () => {
          if (errors[name]?.type === 'server') form.clearErrors(name)
        },
      }),
    }
  }

  function label(name: FieldName, text: string) {
    return <FieldLabel htmlFor={`${id}-${name}`}>{text}</FieldLabel>
  }

  function error(name: FieldName) {
    const message = errors[name]?.message
    return (
      message && (
        <FieldError id={`${id}-${name}-error`}>{t(message)}</FieldError>
      )
    )
  }

  return (
    <DialogContent
      aria-describedby={undefined}
      // fits a narrow screen and scrolls inside when taller than it
      className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-120"
      // half-typed entries are not lost to a stray click; Escape and Cancel still close
      onInteractOutside={(event) => {
        if (isDirty) event.preventDefault()
      }}
    >
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-6">
        <DialogHeader>
          <DialogTitle>
            {t(
              exercise
                ? 'exercises.dialog.editTitle'
                : 'exercises.dialog.addTitle',
            )}
          </DialogTitle>
        </DialogHeader>
        <FieldGroup className="gap-4">
          <Field data-invalid={!!errors.name}>
            {label('name', t('exercises.dialog.name'))}
            <Input
              maxLength={100}
              autoComplete="off"
              placeholder={t('exercises.dialog.namePlaceholder')}
              readOnly={busy}
              {...control('name')}
            />
            {error('name')}
          </Field>
          <Field>
            {label('category', t('exercises.dialog.category'))}
            <Controller
              control={form.control}
              name="category"
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={busy}
                >
                  <SelectTrigger id={`${id}-category`} className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORIES.map((category) => (
                      <SelectItem key={category} value={category}>
                        {t(`exercises.categories.${category}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </Field>
          {exercise ? (
            // fixed once added: a changed measure would change what the amounts in plans mean
            <Field>
              <span className="text-sm leading-snug font-medium">
                {t('exercises.dialog.measure')}
              </span>
              <p>{t(`exercises.dialog.measures.${exercise.measure}`)}</p>
            </Field>
          ) : (
            <FieldSet>
              <FieldLegend id={`${id}-measure`} variant="label">
                {t('exercises.dialog.measure')}
              </FieldLegend>
              <Controller
                control={form.control}
                name="measure"
                render={({ field }) => (
                  <RadioGroup
                    aria-labelledby={`${id}-measure`}
                    value={field.value}
                    onValueChange={field.onChange}
                    disabled={busy}
                    className="flex gap-6"
                  >
                    {MEASURES.map((option) => (
                      <Field
                        key={option}
                        orientation="horizontal"
                        className="w-auto"
                      >
                        <RadioGroupItem
                          value={option}
                          id={`${id}-measure-${option}`}
                        />
                        <FieldLabel
                          htmlFor={`${id}-measure-${option}`}
                          className="font-normal"
                        >
                          {t(`exercises.dialog.measures.${option}`)}
                        </FieldLabel>
                      </Field>
                    ))}
                  </RadioGroup>
                )}
              />
            </FieldSet>
          )}
          <Field data-invalid={!!errors.equipment}>
            {label('equipment', t('exercises.dialog.equipment'))}
            <Input
              maxLength={50}
              autoComplete="off"
              readOnly={busy}
              {...control('equipment')}
            />
            {error('equipment')}
          </Field>
          <div className="grid grid-cols-3 items-start gap-3">
            <Field data-invalid={!!errors.sets}>
              {label('sets', t('exercises.dialog.sets'))}
              <Input
                inputMode="numeric"
                className="num"
                readOnly={busy}
                {...control('sets')}
              />
              {error('sets')}
            </Field>
            <Field data-invalid={!!errors.amount}>
              {label(
                'amount',
                t(
                  measure === 'SECONDS'
                    ? 'exercises.dialog.seconds'
                    : 'exercises.dialog.reps',
                ),
              )}
              <Input
                inputMode="numeric"
                className="num"
                readOnly={busy}
                {...control('amount')}
              />
              {error('amount')}
            </Field>
            <Field data-invalid={!!errors.restSeconds}>
              {label('restSeconds', t('exercises.dialog.rest'))}
              <InputGroup>
                <InputGroupInput
                  inputMode="numeric"
                  className="num"
                  readOnly={busy}
                  {...control('restSeconds')}
                />
                <InputGroupAddon align="inline-end">
                  <InputGroupText>
                    {t('exercises.dialog.restUnit')}
                  </InputGroupText>
                </InputGroupAddon>
              </InputGroup>
              {error('restSeconds')}
            </Field>
          </div>
          <Field data-invalid={!!errors.cues}>
            {label('cues', t('exercises.dialog.cues'))}
            <Textarea maxLength={500} readOnly={busy} {...control('cues')} />
            {error('cues')}
          </Field>
        </FieldGroup>
        {failure && (
          <Alert variant="destructive">
            <AlertDescription>{t(failure)}</AlertDescription>
          </Alert>
        )}
        <DialogFooter>
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t('exercises.dialog.cancel')}
            </Button>
          </DialogClose>
          <Button type="submit" disabled={busy} aria-busy={busy}>
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('exercises.dialog.save')}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  )
}
