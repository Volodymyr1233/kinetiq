import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  const { t } = useTranslation()

  return (
    <main className="mx-auto flex min-h-svh max-w-[640px] flex-col justify-center gap-4 px-4 py-12">
      <h1 className="text-lg font-semibold">{t('notFound.title')}</h1>
      <p className="text-muted-foreground text-sm">{t('notFound.body')}</p>
      <Button asChild className="self-start">
        <Link to="/">{t('notFound.action')}</Link>
      </Button>
    </main>
  )
}
