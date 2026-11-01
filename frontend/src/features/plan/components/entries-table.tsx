import {
  ArrowDownIcon,
  ArrowUpIcon,
  GripVerticalIcon,
  Trash2Icon,
} from 'lucide-react'
import { Fragment, useEffect, useState, type DragEvent } from 'react'
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
import { Button, buttonVariants } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { FieldError } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
  InputGroupText,
} from '@/components/ui/input-group'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useDialogFocus } from '@/hooks/use-dialog-focus'
import { cn } from '@/lib/utils'
import type { WeightUnit } from '@/lib/weight'
import {
  entryFromExercise,
  MAX_EXERCISES,
  moveEntry,
  target,
  type DraftEntry,
  type DraftSession,
  type EntryField,
} from '../plan-draft'
import { ExercisePicker } from './exercise-picker'

const ENTRY_FIELDS: EntryField[] = ['sets', 'amount', 'load', 'rpe', 'rest']
// on the cells, because a table row does not draw a shadow
const LINE_ABOVE = '[&>td]:shadow-[inset_0_2px_0_var(--ring)]'
const LINE_BELOW = '[&>td]:shadow-[inset_0_-2px_0_var(--ring)]'

/** The session's exercises in order, edited in place. Text stays as typed; the editor decides what is valid. */
export function EntriesTable({
  session,
  sessionLabel,
  unit,
  errorOf,
  onLeaveField,
  onChange,
}: {
  session: DraftSession
  sessionLabel: string
  unit: WeightUnit
  /** The message for a field that should show one now. */
  errorOf: (id: string) => string | undefined
  onLeaveField: (id: string) => void
  onChange: (entries: DraftEntry[]) => void
}) {
  const { t } = useTranslation()
  const entries = session.entries
  const full = entries.length >= MAX_EXERCISES
  const [removing, setRemoving] = useState<DraftEntry>()
  const [moved, setMoved] = useState<string>()
  const [focusId, setFocusId] = useState<{ id: string }>()
  const [dragging, setDragging] = useState<number>()
  const [dropBefore, setDropBefore] = useState<number>()
  const removeFocus = useDialogFocus()
  // no line where the row would stay put
  const line =
    dragging !== undefined &&
    dropBefore !== dragging &&
    dropBefore !== dragging + 1
      ? dropBefore
      : undefined

  // moving a row takes focus away from its control, so focus is set once the row is in its new place
  useEffect(() => {
    if (focusId) document.getElementById(focusId.id)?.focus()
  }, [focusId])

  function setField(index: number, field: EntryField, value: string) {
    onChange(
      entries.map((entry, row) =>
        row === index ? { ...entry, [field]: value } : entry,
      ),
    )
  }

  function move(from: number, to: number) {
    const entry = entries[from]
    onChange(moveEntry(entries, from, to))
    setMoved(
      t('plan.entries.moved', { exercise: entry.exerciseName, row: to + 1 }),
    )
    setFocusId({ id: target.move(entry.key) })
  }

  function endDrag() {
    setDragging(undefined)
    setDropBefore(undefined)
  }

  // the half of the row under the pointer says which side of it the dragged row lands on
  const dropZone = (upper: number, lower: number) => ({
    onDragOver: (event: DragEvent<HTMLTableRowElement>) => {
      if (dragging === undefined) return
      event.preventDefault()
      event.dataTransfer.dropEffect = 'move'
      const { top, height } = event.currentTarget.getBoundingClientRect()
      setDropBefore(event.clientY > top + height / 2 ? lower : upper)
    },
  })

  function remove(entry: DraftEntry) {
    const index = entries.indexOf(entry)
    const neighbour = entries[index + 1] ?? entries[index - 1]
    // the control that opened the dialog goes with its row
    const nextFocus = neighbour
      ? target.remove(neighbour.key)
      : target.addExercise(session.key)
    removeFocus.afterClosing(() => document.getElementById(nextFocus)?.focus())
    onChange(entries.filter((other) => other !== entry))
    setRemoving(undefined)
  }

  // kept while the dialog fades out, so its title does not lose the exercise
  const [shown, setShown] = useState(removing)
  if (removing && removing !== shown) setShown(removing)

  return (
    <div className="rounded-lg border" data-density="compact">
      {entries.length === 0 ? (
        <p className="text-muted-foreground px-3 py-4 text-sm">
          {t('plan.entries.empty')}
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">
                <span className="sr-only">{t('plan.entries.order')}</span>
              </TableHead>
              <TableHead>{t('plan.entries.exercise')}</TableHead>
              <TableHead className="text-right whitespace-normal">
                {t('plan.entries.sets')}
              </TableHead>
              <TableHead className="text-right whitespace-normal">
                {t('plan.entries.amount')}
              </TableHead>
              <TableHead className="text-right whitespace-normal">
                {t('plan.entries.load', { unit: t(`plan.units.${unit}`) })}
              </TableHead>
              <TableHead className="text-right whitespace-normal">
                {t('plan.entries.rpe')}
              </TableHead>
              <TableHead className="text-right whitespace-normal">
                {t('plan.entries.rest')}
              </TableHead>
              <TableHead className="w-10">
                <span className="sr-only">{t('plan.entries.actions')}</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody
            onDrop={(event) => {
              event.preventDefault()
              if (dragging !== undefined && line !== undefined)
                move(dragging, line > dragging ? line - 1 : line)
              endDrag()
            }}
            onDragLeave={(event) => {
              if (
                !event.currentTarget.contains(
                  event.relatedTarget as Node | null,
                )
              )
                setDropBefore(undefined)
            }}
          >
            {entries.map((entry, index) => {
              const names = { exercise: entry.exerciseName, row: index + 1 }
              const cell = (
                field: EntryField,
                label: string,
                width: string,
              ) => (
                <NumberCell
                  id={target.field(entry.key, field)}
                  label={label}
                  width={width}
                  value={entry[field]}
                  inputMode={field === 'load' ? 'decimal' : 'numeric'}
                  error={errorOf(target.field(entry.key, field))}
                  onLeave={onLeaveField}
                  onChange={(value) => setField(index, field, value)}
                />
              )
              const rowErrors = ENTRY_FIELDS.flatMap((field) => {
                const id = target.field(entry.key, field)
                const message = errorOf(id)
                return message ? [{ id, message }] : []
              })
              return (
                <Fragment key={entry.key}>
                  <TableRow
                    {...dropZone(index, index + 1)}
                    className={cn(
                      rowErrors.length > 0 && 'border-b-0',
                      dragging === index && 'opacity-50',
                      line === index && LINE_ABOVE,
                      line === entries.length &&
                        index === entries.length - 1 &&
                        rowErrors.length === 0 &&
                        LINE_BELOW,
                    )}
                  >
                    <TableCell className="align-top">
                      {entries.length > 1 && (
                        <MoveHandle
                          id={target.move(entry.key)}
                          label={t('plan.entries.move', names)}
                          row={index}
                          rows={entries.length}
                          onMove={(to) => move(index, to)}
                          onDragStart={(event) => {
                            event.dataTransfer.effectAllowed = 'move'
                            // Firefox starts only a drag that carries data
                            event.dataTransfer.setData(
                              'application/x-plan-entry',
                              entry.key,
                            )
                            const row = event.currentTarget.closest('tr')
                            if (row)
                              event.dataTransfer.setDragImage(row, 16, 16)
                            setDragging(index)
                          }}
                          onDragEnd={endDrag}
                        />
                      )}
                    </TableCell>
                    <TableCell className="min-w-32 pt-3 align-top font-medium whitespace-normal">
                      {entry.exerciseName}
                    </TableCell>
                    {cell('sets', t('plan.entries.fields.sets', names), 'w-14')}
                    <TableCell className="text-right align-top">
                      <div className="ml-auto w-20">
                        <InputGroup className="h-8">
                          <InputGroupInput
                            id={target.field(entry.key, 'amount')}
                            inputMode="numeric"
                            autoComplete="off"
                            aria-label={t(
                              `plan.entries.fields.amount.${entry.measure}`,
                              names,
                            )}
                            aria-invalid={
                              !!errorOf(target.field(entry.key, 'amount'))
                            }
                            aria-describedby={describedBy(
                              errorOf(target.field(entry.key, 'amount')),
                              target.field(entry.key, 'amount'),
                            )}
                            className="num h-8 pr-1 text-right"
                            value={entry.amount}
                            onChange={(event) =>
                              setField(index, 'amount', event.target.value)
                            }
                            onBlur={() =>
                              onLeaveField(target.field(entry.key, 'amount'))
                            }
                          />
                          <InputGroupAddon align="inline-end">
                            <InputGroupText>
                              {t(`plan.entries.measures.${entry.measure}`)}
                            </InputGroupText>
                          </InputGroupAddon>
                        </InputGroup>
                      </div>
                    </TableCell>
                    {cell(
                      'load',
                      t('plan.entries.fields.load', {
                        ...names,
                        unit: t(`plan.units.${unit}`),
                      }),
                      'w-16',
                    )}
                    {cell('rpe', t('plan.entries.fields.rpe', names), 'w-14')}
                    {cell('rest', t('plan.entries.fields.rest', names), 'w-14')}
                    <TableCell className="align-top">
                      <Button
                        id={target.remove(entry.key)}
                        variant="ghost"
                        size="icon-sm"
                        aria-label={t('plan.entries.remove', names)}
                        onClick={() => setRemoving(entry)}
                      >
                        <Trash2Icon aria-hidden="true" />
                      </Button>
                    </TableCell>
                  </TableRow>
                  {/* under the row, so a message never widens a narrow column */}
                  {rowErrors.length > 0 && (
                    <TableRow
                      {...dropZone(index + 1, index + 1)}
                      className={cn(
                        line === entries.length &&
                          index === entries.length - 1 &&
                          LINE_BELOW,
                      )}
                    >
                      <TableCell
                        colSpan={8}
                        className="pt-0 text-right whitespace-normal"
                      >
                        {rowErrors.map(({ id, message }) => (
                          <FieldError key={id} id={`${id}-error`}>
                            {message}
                          </FieldError>
                        ))}
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              )
            })}
          </TableBody>
        </Table>
      )}
      <div className="flex flex-wrap items-center gap-3 border-t px-3 py-2">
        <ExercisePicker
          id={target.addExercise(session.key)}
          disabled={full}
          sessionLabel={sessionLabel}
          onPick={(exercise) =>
            onChange([...entries, entryFromExercise(exercise)])
          }
        />
        <span className="text-muted-foreground text-sm">
          {t(full ? 'plan.entries.limit' : 'plan.entries.fromLibrary', {
            count: MAX_EXERCISES,
          })}
        </span>
      </div>
      <p role="status" className="sr-only">
        {moved}
      </p>
      <AlertDialog
        open={!!removing}
        onOpenChange={(open) => {
          if (!open) setRemoving(undefined)
        }}
      >
        <AlertDialogContent {...removeFocus.contentProps}>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('plan.entries.removeDialog.title', {
                exercise: shown?.exerciseName,
                session: sessionLabel,
              })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t('plan.entries.removeDialog.body')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('plan.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => removing && remove(removing)}
            >
              {t('plan.entries.removeDialog.confirm')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

/**
 * The row's grip: drag it to reorder, or press it for a menu that does the same without dragging (WCAG 2.5.7), by
 * pointer, touch and keyboard.
 */
function MoveHandle({
  id,
  label,
  row,
  rows,
  onMove,
  onDragStart,
  onDragEnd,
}: {
  id: string
  label: string
  row: number
  rows: number
  onMove: (to: number) => void
  onDragStart: (event: DragEvent<HTMLDivElement>) => void
  onDragEnd: () => void
}) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)

  return (
    <div className="relative w-fit">
      {/* no button element: Firefox does not drag one */}
      <div
        id={id}
        role="button"
        tabIndex={0}
        draggable
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        className={cn(
          buttonVariants({ variant: 'ghost', size: 'icon-sm' }),
          'focus:border-ring focus:ring-ring/50 cursor-grab focus:ring-3 active:cursor-grabbing',
        )}
        onClick={() => setOpen(true)}
        onKeyDown={(event) => {
          if (['Enter', ' ', 'ArrowDown'].includes(event.key)) {
            event.preventDefault()
            setOpen(true)
          }
        }}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
      >
        <GripVerticalIcon aria-hidden="true" />
      </div>
      <DropdownMenu open={open} onOpenChange={setOpen}>
        {/* only the menu's anchor and name: the Radix trigger cancels the press, and with it any drag */}
        <DropdownMenuTrigger asChild>
          <span
            aria-hidden="true"
            aria-label={label}
            tabIndex={-1}
            className="pointer-events-none absolute inset-0"
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent
          align="start"
          // by default as wide as its trigger, here the grip
          className="w-auto"
          onCloseAutoFocus={(event) => {
            event.preventDefault()
            document.getElementById(id)?.focus()
          }}
        >
          <DropdownMenuItem
            disabled={row === 0}
            onSelect={() => onMove(row - 1)}
          >
            <ArrowUpIcon aria-hidden="true" />
            {t('plan.entries.moveUp')}
          </DropdownMenuItem>
          <DropdownMenuItem
            disabled={row === rows - 1}
            onSelect={() => onMove(row + 1)}
          >
            <ArrowDownIcon aria-hidden="true" />
            {t('plan.entries.moveDown')}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}

const describedBy = (error: string | undefined, id: string) =>
  error ? `${id}-error` : undefined

function NumberCell({
  id,
  label,
  width,
  value,
  inputMode,
  error,
  onLeave,
  onChange,
}: {
  id: string
  label: string
  width: string
  value: string
  inputMode: 'numeric' | 'decimal'
  error?: string
  onLeave: (id: string) => void
  onChange: (value: string) => void
}) {
  return (
    <TableCell className="text-right align-top">
      <Input
        id={id}
        inputMode={inputMode}
        autoComplete="off"
        aria-label={label}
        aria-invalid={!!error}
        aria-describedby={describedBy(error, id)}
        className={cn('num ml-auto h-8 text-right', width)}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onBlur={() => onLeave(id)}
      />
    </TableCell>
  )
}
