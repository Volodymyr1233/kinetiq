import { WifiOffIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { storageWorks } from '@/lib/pending-logs'

/**
 * Shown while the connection is down: rows stay editable and what is logged is kept on the phone. Not an alert, because
 * the logger announces its appearance once in a status of its own, without moving focus.
 */
export function OfflineBanner() {
  const { t } = useTranslation()
  // asked once, when the connection drops: the answer does not change while the banner shows
  const [kept] = useState(storageWorks)
  return (
    <Alert variant="info" role={undefined}>
      <WifiOffIcon aria-hidden />
      <AlertTitle>{t('sessionlog.offline.title')}</AlertTitle>
      <AlertDescription>
        <p>{t('sessionlog.offline.text')}</p>
        {!kept && <p>{t('sessionlog.offline.notKept')}</p>}
      </AlertDescription>
    </Alert>
  )
}
