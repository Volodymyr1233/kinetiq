import { useMutationState } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldTitle,
} from '@/components/ui/field'
import { errorMessageKey } from '@/lib/api/problem'
import {
  updateAccountKey,
  useUpdateAccount,
  type AccountUpdate,
  type CurrentAccount,
} from '../api/account'
import { SettingToggleGroup } from './setting-toggle-group'

const OPTIONS = {
  units: ['KG', 'LB'],
  language: ['EN', 'PL'],
  theme: ['LIGHT', 'DARK', 'SYSTEM'],
} as const

type Setting = keyof typeof OPTIONS

/** Units, language and theme of the signed-in person, each saved as soon as it is chosen. */
export function DisplaySettings({ account }: { account: CurrentAccount }) {
  const { t } = useTranslation()
  const update = useUpdateAccount()
  // a choice shows while it is saved; a failed one drops out, so the stored value shows again
  const pending = useMutationState({
    filters: { mutationKey: updateAccountKey, status: 'pending' },
    select: (mutation) => mutation.state.variables as AccountUpdate,
  })
  const shown = pending.reduce<AccountUpdate>(
    (values, choice) => ({ ...values, ...choice }),
    { units: account.units, language: account.language, theme: account.theme },
  )

  function group(
    setting: Setting,
    title: string,
    labelOf: (option: string) => string,
    hint?: string,
  ) {
    const id = `sheet-${setting}`
    return (
      <Field>
        <FieldTitle id={id}>{title}</FieldTitle>
        <SettingToggleGroup
          touch
          value={shown[setting]}
          onValueChange={(value) => update.mutate({ [setting]: value })}
          options={OPTIONS[setting].map((option) => ({
            value: option,
            label: labelOf(option),
          }))}
          aria-labelledby={id}
          aria-describedby={hint ? `${id}-hint` : undefined}
        />
        {hint && <FieldDescription id={`${id}-hint`}>{hint}</FieldDescription>}
      </Field>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <Card size="sm">
        <CardContent>
          <FieldGroup className="gap-5">
            {group(
              'units',
              t('settings.units.label'),
              (unit) => t(`settings.units.${unit}`),
              t('settings.sheet.unitsHint', {
                weightUnit: t(`settings.units.${shown.units}`),
              }),
            )}
            {group('language', t('language.label'), (language) =>
              t(`language.${language.toLowerCase()}`),
            )}
            {group(
              'theme',
              t('settings.sheet.theme'),
              (theme) => t(`theme.${theme.toLowerCase()}`),
              t('settings.sheet.themeHint'),
            )}
          </FieldGroup>
        </CardContent>
      </Card>
      {update.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {t(errorMessageKey(update.error))}
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}
