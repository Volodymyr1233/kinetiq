import { useQueryClient } from '@tanstack/react-query'
import { useState, type ComponentProps, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { accountDeletionQuery, useDeleteAccount } from '../api/account'
import { PasswordInput } from './password-input'

type DeleteAccountDialogProps = {
  /** What the deletion erases for this person's role. */
  description: string
  trigger?: Pick<
    ComponentProps<typeof Button>,
    'variant' | 'size' | 'className'
  >
}

function numberIn(error: unknown, property: string) {
  return typeof error === 'object' &&
    error !== null &&
    property in error &&
    typeof (error as Record<string, unknown>)[property] === 'number'
    ? ((error as Record<string, unknown>)[property] as number)
    : undefined
}

/**
 * "Delete account" and the confirmation it opens, which asks for the password. Radix starts focus on Cancel, the safe
 * choice, and returns it to the trigger; the dialog stays open while the request runs and after a refusal.
 */
export function DeleteAccountDialog({
  description,
  trigger,
}: DeleteAccountDialogProps) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const deletion = useDeleteAccount()
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const busy = deletion.isPending
  const code = problemCode(deletion.error)
  const wrongPassword = code === 'account.password-incorrect'

  function close(next: boolean) {
    if (busy) return
    setOpen(next)
    if (!next) {
      setPassword('')
      deletion.reset()
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    if (busy || password === '') return
    deletion.mutate(password, {
      onError: (error) => {
        // the count on the settings screen is stale: a client joined after it was read
        if (problemCode(error) === 'account.has-clients') {
          void queryClient.invalidateQueries(accountDeletionQuery)
        }
      },
    })
  }

  return (
    <AlertDialog open={open} onOpenChange={close}>
      <AlertDialogTrigger asChild>
        <Button variant="destructive" {...trigger}>
          {t('settings.delete.button')}
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <form noValidate onSubmit={submit} className="contents">
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('settings.delete.dialogTitle')}
            </AlertDialogTitle>
            <AlertDialogDescription>{description}</AlertDialogDescription>
          </AlertDialogHeader>
          <Field data-invalid={wrongPassword}>
            <FieldLabel htmlFor="delete-account-password">
              {t('settings.delete.password')}
            </FieldLabel>
            <PasswordInput
              id="delete-account-password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              readOnly={busy}
              aria-invalid={wrongPassword}
              aria-describedby="delete-account-password-hint"
            />
            {wrongPassword ? (
              <FieldError id="delete-account-password-hint">
                {t('errors.account.password-incorrect')}
              </FieldError>
            ) : (
              <FieldDescription id="delete-account-password-hint">
                {t('settings.delete.passwordHelp')}
              </FieldDescription>
            )}
          </Field>
          {deletion.isError && !wrongPassword && (
            <RefusalAlert error={deletion.error} />
          )}
          <AlertDialogFooter>
            <AlertDialogCancel type="button" disabled={busy}>
              {t('settings.delete.cancel')}
            </AlertDialogCancel>
            {/* not AlertDialogAction, which would close before the request ends */}
            <Button
              type="submit"
              variant="destructive"
              disabled={busy || password === ''}
              aria-busy={busy}
            >
              {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
              {t('settings.delete.confirm')}
            </Button>
          </AlertDialogFooter>
        </form>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function RefusalAlert({ error }: { error: unknown }) {
  const { t } = useTranslation()

  switch (problemCode(error)) {
    case 'account.sign-in-locked': {
      // the answer carries the seconds left; whole minutes, rounded up, as on the sign-in page
      const seconds = numberIn(error, 'retryAfterSeconds') ?? 15 * 60
      return (
        <Alert variant="destructive">
          <AlertTitle>{t('account.signInPage.lockedTitle')}</AlertTitle>
          <AlertDescription>
            {t('account.signInPage.lockedBody', {
              count: Math.max(1, Math.ceil(seconds / 60)),
            })}
          </AlertDescription>
        </Alert>
      )
    }
    case 'account.has-clients':
      return (
        <Alert variant="destructive">
          <AlertDescription>
            {t('errors.account.has-clients', {
              count: numberIn(error, 'clients') ?? 1,
            })}
          </AlertDescription>
        </Alert>
      )
    default:
      return (
        <Alert variant="destructive">
          <AlertDescription>{t(errorMessageKey(error))}</AlertDescription>
        </Alert>
      )
  }
}
