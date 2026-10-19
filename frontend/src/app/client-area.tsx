import { useTranslation } from 'react-i18next'
import { PlaceholderPage } from '@/app/placeholder-page'

export function ClientArea() {
  const { t } = useTranslation()

  return (
    <main>
      <PlaceholderPage title={t('areas.client')} />
    </main>
  )
}
