import { LogOutIcon } from 'lucide-react'
import type { ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useSignOut } from '../api/account'

type SignOutButtonProps = Pick<
  ComponentProps<typeof Button>,
  'variant' | 'size' | 'className'
>

/** Signs out and lands on sign-in; when the server cannot be reached the person stays and sees why. */
export function SignOutButton(props: SignOutButtonProps) {
  const { t } = useTranslation()
  const signOut = useSignOut()

  return (
    <>
      {signOut.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {t(errorMessageKey(signOut.error))}
          </AlertDescription>
        </Alert>
      )}
      <Button
        {...props}
        disabled={signOut.isPending}
        aria-busy={signOut.isPending}
        onClick={() => signOut.mutate()}
      >
        {signOut.isPending ? (
          <Spinner data-icon="inline-start" aria-hidden="true" />
        ) : (
          <LogOutIcon data-icon="inline-start" aria-hidden="true" />
        )}
        {t('account.signOut')}
      </Button>
    </>
  )
}
