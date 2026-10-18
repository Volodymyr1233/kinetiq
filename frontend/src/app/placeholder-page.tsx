import { useTranslation } from 'react-i18next'

// stands in for screens whose feature has not been built yet
export function PlaceholderPage({ title }: { title: string }) {
  const { t } = useTranslation()

  return (
    <main className="flex flex-col gap-2 px-4 py-12">
      <h1 className="text-lg font-semibold">{title}</h1>
      <p className="text-muted-foreground text-sm">
        {t('placeholderPage.body')}
      </p>
    </main>
  )
}
