import { zodResolver } from '@hookform/resolvers/zod'
import { CircleCheckIcon, Link2OffIcon, MailIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Trans, useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useRequestPasswordReset, useSetPassword } from '../api/account'
import { AuthCard, AuthFooter, AuthLayout } from '../components/auth-layout'
import { PasswordInput } from '../components/password-input'
import { emailSchema, isValidPassword } from '../sign-up-schema'

const emailFormSchema = z.object({ email: emailSchema })
const passwordFormSchema = z.object({
  password: z.string().refine(isValidPassword, 'account.passwordRules'),
})

const LINK_INVALID = 'errors.account.password-reset-link-invalid'

// the token travels in the fragment, which never reaches a server log or Referer header
function tokenFromAddress() {
  return new URLSearchParams(window.location.hash.slice(1)).get('token')
}

/** One address for both halves: with a token in the link it sets the password, without one it asks for a link. */
export function ResetPasswordPage() {
  const [token] = useState(tokenFromAddress)

  useEffect(() => {
    window.history.replaceState(null, '', window.location.pathname)
  }, [])

  return token ? <SetPasswordState token={token} /> : <RequestState />
}

function RequestState({ expired = false }: { expired?: boolean }) {
  const { t } = useTranslation()
  const requestLink = useRequestPasswordReset()
  const [sentTo, setSentTo] = useState<string>()
  const form = useForm<
    z.input<typeof emailFormSchema>,
    unknown,
    z.output<typeof emailFormSchema>
  >({
    resolver: zodResolver(emailFormSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { email: '' },
  })
  const error = form.formState.errors.email
  const busy = requestLink.isPending

  const onSubmit = form.handleSubmit(({ email }) => {
    if (busy) return
    requestLink.mutate(email, { onSuccess: () => setSentTo(email) })
  })

  if (sentTo) {
    return (
      <AuthLayout>
        <AuthCard
          focusHeading
          icon={<MailIcon aria-hidden="true" />}
          title={t('account.reset.sent.title')}
          description={
            <Trans
              i18nKey="account.reset.sent.description"
              values={{ email: sentTo }}
              components={{
                strong: (
                  <strong className="text-foreground font-medium wrap-anywhere" />
                ),
              }}
            />
          }
        >
          <p className="text-muted-foreground text-center text-xs">
            {t('account.checkEmail.hint')}
          </p>
          <Button asChild size="touch" className="w-full">
            <Link to="/sign-in">{t('account.signIn')}</Link>
          </Button>
        </AuthCard>
        <AuthFooter>
          {t('account.checkEmail.wrongEmail')}{' '}
          <Button
            variant="link"
            className="text-foreground h-auto p-0 text-xs underline"
            onClick={() => setSentTo(undefined)}
          >
            {t('account.checkEmail.changeIt')}
          </Button>
        </AuthFooter>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <AuthCard
        focusHeading={expired}
        icon={expired ? <Link2OffIcon aria-hidden="true" /> : undefined}
        title={t(
          expired
            ? 'account.reset.expired.title'
            : 'account.reset.request.title',
        )}
        description={t(
          expired
            ? 'account.reset.expired.description'
            : 'account.reset.request.description',
        )}
      >
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <Field data-invalid={!!error}>
            <FieldLabel htmlFor="email">
              {t('account.signInPage.email')}
            </FieldLabel>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder={t('account.signUp.emailPlaceholder')}
              className="h-(--control-h-touch) text-base md:text-base"
              readOnly={busy}
              aria-invalid={!!error}
              aria-describedby={error ? 'email-error' : undefined}
              {...form.register('email')}
            />
            {error?.message && (
              <FieldError id="email-error">{t(error.message)}</FieldError>
            )}
          </Field>
          {requestLink.isError && (
            <Alert variant="destructive">
              <AlertDescription>
                {t(errorMessageKey(requestLink.error))}
              </AlertDescription>
            </Alert>
          )}
          <Button
            type="submit"
            size="touch"
            className="w-full"
            disabled={busy}
            aria-busy={busy}
          >
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('account.reset.request.submit')}
          </Button>
        </form>
      </AuthCard>
      <AuthFooter>
        {t('account.reset.remembered')}{' '}
        <Link to="/sign-in" className="text-foreground">
          {t('account.signIn')}
        </Link>
      </AuthFooter>
    </AuthLayout>
  )
}

function SetPasswordState({ token }: { token: string }) {
  const { t } = useTranslation()
  const setPassword = useSetPassword()
  const form = useForm<
    z.input<typeof passwordFormSchema>,
    unknown,
    z.output<typeof passwordFormSchema>
  >({
    resolver: zodResolver(passwordFormSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { password: '' },
  })
  const error = form.formState.errors.password
  const busy = setPassword.isPending

  const onSubmit = form.handleSubmit(({ password }) => {
    if (busy) return
    setPassword.mutate({ token, password })
  })

  if (setPassword.isSuccess) {
    return (
      <AuthLayout>
        <AuthCard
          focusHeading
          icon={<CircleCheckIcon aria-hidden="true" />}
          iconClassName="bg-success-muted text-success"
          title={t('account.reset.done.title')}
          description={t('account.reset.done.description')}
        >
          <Button asChild size="touch" className="w-full">
            <Link to="/sign-in">{t('account.signIn')}</Link>
          </Button>
        </AuthCard>
      </AuthLayout>
    )
  }

  if (
    setPassword.isError &&
    errorMessageKey(setPassword.error) === LINK_INVALID
  ) {
    return <RequestState expired />
  }

  return (
    <AuthLayout>
      <AuthCard
        title={t('account.reset.set.title')}
        description={t('account.reset.set.description')}
      >
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <Field data-invalid={!!error}>
            <FieldLabel htmlFor="password">
              {t('account.reset.set.password')}
            </FieldLabel>
            <PasswordInput
              id="password"
              autoComplete="new-password"
              readOnly={busy}
              aria-invalid={!!error}
              aria-describedby="password-rules"
              {...form.register('password')}
            />
            {error ? (
              <FieldError id="password-rules">
                {t('account.passwordRules')}
              </FieldError>
            ) : (
              <FieldDescription id="password-rules">
                {t('account.passwordRules')}
              </FieldDescription>
            )}
          </Field>
          {setPassword.isError && (
            <Alert variant="destructive">
              <AlertDescription>
                {t(errorMessageKey(setPassword.error))}
              </AlertDescription>
            </Alert>
          )}
          <Button
            type="submit"
            size="touch"
            className="w-full"
            disabled={busy}
            aria-busy={busy}
          >
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('account.reset.set.submit')}
          </Button>
        </form>
      </AuthCard>
    </AuthLayout>
  )
}
