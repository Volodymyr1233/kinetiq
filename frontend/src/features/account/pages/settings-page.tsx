import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldTitle,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import {
  currentAccountQuery,
  useUpdateAccount,
  type CurrentAccount,
} from '../api/account'
import { SettingToggleGroup } from '../components/setting-toggle-group'
import { signUpSchema } from '../sign-up-schema'

const UNITS = ['KG', 'LB'] as const
const LANGUAGES = ['EN', 'PL'] as const
const THEMES = ['LIGHT', 'DARK', 'SYSTEM'] as const

const settingsSchema = z.object({
  name: signUpSchema.shape.name,
  units: z.enum(UNITS),
  language: z.enum(LANGUAGES),
  theme: z.enum(THEMES),
})

type SettingsValues = z.input<typeof settingsSchema>

const valuesOf = ({ name, units, language, theme }: CurrentAccount) => ({
  name,
  units,
  language,
  theme,
})

function serverNameCode(error: unknown): string | undefined {
  const errors =
    typeof error === 'object' && error !== null && 'errors' in error
      ? error.errors
      : undefined
  return Array.isArray(errors)
    ? errors.find((fieldError) => fieldError?.field === 'name')?.code
    : undefined
}

/** The trainer's profile and display settings, saved together with "Save changes". */
export function SettingsPage() {
  const { data: account } = useQuery(currentAccountQuery)
  // the area's guard loads the account before any of its pages renders
  return account ? <SettingsForm account={account} /> : null
}

function SettingsForm({ account }: { account: CurrentAccount }) {
  const { t } = useTranslation()
  const update = useUpdateAccount()
  const [saved, setSaved] = useState(false)
  const [failure, setFailure] = useState<string>()
  const form = useForm<
    SettingsValues,
    unknown,
    z.output<typeof settingsSchema>
  >({
    resolver: zodResolver(settingsSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: valuesOf(account),
  })
  const { errors, dirtyFields, isDirty } = form.formState
  const busy = update.isPending

  // "Settings saved." stays only until the trainer changes something again
  useEffect(
    () =>
      form.subscribe({
        formState: { values: true },
        callback: () => setSaved(false),
      }),
    [form],
  )

  const onSubmit = form.handleSubmit((values) => {
    if (busy) return
    setFailure(undefined)
    // only what changed here, so a value changed on another device meanwhile stays (spec, Clarifications)
    const changed = Object.fromEntries(
      Object.entries(values).filter(
        ([field]) => dirtyFields[field as keyof SettingsValues],
      ),
    )
    update.mutate(changed, {
      onSuccess: (updated) => {
        form.reset(valuesOf(updated))
        setSaved(true)
      },
      onError: (error) => {
        const code = serverNameCode(error)
        if (code === undefined) {
          setFailure(errorMessageKey(error))
          return
        }
        form.setError(
          'name',
          {
            type: 'server',
            message:
              code === 'size'
                ? 'account.errors.nameLength'
                : 'account.errors.name',
          },
          { shouldFocus: true },
        )
      },
    })
  })

  function displayRow(
    id: string,
    label: string,
    hint: string | null,
    control: ReactNode,
  ) {
    return (
      <Field
        orientation="horizontal"
        className="py-4 first:pt-0 last:pb-0 has-[>[data-slot=field-content]]:items-center"
      >
        <FieldContent>
          <FieldTitle id={id}>{t(label)}</FieldTitle>
          {hint && (
            <FieldDescription id={`${id}-hint`}>{t(hint)}</FieldDescription>
          )}
        </FieldContent>
        {control}
      </Field>
    )
  }

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <h1 className="font-heading text-2xl font-semibold">
        {t('nav.settings')}
      </h1>
      <form
        noValidate
        onSubmit={onSubmit}
        className="flex max-w-3xl flex-col gap-6"
      >
        <Card>
          <CardHeader>
            <CardTitle>{t('settings.profile')}</CardTitle>
          </CardHeader>
          <CardContent>
            <FieldGroup className="grid gap-4 md:grid-cols-2">
              <Field data-invalid={!!errors.name}>
                <FieldLabel htmlFor="settings-name">
                  {t('settings.name')}
                </FieldLabel>
                <Input
                  id="settings-name"
                  autoComplete="name"
                  readOnly={busy}
                  aria-invalid={!!errors.name}
                  aria-describedby={
                    errors.name ? 'settings-name-error' : undefined
                  }
                  {...form.register('name')}
                />
                {errors.name?.message && (
                  <FieldError id="settings-name-error">
                    {t(errors.name.message)}
                  </FieldError>
                )}
              </Field>
              <Field>
                <FieldLabel htmlFor="settings-email">
                  {t('settings.email')}
                </FieldLabel>
                <Input
                  id="settings-email"
                  type="email"
                  value={account.email}
                  readOnly
                  className="bg-muted"
                />
              </Field>
            </FieldGroup>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('settings.display')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col divide-y">
            {displayRow(
              'settings-units',
              'settings.units.label',
              'settings.units.hint',
              <Controller
                control={form.control}
                name="units"
                render={({ field }) => (
                  <SettingToggleGroup
                    value={field.value}
                    onValueChange={field.onChange}
                    options={UNITS.map((unit) => ({
                      value: unit,
                      label: t(`settings.units.${unit}`),
                    }))}
                    disabled={busy}
                    aria-labelledby="settings-units"
                    aria-describedby="settings-units-hint"
                  />
                )}
              />,
            )}
            {displayRow(
              'settings-language',
              'language.label',
              'settings.languageHint',
              <Controller
                control={form.control}
                name="language"
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                    disabled={busy}
                  >
                    <SelectTrigger
                      className="w-40"
                      aria-labelledby="settings-language"
                      aria-describedby="settings-language-hint"
                    >
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {LANGUAGES.map((language) => (
                        <SelectItem key={language} value={language}>
                          {t(`language.${language.toLowerCase()}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />,
            )}
            {displayRow(
              'settings-theme',
              'theme.label',
              null,
              <Controller
                control={form.control}
                name="theme"
                render={({ field }) => (
                  <SettingToggleGroup
                    value={field.value}
                    onValueChange={field.onChange}
                    options={THEMES.map((theme) => ({
                      value: theme,
                      label: t(`theme.${theme.toLowerCase()}`),
                    }))}
                    disabled={busy}
                    aria-labelledby="settings-theme"
                  />
                )}
              />,
            )}
          </CardContent>
        </Card>

        {failure && (
          <Alert variant="destructive">
            <AlertDescription>{t(failure)}</AlertDescription>
          </Alert>
        )}

        <div className="flex items-center justify-end gap-3">
          <p role="status" className="text-muted-foreground text-sm">
            {saved && t('settings.saved')}
          </p>
          <Button type="submit" disabled={!isDirty || busy} aria-busy={busy}>
            {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
            {t('settings.save')}
          </Button>
        </div>
      </form>
    </div>
  )
}
