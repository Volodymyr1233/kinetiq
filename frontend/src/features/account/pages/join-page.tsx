import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { CircleAlertIcon, Link2OffIcon } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Controller, useForm, type FieldPath } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import type { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { inviteCheckQuery, useJoin, type CurrentAccount } from '../api/account'
import { AccountAvatar } from '../components/account-avatar'
import { AuthCard, AuthFooter, AuthLayout } from '../components/auth-layout'
import { PasswordInput } from '../components/password-input'
import { serverFieldErrors, serverMessage } from '../field-errors'
import { joinSchema, type JoinValues } from '../sign-up-schema'
import { browserTimeZone } from '../time-zone'

const FIELDS: FieldPath<JoinValues>[] = [
  'name',
  'password',
  'atLeastSixteen',
  'healthDataConsent',
]

const LINK_INVALID = 'invite.link-invalid'

const touchInput = 'h-(--control-h-touch) text-base md:text-base'

// the secret stays in the fragment, which never reaches a server log; opening the link changes nothing, so a reload
// may show the form again
function tokenFromAddress() {
  return new URLSearchParams(window.location.hash.slice(1)).get('token')
}

function firstName(name: string) {
  return name.trim().split(/\s+/)[0] ?? ''
}

export function JoinPage() {
  const [token] = useState(tokenFromAddress)
  const check = useQuery({ ...inviteCheckQuery(token ?? ''), enabled: !!token })
  const join = useJoin()

  if (join.isSuccess) {
    return (
      <JoinedState
        account={join.data}
        trainerName={check.data?.trainerName ?? ''}
      />
    )
  }
  if (
    !token ||
    problemCode(check.error) === LINK_INVALID ||
    problemCode(join.error) === LINK_INVALID
  ) {
    return <LinkInvalidState />
  }
  if (check.isPending) return <CheckingState />
  if (!check.data) {
    return (
      <CheckFailedState
        error={check.error}
        retrying={check.isFetching}
        onRetry={() => void check.refetch()}
      />
    )
  }
  return (
    <JoinForm
      token={token}
      email={check.data.email}
      trainerName={check.data.trainerName}
      join={join}
    />
  )
}

function CheckingState() {
  const { t } = useTranslation()
  return (
    <AuthLayout>
      <AuthCard
        icon={<Spinner aria-hidden="true" className="size-5" />}
        title={t('account.join.checking')}
      />
      <p role="status" className="sr-only">
        {t('account.join.checking')}
      </p>
    </AuthLayout>
  )
}

function CheckFailedState({
  error,
  retrying,
  onRetry,
}: {
  error: unknown
  retrying: boolean
  onRetry: () => void
}) {
  const { t } = useTranslation()
  return (
    <AuthLayout>
      <Alert variant="destructive">
        <CircleAlertIcon aria-hidden="true" />
        <AlertDescription>{t(errorMessageKey(error))}</AlertDescription>
      </Alert>
      <Button
        size="touch"
        disabled={retrying}
        aria-busy={retrying}
        onClick={onRetry}
      >
        {retrying && <Spinner data-icon="inline-start" aria-hidden="true" />}
        {t('account.join.retry')}
      </Button>
    </AuthLayout>
  )
}

function LinkInvalidState() {
  const { t } = useTranslation()
  return (
    <AuthLayout>
      <AuthCard
        key="invalid"
        focusHeading
        icon={<Link2OffIcon aria-hidden="true" />}
        title={t('account.join.invalidTitle')}
        description={t('account.join.invalidDescription')}
      >
        <div className="flex flex-col gap-2">
          {/* sign-in sends a signed-in person on to their own area */}
          <Button asChild variant="outline" size="touch" className="w-full">
            <Link to="/sign-in">{t('account.join.signInInstead')}</Link>
          </Button>
          <p className="text-muted-foreground text-center text-xs">
            {t('account.join.invalidNote')}
          </p>
        </div>
      </AuthCard>
    </AuthLayout>
  )
}

function JoinedState({
  account,
  trainerName,
}: {
  account: CurrentAccount
  trainerName: string
}) {
  const { t } = useTranslation()
  return (
    <AuthLayout>
      <AuthCard
        key="joined"
        focusHeading
        icon={<AccountAvatar name={trainerName} size="xl" />}
        iconClassName="size-auto place-items-start bg-transparent"
        title={t('account.join.joinedTitle', {
          clientFirst: firstName(account.name),
        })}
        description={t('account.join.joinedDescription', {
          trainerFirst: firstName(trainerName),
        })}
      >
        <Button
          asChild
          size="touch"
          className="bg-brand text-brand-foreground hover:bg-brand/80 w-full"
        >
          <Link to="/app/quiz">{t('account.join.startQuiz')}</Link>
        </Button>
      </AuthCard>
    </AuthLayout>
  )
}

type JoinFormProps = {
  token: string
  email: string
  trainerName: string
  join: ReturnType<typeof useJoin>
}

function JoinForm({ token, email, trainerName, join }: JoinFormProps) {
  const { t } = useTranslation()
  const [failure, setFailure] = useState<string>()
  const form = useForm<JoinValues, unknown, z.output<typeof joinSchema>>({
    resolver: zodResolver(joinSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: {
      name: '',
      password: '',
      atLeastSixteen: false,
      healthDataConsent: false,
    },
  })
  const { errors } = form.formState
  const busy = join.isPending
  const trainerFirst = firstName(trainerName)

  const onSubmit = form.handleSubmit((values) => {
    if (busy) return
    setFailure(undefined)
    join.mutate(
      // the zone is refreshed from the browser at each sign-in, so UTC only lasts until then
      { ...values, token, timeZone: browserTimeZone() ?? 'UTC' },
      {
        onError: (error) => {
          // a link that stopped working replaces the whole form
          if (problemCode(error) === LINK_INVALID) return
          const fieldErrors = serverFieldErrors(error, FIELDS)
          fieldErrors.forEach(({ field, code }, index) =>
            form.setError(
              field,
              { type: 'server', message: serverMessage(field, code) },
              { shouldFocus: index === 0 },
            ),
          )
          if (fieldErrors.length === 0) {
            const key = errorMessageKey(error)
            setFailure(
              key === 'errors.validation.failed' ? 'errors.generic' : key,
            )
          }
        },
      },
    )
  })

  const box = (
    name: 'atLeastSixteen' | 'healthDataConsent',
    label: string,
    after?: ReactNode,
  ) => (
    <Controller
      control={form.control}
      name={name}
      render={({ field, fieldState }) => (
        <Field orientation="horizontal" data-invalid={fieldState.invalid}>
          {/* centred on the label's first line, which sits in the middle of its 44px row */}
          <div className="flex h-11 shrink-0 items-center">
            <Checkbox
              id={name}
              ref={field.ref}
              name={field.name}
              checked={field.value}
              onCheckedChange={(checked) => field.onChange(checked === true)}
              onBlur={field.onBlur}
              disabled={busy}
              aria-invalid={fieldState.invalid}
              aria-describedby={
                fieldState.invalid ? `${name}-error` : undefined
              }
            />
          </div>
          <FieldContent className="gap-0">
            {/* the whole label is the touch target, at least 44px tall */}
            <FieldLabel
              htmlFor={name}
              className="block w-full py-3 leading-5 font-normal"
            >
              {label}
            </FieldLabel>
            {after}
            {/* the message moves up into the label's padding, and clicks there still reach the label */}
            {fieldState.error?.message && (
              <FieldError
                id={`${name}-error`}
                className="pointer-events-none mb-2 [[data-slot=field-label]+&]:-mt-2"
              >
                {t(fieldState.error.message)}
              </FieldError>
            )}
          </FieldContent>
        </Field>
      )}
    />
  )

  return (
    <AuthLayout>
      <AuthCard
        key="form"
        focusHeading
        icon={
          <span className="flex items-center gap-2.5">
            <AccountAvatar name={trainerName} />
            <span className="text-muted-foreground min-w-0 text-sm break-words">
              {t('account.join.invitedBy', { trainerName })}
            </span>
          </span>
        }
        iconClassName="mb-2 size-auto place-items-start rounded-none bg-transparent"
        title={t('account.join.title', { trainerFirst })}
        description={t('account.join.description', { trainerFirst })}
      >
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <FieldGroup className="gap-4">
            <Field>
              <FieldLabel htmlFor="email">
                {t('account.signUp.email')}
              </FieldLabel>
              <Input
                id="email"
                type="email"
                // lets a password manager store the new password for this email
                autoComplete="username"
                value={email}
                readOnly
                className={`${touchInput} bg-muted border-dashed`}
                aria-describedby="email-help"
              />
              <FieldDescription id="email-help">
                {t('account.join.emailHelp')}
              </FieldDescription>
            </Field>

            <Field data-invalid={!!errors.name}>
              <FieldLabel htmlFor="name">{t('account.signUp.name')}</FieldLabel>
              <Input
                id="name"
                autoComplete="name"
                placeholder={t('account.join.namePlaceholder')}
                className={touchInput}
                readOnly={busy}
                aria-invalid={!!errors.name}
                aria-describedby={errors.name ? 'name-error' : undefined}
                {...form.register('name')}
              />
              {errors.name?.message && (
                <FieldError id="name-error">
                  {t(errors.name.message)}
                </FieldError>
              )}
            </Field>

            <Field data-invalid={!!errors.password}>
              <FieldLabel htmlFor="password">
                {t('account.signUp.password')}
              </FieldLabel>
              <PasswordInput
                id="password"
                autoComplete="new-password"
                readOnly={busy}
                aria-invalid={!!errors.password}
                aria-describedby="password-rules"
                {...form.register('password')}
              />
              {/* the rules are the help text; when they are broken the same text becomes the error */}
              {errors.password ? (
                <FieldError id="password-rules">
                  {t('account.passwordRules')}
                </FieldError>
              ) : (
                <FieldDescription id="password-rules">
                  {t('account.passwordRules')}
                </FieldDescription>
              )}
            </Field>

            <div className="flex flex-col">
              {box('atLeastSixteen', t('account.join.atLeastSixteen'))}
              {box(
                'healthDataConsent',
                t('account.join.consent', { trainerFirst }),
                // outside the label, so opening the notice leaves the box as it was
                <a
                  href="/privacy"
                  target="_blank"
                  rel="noopener"
                  className="text-foreground inline-flex min-h-6 items-center self-start text-sm underline underline-offset-3"
                >
                  {t('account.join.privacyNotice')}
                  <span className="sr-only">
                    {' '}
                    {t('account.join.opensInNewTab')}
                  </span>
                </a>,
              )}
            </div>
          </FieldGroup>

          {failure && (
            <Alert variant="destructive">
              <AlertDescription>{t(failure)}</AlertDescription>
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
            {t('account.join.submit')}
          </Button>
        </form>
      </AuthCard>
      <AuthFooter>
        {t('account.join.haveAccount')}{' '}
        <Link to="/sign-in" className="text-foreground">
          {t('account.signIn')}
        </Link>
      </AuthFooter>
    </AuthLayout>
  )
}
