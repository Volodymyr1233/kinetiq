import { CircleAlertIcon, SparklesIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'
import type { Plan } from '../api/plans'

/** Says what the system did with a draft, until the trainer's first save makes the plan theirs. */
export function AutomaticDraftNote({
  mark,
  clientName,
}: {
  mark: NonNullable<Plan['automaticDraft']>
  clientName: string
}) {
  const { t } = useTranslation()
  const Icon = mark === 'BUILT' ? SparklesIcon : CircleAlertIcon
  return (
    // part of the page, not an event: an alert role would interrupt on every opening
    <Alert variant="info" role="note">
      <Icon aria-hidden="true" />
      <AlertDescription>
        {mark === 'BUILT'
          ? t('plan.note.built', { name: clientName })
          : t('plan.note.failed')}
      </AlertDescription>
    </Alert>
  )
}
