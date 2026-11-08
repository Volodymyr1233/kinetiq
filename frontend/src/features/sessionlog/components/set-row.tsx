import { CheckIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { cn } from '@/lib/utils'
import { fromKg, useWeightUnit } from '@/lib/weight'
import {
  useLogSet,
  type LoggedEntry,
  type PlannedSet,
} from '../api/session-log'
import { bodyOf, problemsOf, valuesOf, type Values } from '../set-values'
import { EntryPanel } from './entry-panel'

/**
 * One planned set: its targets in grey until it is logged, and what the client did afterwards. A tick logs the set as
 * planned; choosing the row opens the entry panel to log other values or to change a logged set. A locked row only
 * shows what was logged, with nothing to focus.
 */
export function SetRow({
  sessionId,
  entry,
  set,
  open,
  locked,
  onToggle,
  onLogged,
  onLocked,
}: {
  sessionId: string
  entry: LoggedEntry
  set: PlannedSet
  open: boolean
  locked: boolean
  onToggle: () => void
  // told when a set is logged for the first time, so the rest after it can start; a change does not start one
  onLogged: (restSeconds: number) => void
  // told when the server refuses a log because the session is locked, which the device's clock may not know yet
  onLocked: () => void
}) {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const log = useLogSet(sessionId)
  // made once, so a send that is repeated after a failure carries the same id and stores the set once
  const [ownId] = useState(() => crypto.randomUUID())
  const [edited, setEdited] = useState<Values | null>(null)
  const [tried, setTried] = useState(false)

  const logged = set.log !== undefined
  const shown = set.log ?? set.target
  const values = edited ?? valuesOf(shown, unit)
  const problems = problemsOf(values, unit)
  const measure = t(`plan.entries.measures.${entry.measure}`)
  const which = { exercise: entry.exerciseName, number: set.number }

  const submit = (toLog: Values) => {
    setTried(true)
    if (Object.keys(problemsOf(toLog, unit)).length > 0) return
    log.mutate(
      {
        setLogId: set.log?.id ?? ownId,
        body: bodyOf(toLog, unit, {
          entryPosition: entry.position,
          setNumber: set.number,
        }),
      },
      {
        onSuccess: () => {
          setEdited(null)
          setTried(false)
          if (open) onToggle()
          if (!logged) onLogged(entry.restSeconds)
        },
        onError: (error) => {
          if (problemCode(error) === 'session-log.locked') onLocked()
        },
      },
    )
  }

  const shownValues = (
    <span className="flex min-w-0 flex-1 flex-wrap gap-x-3 gap-y-0.5">
      <span className="num">
        {t('sessionlog.row.amount', {
          amount: shown.amount,
          unit: measure,
        })}
      </span>
      <span className="num">
        {shown.loadKg === undefined
          ? t('sessionlog.row.noLoad')
          : t('sessionlog.row.load', {
              load: fromKg(shown.loadKg, unit),
              unit: t(`plan.units.${unit}`),
            })}
      </span>
      <span className="num">{t('sessionlog.row.rpe', { rpe: shown.rpe })}</span>
    </span>
  )

  // logged is told by an icon and by text, not by colour alone
  const loggedMark = (
    <span className="text-foreground flex shrink-0 items-center gap-1 text-sm font-medium">
      <CheckIcon aria-hidden className="size-5" />
      {t('sessionlog.row.logged')}
    </span>
  )

  if (locked) {
    // nothing here takes focus; the dashed line is the locked look, and the page's message says it in words
    return (
      <li
        className={cn(
          'flex min-h-14 items-center gap-2 px-4 py-1',
          !logged && 'text-muted-foreground',
        )}
      >
        <span className="bg-muted/50 flex min-h-12 min-w-0 flex-1 items-center gap-3 rounded-md border border-dashed px-2">
          <span className="num w-6 shrink-0 text-sm font-medium">
            {set.number}
          </span>
          {shownValues}
        </span>
        {logged ? (
          loggedMark
        ) : (
          <span className="shrink-0 text-sm">
            {t('sessionlog.row.notLogged')}
          </span>
        )}
      </li>
    )
  }

  return (
    <li>
      <div
        className={cn(
          'flex min-h-14 items-center gap-2 px-4 py-1',
          !logged && 'text-muted-foreground',
        )}
      >
        <button
          type="button"
          aria-expanded={open}
          className="focus-visible:ring-ring/50 -mx-2 flex min-h-12 min-w-0 flex-1 items-center gap-3 rounded-md px-2 text-left outline-none focus-visible:ring-3"
          onClick={onToggle}
        >
          <span className="sr-only">
            {t(logged ? 'sessionlog.row.edit' : 'sessionlog.row.open', which)}
          </span>
          <span aria-hidden className="num w-6 shrink-0 text-sm font-medium">
            {set.number}
          </span>
          {shownValues}
        </button>
        {logged ? (
          loggedMark
        ) : (
          <Button
            type="button"
            variant="outline"
            className="size-12 shrink-0"
            disabled={log.isPending}
            aria-busy={log.isPending}
            aria-label={t('sessionlog.row.tick', which)}
            onClick={() => submit(valuesOf(set.target, unit))}
          >
            {log.isPending ? (
              <Spinner aria-hidden="true" />
            ) : (
              <CheckIcon aria-hidden />
            )}
          </Button>
        )}
      </div>
      {log.isError && (
        <p role="alert" className="text-destructive px-4 pb-2 text-sm">
          {t(errorMessageKey(log.error))}
        </p>
      )}
      {open && (
        <EntryPanel
          exercise={entry.exerciseName}
          number={set.number}
          measure={entry.measure}
          unit={unit}
          values={values}
          problems={tried ? problems : {}}
          logged={logged}
          pending={log.isPending}
          onChange={setEdited}
          onSubmit={() => submit(values)}
        />
      )}
    </li>
  )
}
