import { SparklesIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/components/ui/alert'

/** Says the system built this draft, until the trainer's first save makes the plan theirs. */
export function AutomaticDraftNote({ clientName }: { clientName: string }) {
  const { t } = useTranslation()
  return (
    // part of the page, not an event: an alert role would interrupt on every opening
    <Alert variant="info" role="note">
      <SparklesIcon aria-hidden="true" />
      <AlertDescription>
        {t('plan.note.built', { name: clientName })}
      </AlertDescription>
    </Alert>
  )
}
