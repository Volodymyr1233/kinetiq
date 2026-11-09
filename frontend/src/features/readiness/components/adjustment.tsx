import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import type { Adjustment } from '../api/check-in'

/** The mark on today's session while its targets are lighter. */
export function AdjustmentMark() {
  const { t } = useTranslation()

  return <Badge variant="adjusted">{t('readiness.adjustment.badge')}</Badge>
}

/** One sentence naming the answers that made today's targets lighter, in the order the answers are asked. */
export function AdjustmentReason({ causes }: { causes: Adjustment['causes'] }) {
  const { t, i18n } = useTranslation()
  const list = new Intl.ListFormat(i18n.language, {
    type: 'conjunction',
  }).format(causes.map((cause) => t(`readiness.adjustment.cause.${cause}`)))
  // the list opens the sentence, so its first letter is a capital in the UI language
  const opening =
    list.charAt(0).toLocaleUpperCase(i18n.language) + list.slice(1)

  return <p>{t('readiness.adjustment.reason', { causes: opening })}</p>
}

/** Says that only today's targets change, under the session's action. */
export function AdjustmentNote() {
  const { t } = useTranslation()

  return (
    <p className="text-muted-foreground text-center text-sm">
      {t('readiness.adjustment.planUnchanged')}
    </p>
  )
}
