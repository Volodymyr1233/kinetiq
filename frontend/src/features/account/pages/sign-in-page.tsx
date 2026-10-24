import { zodResolver } from '@hookform/resolvers/zod'
import { CircleAlertIcon, LockIcon, MailIcon } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Trans, useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import type { z } from 'zod'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useSignIn, type CurrentAccount } from '../api/account'
import { AuthCard, AuthFooter, AuthLayout } from '../components/auth-layout'
import { PasswordInput } from '../components/password-input'
import { signInSchema, type SignInValues } from '../sign-in-schema'
import { useResendLink } from '../use-resend-link'

type Notice =
  | { kind: 'wrong' }
  | { kind: 'locked'; minutes: number }
  | { kind: 'unconfirmed'; email: string }
  | { kind: 'failure'; messageKey: string }

const FIELDS = ['email', 'password'] as const

const touchInput = 'h-(--control-h-touch) text-base md:text-base'

function codeOf(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error
    ? error.code
    : undefined
}

// the locked answer carries the seconds left; the page shows whole minutes, rounded up
function minutesLeft(error: unknown) {
  const seconds =
    typeof error === 'object' &&
    error !== null &&
    'retryAfterSeconds' in error &&
    typeof error.retryAfterSeconds === 'number'
      ? error.retryAfterSeconds
      : 15 * 60
  return Math.max(1, Math.ceil(seconds / 60))
}

function serverFieldErrors(error: unknown) {
  const errors =
    typeof error === 'object' && error !== null && 'errors' in error
      ? error.errors
      : undefined
  if (!Array.isArray(errors)) return []
  return errors
    .map((fieldError) => fieldError?.field)
    .filter((field): field is (typeof FIELDS)[number] => FIELDS.includes(field))
}

function noticeFor(error: unknown, email: string): Notice {
  switch (codeOf(error)) {
    case 'account.credentials-invalid':
      return { kind: 'wrong' }
    case 'account.sign-in-locked':
      return { kind: 'locked', minutes: minutesLeft(error) }
    case 'account.email-not-confirmed':
      return { kind: 'unconfirmed', email: email.trim() }
    default:
      return { kind: 'failure', messageKey: errorMessageKey(error) }
  }
}

function landingFor(account: CurrentAccount) {
  return account.role === 'TRAINER' ? '/trainer' : '/app'
}

export function SignInPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const signIn = useSignIn()
  const [notice, setNotice] = useState<Notice>()
  const form = useForm<SignInValues, unknown, z.output<typeof signInSchema>>({
    resolver: zodResolver(signInSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { email: '', password: '' },
  })
  const { errors } = form.formState
  const busy = signIn.isPending

  const onSubmit = form.handleSubmit((values) => {
    if (busy) return
    setNotice(undefined)
    signIn.mutate(values, {
      onSuccess: (account) => void navigate(landingFor(account)),
      onError: (error) => {
        const fields = serverFieldErrors(error)
        if (fields.length > 0) {
          fields.forEach((field, index) =>
            form.setError(
              field,
              {
                type: 'server',
                message:
                  field === 'email'
                    ? 'account.errors.signInEmail'
                    : 'account.errors.signInPassword',
              },
              { shouldFocus: index === 0 },
            ),
          )
          return
        }
        setNotice(noticeFor(error, values.email))
        // the person types the password again; the alert is announced on its own
        form.resetField('password')
        form.setFocus('password')
      },
    })
  })

  return (
    <AuthLayout>
      <AuthCard
        title={t('account.signInPage.title')}
        description={t('account.signInPage.description')}
      >
        {notice && <SignInNotice notice={notice} />}

        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <FieldGroup className="gap-4">
            <Field data-invalid={!!errors.email}>
              <FieldLabel htmlFor="email">
                {t('account.signInPage.email')}
              </FieldLabel>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder={t('account.signInPage.emailPlaceholder')}
                className={touchInput}
                readOnly={busy}
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? 'email-error' : undefined}
                {...form.register('email')}
              />
              {errors.email?.message && (
                <FieldError id="email-error">
                  {t(errors.email.message)}
                </FieldError>
              )}
            </Field>

            <Field data-invalid={!!errors.password}>
              <div className="flex items-center justify-between gap-2">
                <FieldLabel htmlFor="password">
                  {t('account.signInPage.password')}
                </FieldLabel>
                <Button
                  asChild
                  variant="link"
                  size="sm"
                  className="h-auto px-0 font-normal"
                >
                  <Link to="/reset-password">
                    {t('account.signInPage.forgotPassword')}
                  </Link>
                </Button>
              </div>
              <PasswordInput
                id="password"
                autoComplete="current-password"
                readOnly={busy}
                aria-invalid={!!errors.password}
                aria-describedby={
                  errors.password ? 'password-error' : undefined
                }
                {...form.register('password')}
              />
              {errors.password?.message && (
                <FieldError id="password-error">
                  {t(errors.password.message)}
                </FieldError>
              )}
            </Field>
          </FieldGroup>

          <Button
            type="submit"
            size="touch"
            className="w-full"
            disabled={busy}
            aria-busy={busy}
          >
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('account.signInPage.submit')}
          </Button>
        </form>
      </AuthCard>
      <AuthFooter>
        <Trans
          i18nKey="account.signInPage.newTrainer"
          components={{
            signUp: <Link to="/sign-up" className="text-foreground" />,
          }}
        />
      </AuthFooter>
    </AuthLayout>
  )
}

function SignInNotice({ notice }: { notice: Notice }) {
  const { t } = useTranslation()

  switch (notice.kind) {
    case 'wrong':
      return (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden="true" />
          <AlertTitle>{t('account.signInPage.wrongTitle')}</AlertTitle>
          <AlertDescription>
            {t('account.signInPage.wrongBody')}
          </AlertDescription>
        </Alert>
      )
    case 'locked':
      return (
        <Alert variant="destructive">
          <LockIcon aria-hidden="true" />
          <AlertTitle>{t('account.signInPage.lockedTitle')}</AlertTitle>
          <AlertDescription>
            {t('account.signInPage.lockedBody', { count: notice.minutes })}
          </AlertDescription>
        </Alert>
      )
    case 'unconfirmed':
      return <UnconfirmedNotice email={notice.email} />
    case 'failure':
      return (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden="true" />
          <AlertDescription>{t(notice.messageKey)}</AlertDescription>
        </Alert>
      )
  }
}

function UnconfirmedNotice({ email }: { email: string }) {
  const { t } = useTranslation()
  const { request, locked, send } = useResendLink()

  return (
    <Alert variant="info">
      <MailIcon aria-hidden="true" />
      <AlertTitle>{t('account.signInPage.unconfirmedTitle')}</AlertTitle>
      <AlertDescription className="flex flex-col items-start gap-2">
        <span>
          <Trans
            i18nKey="account.signInPage.unconfirmedBody"
            values={{ email }}
            components={{
              strong: <strong className="font-medium wrap-anywhere" />,
            }}
          />
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={request.isPending || locked}
          aria-busy={request.isPending}
          onClick={() => send(email)}
        >
          {request.isPending && (
            <Spinner data-icon="inline-start" aria-hidden="true" />
          )}
          {t('account.signInPage.resend')}
        </Button>
        {/* polite and always present, so "sent again" or a failure is announced once */}
        <span aria-live="polite" className="text-xs">
          {request.isSuccess && t('account.signInPage.resent')}
          {request.isError && t(errorMessageKey(request.error))}
        </span>
      </AlertDescription>
    </Alert>
  )
}
