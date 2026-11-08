import { CheckIcon, MinusIcon, PlusIcon } from 'lucide-react'
import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { fromKg, type WeightUnit } from '@/lib/weight'
import {
  MAX_LOAD_KG,
  MIN_LOAD_KG,
  RPES,
  steppedAmount,
  steppedLoad,
  type Problems,
  type Values,
} from '../set-values'

type Measure = 'REPS' | 'SECONDS'

/**
 * The entry panel of one set: load in steps or typed, repetitions or seconds, RPE chips, and the tick. Every control is
 * at least 48 px, because it is used between sets with a phone in one hand.
 */
export function EntryPanel({
  exercise,
  number,
  measure,
  unit,
  values,
  problems,
  logged,
  pending,
  onChange,
  onSubmit,
}: {
  exercise: string
  number: number
  measure: Measure
  unit: WeightUnit
  values: Values
  problems: Problems
  logged: boolean
  pending: boolean
  onChange: (values: Values) => void
  onSubmit: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const unitLabel = t(`plan.units.${unit}`)
  const amountLabel = t(`sessionlog.panel.amount.${measure}`)
  const which = { exercise, number }

  return (
    <div className="bg-muted/40 flex flex-col gap-4 border-t px-4 py-4">
      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-load`}>
          {t('sessionlog.panel.load', { unit: unitLabel })}
        </Label>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="touch"
            className="w-12 shrink-0 px-0"
            aria-label={t('sessionlog.panel.loadLess', which)}
            onClick={() =>
              onChange({ ...values, load: steppedLoad(values.load, unit, -1) })
            }
          >
            <MinusIcon aria-hidden />
          </Button>
          <Input
            id={`${id}-load`}
            inputMode="decimal"
            autoComplete="off"
            className="num h-12 text-center text-base"
            placeholder={t('sessionlog.panel.noLoad')}
            value={values.load}
            aria-invalid={problems.load === true}
            aria-describedby={problems.load ? `${id}-load-problem` : undefined}
            aria-label={t('sessionlog.panel.loadField', {
              ...which,
              unit: unitLabel,
            })}
            onChange={(event) =>
              onChange({ ...values, load: event.target.value })
            }
          />
          <Button
            type="button"
            variant="outline"
            size="touch"
            className="w-12 shrink-0 px-0"
            aria-label={t('sessionlog.panel.loadMore', which)}
            onClick={() =>
              onChange({ ...values, load: steppedLoad(values.load, unit, 1) })
            }
          >
            <PlusIcon aria-hidden />
          </Button>
        </div>
        {problems.load && (
          <p id={`${id}-load-problem`} className="text-destructive text-sm">
            {t('sessionlog.panel.loadProblem', {
              min: fromKg(MIN_LOAD_KG, unit),
              max: fromKg(MAX_LOAD_KG, unit),
              unit: unitLabel,
            })}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor={`${id}-amount`}>{amountLabel}</Label>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="touch"
            className="w-12 shrink-0 px-0"
            aria-label={t('sessionlog.panel.amountLess', {
              ...which,
              amount: amountLabel,
            })}
            onClick={() =>
              onChange({ ...values, amount: steppedAmount(values.amount, -1) })
            }
          >
            <MinusIcon aria-hidden />
          </Button>
          <Input
            id={`${id}-amount`}
            inputMode="numeric"
            autoComplete="off"
            className="num h-12 text-center text-base"
            value={values.amount}
            aria-invalid={problems.amount === true}
            aria-describedby={
              problems.amount ? `${id}-amount-problem` : undefined
            }
            aria-label={t('sessionlog.panel.amountField', {
              ...which,
              amount: amountLabel,
            })}
            onChange={(event) =>
              onChange({ ...values, amount: event.target.value })
            }
          />
          <Button
            type="button"
            variant="outline"
            size="touch"
            className="w-12 shrink-0 px-0"
            aria-label={t('sessionlog.panel.amountMore', {
              ...which,
              amount: amountLabel,
            })}
            onClick={() =>
              onChange({ ...values, amount: steppedAmount(values.amount, 1) })
            }
          >
            <PlusIcon aria-hidden />
          </Button>
        </div>
        {problems.amount && (
          <p id={`${id}-amount-problem`} className="text-destructive text-sm">
            {t('sessionlog.panel.amountProblem')}
          </p>
        )}
      </div>

      <div className="@container flex flex-col gap-2">
        <span id={`${id}-rpe`} className="text-sm font-medium">
          {t('sessionlog.panel.rpe')}
        </span>
        <ToggleGroup
          type="single"
          variant="outline"
          spacing={1}
          aria-labelledby={`${id}-rpe`}
          // one row when six 48 px chips fit, else two even rows of three
          className="grid w-full grid-cols-3 @min-[19.25rem]:grid-cols-6"
          value={String(values.rpe)}
          // a chip stays chosen when it is pressed again: a set always has an RPE
          onValueChange={(rpe) =>
            rpe !== '' && onChange({ ...values, rpe: Number(rpe) })
          }
        >
          {RPES.map((rpe) => (
            <ToggleGroupItem
              key={rpe}
              value={String(rpe)}
              aria-label={t('sessionlog.panel.rpeOption', { rpe, ...which })}
              className="num data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:border-primary data-[state=on]:hover:bg-primary data-[state=on]:hover:text-primary-foreground h-12 w-full min-w-12"
            >
              {rpe}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      <Button
        type="button"
        size="touch"
        className="bg-brand text-brand-foreground hover:bg-brand/80"
        disabled={pending}
        aria-busy={pending}
        onClick={onSubmit}
      >
        {pending ? (
          <Spinner data-icon="inline-start" aria-hidden="true" />
        ) : (
          <CheckIcon aria-hidden />
        )}
        {logged ? t('sessionlog.panel.update') : t('sessionlog.panel.log')}
      </Button>
    </div>
  )
}
