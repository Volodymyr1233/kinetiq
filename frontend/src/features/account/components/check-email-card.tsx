import { MailIcon } from 'lucide-react'
import { Trans, useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useResendLink } from '../use-resend-link'
import { AuthCard, AuthFooter } from './auth-layout'

type CheckEmailCardProps = {
  email: string
  onChangeEmail: () => void
}

export function CheckEmailCard({ email, onChangeEmail }: CheckEmailCardProps) {
  const { t } = useTranslation()
  const { request: resend, locked, send } = useResendLink()

  return (
    <>
      <AuthCard
        focusHeading
        icon={<MailIcon aria-hidden="true" />}
        title={t('account.checkEmail.title')}
        description={
          <Trans
            i18nKey="account.checkEmail.description"
            values={{ email }}
            components={{
              strong: (
                <strong className="text-foreground font-medium wrap-anywhere" />
              ),
            }}
          />
        }
      >
        {resend.isError && (
          <Alert variant="destructive">
            <AlertDescription>
              {t(errorMessageKey(resend.error))}
            </AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2.5">
          <Button
            variant="outline"
            size="touch"
            className="w-full"
            disabled={resend.isPending || locked}
            aria-busy={resend.isPending}
            onClick={() => send(email)}
          >
            {resend.isPending && (
              <Spinner data-icon="inline-start" aria-hidden="true" />
            )}
            {t('account.checkEmail.resend')}
          </Button>
          {/* polite and always present, so the change to "sent again" is announced once */}
          <p
            aria-live="polite"
            className="text-muted-foreground text-center text-xs"
          >
            {resend.isSuccess
              ? t('account.checkEmail.resent')
              : t('account.checkEmail.hint')}
          </p>
        </div>
      </AuthCard>
      <AuthFooter>
        {t('account.checkEmail.wrongEmail')}{' '}
        <Button
          variant="link"
          className="text-foreground h-auto p-0 text-xs underline"
          onClick={onChangeEmail}
        >
          {t('account.checkEmail.changeIt')}
        </Button>
      </AuthFooter>
    </>
  )
}
