import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { errorMessageKey } from '@/lib/api/problem'
import { formatDay } from '@/lib/dates'
import {
  waitsForDecisionOf,
  type PlanChangeSide,
} from '@/lib/plan-change-state'
import type { DeclineReason } from '../api/client-plan-change'
import { KeepPlanForm } from './keep-plan-form'

type Decidable = {
  state: 'PENDING' | 'ACCEPTED_BY_ONE' | 'APPLIED' | 'DECLINED' | 'EXPIRED'
  effectiveWeek: string
  acceptedByTrainer: boolean
  acceptedByClient: boolean
  declinedBy?: PlanChangeSide
  declineReason?: DeclineReason
  declineNote?: string
}

/**
 * What a person can do with a plan change and what happened to it, by state: accept or keep the current plan while it
 * waits for them, undo after accepting, and the outcome once it ended. The client gets a bottom sheet for the reason and
 * 48px buttons; the trainer a dialog.
 */
export function PlanChangeDecisionPanel({
  planChange,
  viewer,
  otherName,
  planLink,
  busy,
  error,
  onAccept,
  onUndo,
  onKeep,
}: {
  planChange: Decidable
  viewer: PlanChangeSide
  otherName: string
  /** Where "See plan" goes once the plan change is applied. */
  planLink: string
  busy: boolean
  /** The last refusal or failure of a decision, shown until the next one succeeds. */
  error?: unknown
  onAccept: () => void
  onUndo: () => void
  /** Resolves when the plan change was declined, so the form can close; a refusal is passed as `error`. */
  onKeep: (reason: DeclineReason, note: string | undefined) => Promise<boolean>
}) {
  const { t, i18n } = useTranslation()
  const [keepOpen, setKeepOpen] = useState(false)
  const keepButton = useRef<HTMLButtonElement>(null)
  const outcome = useRef<HTMLDivElement>(null)
  // the sheet and the dialog have no trigger of their own, so focus goes back to the button that opened them; once the
  // plan change was declined that button is gone and focus goes to what replaced it
  const returnFocus = (event: Event) => {
    event.preventDefault()
    ;(keepButton.current ?? outcome.current)?.focus()
  }
  const touch = viewer === 'CLIENT'
  const size = touch ? 'touch' : 'default'
  const date = formatDay(planChange.effectiveWeek, i18n.language)
  const errorText = error === undefined ? undefined : t(errorMessageKey(error))
  const reason =
    planChange.declineReason === undefined
      ? ''
      : t(`planChange.reason.${planChange.declineReason}`)

  const form = (
    <KeepPlanForm
      touch={touch}
      busy={busy}
      error={errorText}
      onBack={() => setKeepOpen(false)}
      onSubmit={(chosen, note) =>
        void onKeep(chosen, note).then((declined) => {
          if (declined) setKeepOpen(false)
        })
      }
    />
  )
  const title = t('planChange.keep.title')
  const description = t('planChange.keep.description', { name: otherName })

  let body
  if (waitsForDecisionOf(planChange, viewer)) {
    const otherAccepted = planChange.state === 'ACCEPTED_BY_ONE'
    body = (
      <>
        {otherAccepted && (
          <p>{t('planChange.outcome.otherAccepted', { name: otherName })}</p>
        )}
        {/* one column on a phone: side by side, the labels do not fit at 375px */}
        <div className={touch ? 'flex flex-col gap-2' : 'flex gap-2'}>
          <Button
            ref={keepButton}
            variant="outline"
            size={size}
            disabled={busy}
            onClick={() => setKeepOpen(true)}
          >
            {t('planChange.actions.keep')}
          </Button>
          <Button size={size} disabled={busy} onClick={onAccept}>
            {t('planChange.actions.accept')}
          </Button>
        </div>
        <p className="text-muted-foreground text-sm">
          {touch
            ? t('planChange.client.effective', { date })
            : t('planChange.effective', { date, client: otherName })}
        </p>
      </>
    )
  } else if (planChange.state === 'ACCEPTED_BY_ONE') {
    // the viewer accepted and the other has not
    body = (
      <div className="bg-plan-change-muted flex items-center justify-between gap-3 rounded-lg px-4 py-3">
        <p>{t('planChange.outcome.waiting', { name: otherName })}</p>
        <Button variant="ghost" size="sm" disabled={busy} onClick={onUndo}>
          {t('planChange.actions.undo')}
        </Button>
      </div>
    )
  } else if (planChange.state === 'APPLIED') {
    body = (
      <div className="flex items-center justify-between gap-3">
        <p>{t('planChange.outcome.applied', { date })}</p>
        <Button variant="link" size="sm" asChild>
          <Link to={planLink}>{t('planChange.actions.seePlan')}</Link>
        </Button>
      </div>
    )
  } else if (planChange.state === 'DECLINED') {
    body = (
      <>
        <p>
          {planChange.declinedBy === viewer
            ? t(
                touch
                  ? 'planChange.outcome.keptByYouClient'
                  : 'planChange.outcome.keptByYouTrainer',
                { reason, name: otherName },
              )
            : t('planChange.outcome.keptByOther', { reason, name: otherName })}
        </p>
        {planChange.declineNote && (
          // plain text: the note is what a person typed
          <blockquote className="text-muted-foreground border-s-2 ps-3 text-sm whitespace-pre-wrap">
            {planChange.declineNote}
          </blockquote>
        )}
      </>
    )
  } else {
    body = <p>{t('planChange.outcome.expired')}</p>
  }

  const open = waitsForDecisionOf(planChange, viewer) && keepOpen
  return (
    <div className="flex flex-col gap-3">
      <div
        ref={outcome}
        tabIndex={-1}
        className="flex flex-col gap-3 outline-none"
      >
        {body}
      </div>
      <div aria-live="polite" className="text-destructive text-sm empty:hidden">
        {!open && errorText}
      </div>
      {touch ? (
        <Sheet open={open} onOpenChange={setKeepOpen}>
          <SheetContent
            side="bottom"
            className="gap-4 px-4 pb-6"
            onCloseAutoFocus={returnFocus}
          >
            <SheetHeader className="px-0">
              <SheetTitle>{title}</SheetTitle>
              <SheetDescription>{description}</SheetDescription>
            </SheetHeader>
            {form}
          </SheetContent>
        </Sheet>
      ) : (
        <Dialog open={open} onOpenChange={setKeepOpen}>
          <DialogContent onCloseAutoFocus={returnFocus}>
            <DialogHeader>
              <DialogTitle>{title}</DialogTitle>
              <DialogDescription>{description}</DialogDescription>
            </DialogHeader>
            {form}
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
}
