import { Trash2Icon } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { TabsContent } from '@/components/ui/tabs'
import type { WeightUnit } from '@/lib/weight'
import {
  dayName,
  DAYS,
  NAME_MAX_LENGTH,
  sessionTotals,
  type Day,
  type DraftSession,
} from '../plan-draft'
import { EntriesTable } from './entries-table'

/** The selected session: its name and day, the delete action, its exercises and its totals. */
export function SessionPanel({
  session,
  sessions,
  unit,
  autoFocusName,
  onChange,
  onDelete,
}: {
  session: DraftSession
  sessions: DraftSession[]
  unit: WeightUnit
  autoFocusName: boolean
  onChange: (
    change: Partial<Pick<DraftSession, 'name' | 'day' | 'entries'>>,
  ) => void
  onDelete: () => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const nameRef = useRef<HTMLInputElement>(null)
  const [confirming, setConfirming] = useState(false)
  const totals = sessionTotals(session)
  const label = session.name.trim() || t('plan.session.unnamed')

  useEffect(() => {
    if (autoFocusName) nameRef.current?.focus()
  }, [autoFocusName])

  return (
    <TabsContent value={session.key} className="pt-4">
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-end gap-x-4 gap-y-3">
          <Field className="w-72 max-w-full">
            <FieldLabel htmlFor={`${id}-name`}>
              {t('plan.session.name')}
            </FieldLabel>
            <Input
              ref={nameRef}
              id={`${id}-name`}
              autoComplete="off"
              maxLength={NAME_MAX_LENGTH}
              placeholder={t('plan.session.namePlaceholder')}
              value={session.name}
              onChange={(event) => onChange({ name: event.target.value })}
            />
          </Field>
          <Field className="w-44">
            <FieldLabel htmlFor={`${id}-day`}>
              {t('plan.session.day')}
            </FieldLabel>
            <Select
              value={session.day}
              onValueChange={(day) => onChange({ day: day as Day })}
            >
              <SelectTrigger id={`${id}-day`} className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {DAYS.map((day) => (
                  <SelectItem
                    key={day}
                    value={day}
                    disabled={sessions.some(
                      (other) => other.key !== session.key && other.day === day,
                    )}
                  >
                    {dayName(day, i18n.language)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Button
            variant="ghost"
            className="ml-auto"
            onClick={() =>
              session.entries.length > 0 ? setConfirming(true) : onDelete()
            }
          >
            <Trash2Icon data-icon="inline-start" aria-hidden="true" />
            {t('plan.session.delete')}
          </Button>
        </div>
        <div className="flex flex-wrap items-start gap-6">
          <div className="flex min-w-0 flex-[999_1_600px] flex-col gap-3">
            <EntriesTable
              session={session}
              sessionLabel={label}
              unit={unit}
              onChange={(entries) => onChange({ entries })}
            />
          </div>
          <Card
            data-density="compact"
            className="max-w-full flex-[1_1_280px] gap-3"
          >
            <CardHeader>
              <CardTitle>{label}</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="flex flex-col gap-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">
                    {t('plan.summary.exercises')}
                  </dt>
                  <dd className="num font-medium">{totals.exercises}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">
                    {t('plan.summary.sets')}
                  </dt>
                  <dd className="num font-medium">{totals.sets}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">
                    {t('plan.summary.time')}
                  </dt>
                  <dd className="num font-medium">
                    {t('plan.summary.minutes', { minutes: totals.minutes })}
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>
        </div>
      </div>
      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('plan.session.deleteDialog.title', { session: label })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t('plan.session.deleteDialog.body', {
                count: session.entries.length,
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('plan.cancel')}</AlertDialogCancel>
            <AlertDialogAction variant="destructive" onClick={onDelete}>
              {t('plan.session.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </TabsContent>
  )
}
