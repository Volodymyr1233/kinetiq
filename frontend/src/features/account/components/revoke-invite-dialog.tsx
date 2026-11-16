import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { useDialogFocus } from '@/hooks/use-dialog-focus'
import type { Invite } from '../api/invites'

/** Asks before revoking; stays open while the request runs. Radix starts focus on Cancel, the safe choice. */
export function RevokeInviteDialog({
  invite,
  busy,
  onConfirm,
  onCancel,
  onRevoked,
}: {
  invite?: Invite
  busy: boolean
  onConfirm: (invite: Invite) => void
  onCancel: () => void
  /** Where focus goes once the invite is gone, since its Revoke button went with it. */
  onRevoked: () => void
}) {
  const { t } = useTranslation()
  const focus = useDialogFocus()
  // kept while the dialog fades out, so its title does not lose the email
  const [shown, setShown] = useState(invite)
  if (invite && invite !== shown) setShown(invite)

  return (
    <AlertDialog
      open={!!invite}
      onOpenChange={(open) => {
        if (!open && !busy) onCancel()
      }}
    >
      <AlertDialogContent {...focus.contentProps}>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {t('invites.revokeDialog.title', { email: shown?.email })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {t('invites.revokeDialog.description')}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={busy}>
            {t('invites.revokeDialog.cancel')}
          </AlertDialogCancel>
          {/* not AlertDialogAction, which would close before the request ends */}
          <Button
            variant="destructive"
            disabled={busy}
            aria-busy={busy}
            onClick={() => {
              if (!invite) return
              // the dialog cannot close while the request runs, so its next close follows the request
              focus.afterClosing(onRevoked)
              onConfirm(invite)
            }}
          >
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('invites.revokeDialog.confirm')}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
