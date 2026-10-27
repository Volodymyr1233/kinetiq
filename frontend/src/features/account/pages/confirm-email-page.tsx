import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { CircleCheckIcon, Link2OffIcon } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import {
  currentAccountQuery,
  useConfirmEmail,
  useRequestConfirmationLink,
} from '../api/account'
import { AuthCard, AuthFooter, AuthLayout } from '../components/auth-layout'
import { CheckEmailCard } from '../components/check-email-card'
import { landingFor } from '../landing'
import { emailSchema } from '../sign-up-schema'

const newLinkSchema = z.object({ email: emailSchema })

// the token travels in the fragment, which never reaches a server log or Referer header
function tokenFromAddress() {
  return new URLSearchParams(window.location.hash.slice(1)).get('token')
}

export function ConfirmEmailPage() {
  const { t } = useTranslation()
  const [token] = useState(tokenFromAddress)
  const confirm = useConfirmEmail()
  const queryClient = useQueryClient()
  const started = useRef(false)

  useEffect(() => {
    // StrictMode runs effects twice; a second confirmation would report a used link
    if (started.current) return
    started.current = true
    window.history.replaceState(null, '', window.location.pathname)
    if (token) {
      confirm.mutate(token, {
        // an open client app in this browser drops its banner
        onSuccess: () =>
          void queryClient.invalidateQueries({
            queryKey: currentAccountQuery.queryKey,
          }),
      })
    }
  }, [token, confirm, queryClient])

  if (!token || confirm.isError) {
    return <NewLinkState failure={confirm.error} />
  }

  if (confirm.isSuccess) return <ConfirmedState />

  return (
    <AuthLayout>
      <AuthCard
        icon={<Spinner aria-hidden="true" className="size-5" />}
        title={t('account.confirming.title')}
      />
      <p role="status" className="sr-only">
        {t('account.confirming.title')}
      </p>
    </AuthLayout>
  )
}

function ConfirmedState() {
  const { t } = useTranslation()
  // confirming does not sign anyone in; a person already signed in here continues to their area
  const { data: account } = useQuery(currentAccountQuery)

  return (
    <AuthLayout>
      <AuthCard
        focusHeading
        icon={<CircleCheckIcon aria-hidden="true" />}
        iconClassName="bg-success-muted text-success"
        title={t('account.confirmed.title')}
        description={t('account.confirmed.description')}
      >
        <Button asChild size="touch" className="w-full">
          {account ? (
            <Link to={landingFor(account.role)}>
              {t('account.confirmed.continue')}
            </Link>
          ) : (
            <Link to="/sign-in">{t('account.signIn')}</Link>
          )}
        </Button>
      </AuthCard>
    </AuthLayout>
  )
}

function NewLinkState({ failure }: { failure: unknown }) {
  const { t } = useTranslation()
  const requestLink = useRequestConfirmationLink()
  const [sentTo, setSentTo] = useState<string>()
  const form = useForm<
    z.input<typeof newLinkSchema>,
    unknown,
    z.output<typeof newLinkSchema>
  >({
    resolver: zodResolver(newLinkSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { email: '' },
  })
  const error = form.formState.errors.email
  const busy = requestLink.isPending
  // a failed connection is not an expired link: say so, but still offer a new one
  const otherFailure =
    failure !== null &&
    failure !== undefined &&
    errorMessageKey(failure) !== 'errors.account.confirmation-link-invalid'
      ? errorMessageKey(failure)
      : undefined

  const onSubmit = form.handleSubmit(({ email }) => {
    if (busy) return
    requestLink.mutate(email, { onSuccess: () => setSentTo(email) })
  })

  if (sentTo) {
    return (
      <AuthLayout>
        <CheckEmailCard
          email={sentTo}
          onChangeEmail={() => setSentTo(undefined)}
        />
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <AuthCard
        focusHeading
        icon={<Link2OffIcon aria-hidden="true" />}
        title={t('account.expired.title')}
        description={t('account.expired.description')}
      >
        {otherFailure && (
          <Alert variant="destructive">
            <AlertDescription>{t(otherFailure)}</AlertDescription>
          </Alert>
        )}
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <Field data-invalid={!!error}>
            <FieldLabel htmlFor="email">
              {t('account.expired.email')}
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
            {t('account.expired.submit')}
          </Button>
        </form>
      </AuthCard>
      <AuthFooter>
        {t('account.expired.alreadyConfirmed')}{' '}
        <Link to="/sign-in" className="text-foreground">
          {t('account.signIn')}
        </Link>
      </AuthFooter>
    </AuthLayout>
  )
}
