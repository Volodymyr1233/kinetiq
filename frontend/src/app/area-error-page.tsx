import { CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useRevalidator } from 'react-router'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'

// shown when an area cannot find out who is signed in, for example while the network is down
export function AreaErrorPage() {
  const { t } = useTranslation()
  const revalidator = useRevalidator()
  const busy = revalidator.state === 'loading'

  return (
    <main className="bg-background mx-auto flex min-h-svh w-full max-w-[400px] flex-col justify-center gap-4 px-4">
      <Alert variant="destructive">
        <CircleAlertIcon aria-hidden="true" />
        <AlertDescription>{t('errors.generic')}</AlertDescription>
      </Alert>
      <Button
        size="touch"
        disabled={busy}
        aria-busy={busy}
        onClick={() => void revalidator.revalidate()}
      >
        {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
        {t('areaError.retry')}
      </Button>
    </main>
  )
}
