import { CircleAlertIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate } from 'react-router'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useQuizStatus, type QuizStatus } from '../api/quiz'

// a published plan leaves the quiz for the app
const PAGE_FOR: Record<QuizStatus, string> = {
  NOT_SUBMITTED: '/app/quiz',
  WAITING_FOR_PLAN: '/app/quiz/waiting',
  PLAN_PUBLISHED: '/app',
}

/** Shows a quiz page only for the status it belongs to and sends the client to the page of their status (research R12). */
export function QuizStatusCheck({
  status,
  children,
}: {
  status: QuizStatus
  children: ReactNode
}) {
  const { t } = useTranslation()
  const query = useQuizStatus()

  // a check that fails later, on returning to the tab, keeps the page that is shown
  if (query.data === undefined) {
    if (!query.isError) {
      return (
        <p role="status" className="sr-only">
          {t('quiz.loading')}
        </p>
      )
    }
    return (
      <main className="bg-background mx-auto flex min-h-svh w-full max-w-[400px] flex-col justify-center gap-4 px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden="true" />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        </Alert>
        <Button
          size="touch"
          disabled={query.isFetching}
          aria-busy={query.isFetching}
          onClick={() => void query.refetch()}
        >
          {query.isFetching && (
            <Spinner data-icon="inline-start" aria-hidden="true" />
          )}
          {t('quiz.retry')}
        </Button>
      </main>
    )
  }

  if (query.data !== status) {
    return <Navigate replace to={PAGE_FOR[query.data]} />
  }
  return children
}
