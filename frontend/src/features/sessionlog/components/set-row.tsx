import { onlineManager } from '@tanstack/react-query'
import { CheckIcon } from 'lucide-react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { ValueList } from '@/components/value-list'
import { cn } from '@/lib/utils'
import { fromKg, useWeightUnit } from '@/lib/weight'
import {
  useLogSet,
  usePendingLog,
  type LoggedEntry,
  type PlannedSet,
  type SyncState,
} from '../api/session-log'
import {
  bodyOf,
  problemsOf,
  sameValues,
  valuesOf,
  type Values,
} from '../set-values'
import { EntryPanel } from './entry-panel'
import { SyncBadge } from './sync-badge'

/**
 * One planned set: its targets in grey until it is logged, and what the client did afterwards. A tick logs the set as
 * planned; choosing the row opens the entry panel to log other values or to change a logged set. A logged set says
 * where it stands: Saved on this phone, Synced, or Not synced with its reason, Retry and Discard. A locked row only
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
}: {
  sessionId: string
  entry: LoggedEntry
  set: PlannedSet
  open: boolean
  locked: boolean
  onToggle: () => void
  // told when a set is logged for the first time, so the rest after it can start; a change does not start one
  onLogged: (restSeconds: number) => void
}) {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const pending = usePendingLog(sessionId, entry.position, set.number)
  // made once, so a send that is repeated after a failure carries the same id and stores the set once
  const [ownId] = useState(() => crypto.randomUUID())
  // a send kept on the phone has an id already, which a reload must not replace
  const setLogId = pending?.setLogId ?? set.log?.id ?? ownId
  const log = useLogSet(sessionId, entry.position, set.number, setLogId)
  const [edited, setEdited] = useState<Values | null>(null)
  const [tried, setTried] = useState(false)
  const toggle = useRef<HTMLButtonElement>(null)

  // what the client logged last: kept on the phone, or stored by the server, which is the later word once it has it
  const current =
    pending && pending.state !== 'synced'
      ? pending.body
      : (set.log ?? pending?.body)
  const state: SyncState | undefined =
    pending?.state ?? (set.log === undefined ? undefined : 'synced')
  const logged = state !== undefined
  // a failed send keeps what was entered, but the set does not count as logged until a send is kept or stored
  const counted = set.log !== undefined || (logged && state !== 'not-synced')
  const shown = current ?? set.target
  const values = edited ?? valuesOf(shown, unit)
  const problems = problemsOf(values, unit)
  // a logged set that still holds what was logged has nothing to update
  const unchanged =
    state !== 'not-synced' &&
    current !== undefined &&
    Object.keys(problems).length === 0 &&
    sameValues(values, valuesOf(current, unit), unit)
  const measure = t(`plan.entries.measures.${entry.measure}`)
  const which = { exercise: entry.exerciseName, number: set.number }

  const submit = (toLog: Values) => {
    setTried(true)
    if (Object.keys(problemsOf(toLog, unit)).length > 0) return
    // a change replaces a failed send instead of queueing behind it
    if (pending?.state === 'not-synced') pending.discard()
    const done = () => {
      setEdited(null)
      setTried(false)
      if (open) onToggle()
      if (!counted) onLogged(entry.restSeconds)
    }
    log.mutate(
      {
        sessionId,
        setLogId,
        body: bodyOf(toLog, unit, {
          entryPosition: entry.position,
          setNumber: set.number,
        }),
      },
      { onSuccess: done },
    )
    // with no connection the send waits and the phone keeps it: the row is logged now, not when it is sent
    if (!onlineManager.isOnline()) done()
  }

  // Retry and Discard disappear when they are used, so the focus goes back to the row's own button
  const settle = (act: () => void) => () => {
    act()
    toggle.current?.focus()
  }

  const shownValues = (
    <ValueList className="min-w-0 flex-1">
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
    </ValueList>
  )

  // a polite status, so a change from Saved on this phone to Synced or Not synced is said without moving focus
  const mark = (
    <span role="status" className="flex max-w-32 shrink-0 justify-end">
      {state && <SyncBadge state={state} />}
    </span>
  )

  const notSynced = pending?.state === 'not-synced' && (
    <div className="flex flex-col gap-2 px-4 pb-3">
      <p className="text-sm">
        {t(`sessionlog.sync.reason.${pending.reason ?? 'unreachable'}`)}
      </p>
      <div className="flex flex-wrap gap-2">
        {/* a locked session can no longer take the set, so only the kept copy can go */}
        {!locked && (
          <Button
            size="touch"
            aria-label={t('sessionlog.sync.retryLabel', which)}
            onClick={settle(pending.retry)}
          >
            {t('sessionlog.sync.retry')}
          </Button>
        )}
        <Button
          variant="outline"
          size="touch"
          aria-label={t('sessionlog.sync.discardLabel', which)}
          onClick={settle(() => {
            pending.discard()
            setEdited(null)
          })}
        >
          {t('sessionlog.sync.discard')}
        </Button>
      </div>
    </div>
  )

  if (locked) {
    // nothing here takes focus; the dashed line is the locked look, and the page's message says it in words. A log still
    // kept on the phone shows where it stands, and can be discarded, but not retried: the session can no longer change
    return (
      <li>
        <div
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
          {pending && pending.state !== 'synced' ? (
            mark
          ) : logged ? (
            // logged is told by an icon and by text, not by colour alone
            <span className="text-foreground flex shrink-0 items-center gap-1 text-sm font-medium">
              <CheckIcon aria-hidden className="size-5" />
              {t('sessionlog.row.logged')}
            </span>
          ) : (
            <span className="shrink-0 text-sm">
              {t('sessionlog.row.notLogged')}
            </span>
          )}
        </div>
        {notSynced}
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
          ref={toggle}
          type="button"
          aria-expanded={open}
          className="focus-visible:ring-ring/50 -mx-2 flex min-h-12 min-w-0 flex-1 items-center gap-3 rounded-md px-2 text-left outline-none focus-visible:ring-3"
          onClick={onToggle}
        >
          <span className="sr-only">
            {t(counted ? 'sessionlog.row.edit' : 'sessionlog.row.open', which)}
          </span>
          <span aria-hidden className="num w-6 shrink-0 text-sm font-medium">
            {set.number}
          </span>
          {shownValues}
        </button>
        {logged ? (
          mark
        ) : (
          <Button
            type="button"
            variant="outline"
            className="size-12 shrink-0"
            aria-label={t('sessionlog.row.tick', which)}
            onClick={() => submit(valuesOf(shown, unit))}
          >
            <CheckIcon aria-hidden />
          </Button>
        )}
      </div>
      {notSynced}
      {open && (
        <EntryPanel
          exercise={entry.exerciseName}
          number={set.number}
          measure={entry.measure}
          unit={unit}
          values={values}
          problems={tried ? problems : {}}
          logged={counted}
          unchanged={unchanged}
          // a send that waits for a connection is kept, so the next change can be made
          pending={log.isPending && !log.isPaused}
          onChange={setEdited}
          onSubmit={() => submit(values)}
        />
      )}
    </li>
  )
}
