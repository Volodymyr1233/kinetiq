import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useBlocker } from 'react-router'
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
import { setLeaveGuard } from '@/lib/leave-guard'
import { useDialogFocus } from '../use-dialog-focus'

/** Asks before unsaved work is lost: on navigation in the app, on signing out, and on reload or close. */
export function LeaveGuard({ dirty }: { dirty: boolean }) {
  const { t } = useTranslation()
  // signing out asks through the guard below, and then lands on the sign-in page
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty &&
      nextLocation.pathname !== '/sign-in' &&
      nextLocation.pathname !== currentLocation.pathname,
  )
  const [signingOut, setSigningOut] = useState(false)
  const answer = useRef<(leave: boolean) => void>(undefined)
  const focus = useDialogFocus()

  useEffect(() => {
    if (!dirty) return undefined
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    const release = setLeaveGuard(
      () =>
        new Promise((resolve) => {
          answer.current = resolve
          setSigningOut(true)
        }),
    )
    return () => {
      window.removeEventListener('beforeunload', warn)
      release()
    }
  }, [dirty])

  function decide(leave: boolean) {
    setSigningOut(false)
    answer.current?.(leave)
    answer.current = undefined
    if (blocker.state === 'blocked') {
      if (leave) blocker.proceed()
      else blocker.reset()
    }
  }

  return (
    <AlertDialog
      open={blocker.state === 'blocked' || signingOut}
      onOpenChange={(open) => {
        if (!open) decide(false)
      }}
    >
      <AlertDialogContent {...focus.contentProps}>
        <AlertDialogHeader>
          <AlertDialogTitle>{t('plan.leave.title')}</AlertDialogTitle>
          <AlertDialogDescription>
            {t('plan.leave.body')}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('plan.leave.stay')}</AlertDialogCancel>
          <AlertDialogAction variant="outline" onClick={() => decide(true)}>
            {t('plan.leave.leave')}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
