import { zodResolver } from '@hookform/resolvers/zod'
import { EyeIcon, EyeOffIcon } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, type FieldPath } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import type { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupButton,
  InputGroupInput,
} from '@/components/ui/input-group'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useSignUp } from '../api/account'
import { AuthCard, AuthFooter, AuthLayout } from '../components/auth-layout'
import { CheckEmailCard } from '../components/check-email-card'
import { signUpSchema, type SignUpValues } from '../sign-up-schema'

type FieldName = FieldPath<SignUpValues>

const FIELDS: FieldName[] = ['name', 'email', 'password', 'adult']

// one message per field and rule, the same whether the form or the server found it (FR-025)
function serverMessage(field: FieldName, code: string) {
  if (field === 'name') {
    return code === 'size' ? 'account.errors.nameLength' : 'account.errors.name'
  }
  if (field === 'password') return 'account.passwordRules'
  return `account.errors.${field}`
}

function serverFieldErrors(error: unknown) {
  const errors =
    typeof error === 'object' && error !== null && 'errors' in error
      ? error.errors
      : undefined
  if (!Array.isArray(errors)) return []
  return errors.filter(
    (fieldError): fieldError is { field: FieldName; code: string } =>
      FIELDS.includes(fieldError?.field),
  )
}

// some browsers report no zone or Etc/Unknown; the trainer can change it in settings later
function browserTimeZone() {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  return !zone || zone === 'Etc/Unknown' ? 'UTC' : zone
}

const touchInput = 'h-(--control-h-touch) text-base md:text-base'

export function SignUpPage() {
  const { t } = useTranslation()
  const signUp = useSignUp()
  const [sentTo, setSentTo] = useState<string>()
  const [failure, setFailure] = useState<string>()
  const [passwordShown, setPasswordShown] = useState(false)
  const form = useForm<SignUpValues, unknown, z.output<typeof signUpSchema>>({
    resolver: zodResolver(signUpSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { name: '', email: '', password: '', adult: false },
  })
  const { errors } = form.formState
  const busy = signUp.isPending

  const onSubmit = form.handleSubmit((values) => {
    if (busy) return
    setFailure(undefined)
    signUp.mutate(
      { ...values, timeZone: browserTimeZone() },
      {
        onSuccess: () => {
          // the password never stays in the form once the person leaves it
          form.resetField('password')
          setPasswordShown(false)
          setSentTo(values.email)
        },
        onError: (error) => {
          const fieldErrors = serverFieldErrors(error)
          fieldErrors.forEach(({ field, code }, index) =>
            form.setError(
              field,
              { type: 'server', message: serverMessage(field, code) },
              { shouldFocus: index === 0 },
            ),
          )
          // a field the form has no message for, such as the time zone, gets the generic one
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
        title={t('account.signUp.title')}
        description={t('account.signUp.description')}
      >
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-5">
          <FieldGroup className="gap-4">
            <Field data-invalid={!!errors.name}>
              <FieldLabel htmlFor="name">{t('account.signUp.name')}</FieldLabel>
              <Input
                id="name"
                autoComplete="name"
                placeholder={t('account.signUp.namePlaceholder')}
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

            <Field data-invalid={!!errors.email}>
              <FieldLabel htmlFor="email">
                {t('account.signUp.email')}
              </FieldLabel>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder={t('account.signUp.emailPlaceholder')}
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
              <FieldLabel htmlFor="password">
                {t('account.signUp.password')}
              </FieldLabel>
              <InputGroup className="h-(--control-h-touch)">
                <InputGroupInput
                  id="password"
                  type={passwordShown ? 'text' : 'password'}
                  autoComplete="new-password"
                  maxLength={128}
                  className="h-full text-base md:text-base"
                  readOnly={busy}
                  aria-invalid={!!errors.password}
                  aria-describedby="password-rules"
                  {...form.register('password')}
                />
                <InputGroupAddon align="inline-end">
                  <InputGroupButton
                    size="icon-sm"
                    className="size-11"
                    aria-label={t(
                      passwordShown
                        ? 'account.signUp.hidePassword'
                        : 'account.signUp.showPassword',
                    )}
                    onClick={() => setPasswordShown((shown) => !shown)}
                  >
                    {passwordShown ? <EyeOffIcon /> : <EyeIcon />}
                  </InputGroupButton>
                </InputGroupAddon>
              </InputGroup>
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

            <Controller
              control={form.control}
              name="adult"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid} className="gap-2">
                  <div className="flex items-start gap-3">
                    <Checkbox
                      id="adult"
                      ref={field.ref}
                      name={field.name}
                      checked={field.value}
                      onCheckedChange={(checked) =>
                        field.onChange(checked === true)
                      }
                      onBlur={field.onBlur}
                      disabled={busy}
                      className="mt-0.5"
                      aria-invalid={fieldState.invalid}
                      aria-describedby={
                        fieldState.invalid ? 'adult-error' : undefined
                      }
                    />
                    <FieldLabel htmlFor="adult" className="font-normal">
                      {t('account.signUp.adult')}
                    </FieldLabel>
                  </div>
                  {fieldState.error?.message && (
                    <FieldError id="adult-error" className="pl-7">
                      {t(fieldState.error.message)}
                    </FieldError>
                  )}
                </Field>
              )}
            />
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
            {t('account.signUp.submit')}
          </Button>
        </form>
      </AuthCard>
      <AuthFooter>
        {t('account.signUp.haveAccount')}{' '}
        <Link to="/sign-in" className="text-foreground">
          {t('account.signIn')}
        </Link>
      </AuthFooter>
    </AuthLayout>
  )
}
