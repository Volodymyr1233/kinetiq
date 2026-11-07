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
import { useDialogFocus } from '@/hooks/use-dialog-focus'

/**
 * Asks before a new automatic draft replaces the current one. The safe Cancel takes the focus first, and focus goes
 * back to the button that opened it, which stays on screen.
 */
export function BuildAgainDialog({
  open,
  clientName,
  onOpenChange,
  onAccept,
}: {
  open: boolean
  clientName: string
  onOpenChange: (open: boolean) => void
  onAccept: () => void
}) {
  const { t } = useTranslation()
  const focus = useDialogFocus()
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent {...focus.contentProps}>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {t('plan.rebuild.dialog.title', { name: clientName })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {t('plan.rebuild.dialog.body')}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('plan.cancel')}</AlertDialogCancel>
          <AlertDialogAction onClick={onAccept}>
            {t('plan.rebuild.dialog.accept')}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
