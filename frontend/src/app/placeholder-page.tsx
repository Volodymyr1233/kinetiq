import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

// stands in for screens whose feature has not been built yet
export function PlaceholderPage({
  title,
  children,
}: {
  title: string
  children?: ReactNode
}) {
  const { t } = useTranslation()

  return (
    <div className="flex flex-col gap-2 px-6 py-8">
      <h1 className="text-2xl font-semibold">{title}</h1>
      <p className="text-muted-foreground text-sm">
        {t('placeholderPage.body')}
      </p>
      {children}
    </div>
  )
}
