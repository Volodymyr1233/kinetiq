import { useTranslation } from 'react-i18next'
import type { components } from '@/lib/api/schema'

type Band = NonNullable<components['schemas']['ReadinessDayResponse']['band']>
type Cause = components['schemas']['RecoveryDayResponse']['causes'][number]

// the readiness scale, never red: a low score is not an error
export const BAND_STYLE: Record<Band, { surface: string; dot: string }> = {
  READY: { surface: 'bg-ready-high-muted', dot: 'bg-ready-high' },
  TAKE_IT_EASIER: { surface: 'bg-ready-mid-muted', dot: 'bg-ready-mid' },
  RECOVERY_DAY: { surface: 'bg-ready-low-muted', dot: 'bg-ready-low' },
}

/**
 * Joins the check-in answers that called for lighter targets into one list in the UI language, in the order the
 * answers are asked. `opening` capitalises its first letter, for a list that starts a sentence.
 */
export function useCauseList() {
  const { t, i18n } = useTranslation()
  return (causes: Cause[], { opening = false } = {}) => {
    const list = new Intl.ListFormat(i18n.language, {
      type: 'conjunction',
    }).format(causes.map((cause) => t(`readiness.adjustment.cause.${cause}`)))
    return opening
      ? list.charAt(0).toLocaleUpperCase(i18n.language) + list.slice(1)
      : list
  }
}
