import { useTranslation } from 'react-i18next'
import { PlaceholderPage } from '@/app/placeholder-page'

export function TrainerArea() {
  const { t } = useTranslation()

  return <PlaceholderPage title={t('areas.trainer')} />
}
