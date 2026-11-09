import { CircleAlertIcon } from 'lucide-react'
import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
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
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { useDialogFocus } from '@/hooks/use-dialog-focus'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useOnline } from '@/lib/online'
import { usePendingLogs, type SessionLog } from '../api/session-log'
import { useFinishSession } from '../api/session-summary'

/**
 * The logger's sticky footer: how many sets are logged and the way out. An open session offers "Finish session",
 * unavailable while no set is logged, while the client is offline, and while a set is not yet synced, and confirmed
 * first when planned sets are missing; a finished one offers its summary. A locked session offers nothing.
 */
export function FinishFooter({
  session,
  locked,
  onLocked,
}: {
  session: SessionLog
  locked: boolean
  onLocked: () => void
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const finish = useFinishSession(session.id)
  const focus = useDialogFocus()
  const reason = useId()
  const online = useOnline()
  const kept = usePendingLogs(session.id)
  const unsynced = kept.length
  const [confirming, setConfirming] = useState(false)

  const sets = session.entries.flatMap((entry) =>
    entry.sets.map((set) => ({ position: entry.position, set })),
  )
  // a set kept on the phone counts: the client did log it
  const logged = sets.filter(
    ({ position, set }) =>
      set.log !== undefined ||
      kept.some(
        (log) =>
          log.entryPosition === position &&
          log.setNumber === set.number &&
          log.state === 'saved',
      ),
  ).length
  const missing = sets.length - logged
  const finished = session.finishedAt !== undefined
  // one reason at a time, the one that stands in the way first; nothing is queued to finish later
  const why = !online
    ? 'sessionlog.sync.finishOffline'
    : unsynced > 0
      ? 'sessionlog.sync.finishWaiting'
      : logged === 0
        ? 'sessionlog.finish.nothingLogged'
        : undefined

  if (locked) return null

  const send = () => {
    setConfirming(false)
    finish.mutate(undefined, {
      onSuccess: () => void navigate(`/app/summary/${session.id}`),
      onError: (error) => {
        if (problemCode(error) === 'session-log.locked') onLocked()
      },
    })
  }

  return (
    <div className="bg-background flex flex-col gap-2 border-t pt-3 pb-3">
      <p className="text-muted-foreground text-center text-sm">
        {t('sessionlog.finish.count', { logged, count: sets.length })}
      </p>
      {finish.isError && problemCode(finish.error) !== 'session-log.locked' && (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>
            {t(
              problemCode(finish.error) === undefined
                ? 'sessionlog.finish.failed'
                : errorMessageKey(finish.error),
            )}
          </AlertDescription>
        </Alert>
      )}
      {finished ? (
        <Button variant="outline" size="touch" asChild>
          <Link to={`/app/summary/${session.id}`}>
            {t('sessionlog.finish.viewSummary')}
          </Link>
        </Button>
      ) : (
        <>
          <Button
            size="touch"
            className="bg-brand text-brand-foreground hover:bg-brand/80"
            disabled={why !== undefined || finish.isPending}
            aria-describedby={why === undefined ? undefined : reason}
            onClick={() => (missing > 0 ? setConfirming(true) : send())}
          >
            {t('sessionlog.finish.action')}
          </Button>
          {why !== undefined && (
            <p
              id={reason}
              className="text-muted-foreground text-center text-sm"
            >
              {t(why)}
            </p>
          )}
        </>
      )}
      <AlertDialog open={confirming} onOpenChange={setConfirming}>
        <AlertDialogContent {...focus.contentProps}>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('sessionlog.finish.confirm.title')}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t('sessionlog.finish.confirm.text', { count: missing })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>
              {t('sessionlog.finish.confirm.keep')}
            </AlertDialogCancel>
            <AlertDialogAction onClick={send}>
              {t('sessionlog.finish.action')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
