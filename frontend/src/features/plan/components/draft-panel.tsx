import { CircleAlertIcon, RefreshCwIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { QuizAnswersList } from '@/components/quiz-answers'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { cn } from '@/lib/utils'
import { useBuildPlanAgain, type Plan } from '../api/plans'
import { useQuizAnswers } from '../api/quiz-answers'
import { BuildAgainDialog } from './build-again-dialog'

// the 429 carries when the next build is allowed
function availableAt(error: unknown) {
  return typeof error === 'object' &&
    error !== null &&
    'availableAt' in error &&
    typeof error.availableAt === 'string'
    ? error.availableAt
    : undefined
}

/**
 * Beside a draft: what the client answered, and the way to ask for a new automatic draft. While a drafting runs the
 * button stays in place, unavailable, so the focus it may hold is not lost.
 */
export function DraftPanel({
  client,
  plan,
  drafting,
  onBuildAgain,
}: {
  client: { id: string; name: string }
  plan: Plan
  drafting: boolean
  onBuildAgain: () => void
}) {
  const { t, i18n } = useTranslation()
  const answers = useQuizAnswers(client.id)
  const buildAgain = useBuildPlanAgain(client.id)
  const [confirming, setConfirming] = useState(false)
  const failed = plan.automaticDraft === 'FAILED'
  const limitUntil = availableAt(buildAgain.error)
  const unavailable = drafting || buildAgain.isPending || !!limitUntil
  // a refusal for a drafting already running says nothing: the tab moves to that drafting
  const error =
    buildAgain.isError &&
    !limitUntil &&
    problemCode(buildAgain.error) !== 'plan.drafting'
      ? buildAgain.error
      : undefined

  function accept() {
    setConfirming(false)
    buildAgain.mutate(undefined, { onSuccess: onBuildAgain })
  }

  return (
    <Card className="max-w-full flex-[1_1_280px] gap-4">
      <CardHeader>
        <CardTitle>
          <h2>{t('plan.panel.title', { name: client.name })}</h2>
        </CardTitle>
        {answers.data && (
          <CardDescription>
            {new Intl.DateTimeFormat(i18n.language, {
              dateStyle: 'medium',
            }).format(new Date(answers.data.submittedAt))}
          </CardDescription>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {failed && (
          <Alert variant="info" role="note">
            <CircleAlertIcon aria-hidden="true" />
            <AlertDescription>{t('plan.panel.failed')}</AlertDescription>
          </Alert>
        )}
        {answers.isPending ? (
          <div aria-hidden className="flex flex-col gap-2">
            {Array.from({ length: 4 }, (_, row) => (
              <Skeleton key={row} className="h-5 w-full" />
            ))}
          </div>
        ) : answers.data ? (
          <QuizAnswersList answers={answers.data} name={client.name} />
        ) : (
          <p className="text-muted-foreground text-sm">
            {t('plan.panel.unavailable', { name: client.name })}
          </p>
        )}
        <Separator />
        <div className="flex flex-col items-start gap-2">
          <Button
            // primary for a failed draft, where it is the way forward; quieter beside a draft to review
            variant={failed ? 'default' : 'outline'}
            aria-disabled={unavailable}
            className={cn(unavailable && 'cursor-not-allowed opacity-50')}
            onClick={() => {
              if (!unavailable) setConfirming(true)
            }}
          >
            <RefreshCwIcon data-icon="inline-start" aria-hidden="true" />
            {t('plan.panel.buildAgain')}
          </Button>
          {/* the reason the button is unavailable, or what went wrong */}
          <p
            role="status"
            className="text-muted-foreground text-sm empty:hidden"
          >
            {limitUntil
              ? t('plan.panel.limit', {
                  time: new Intl.DateTimeFormat(i18n.language, {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                  }).format(new Date(limitUntil)),
                })
              : error && t(errorMessageKey(error))}
          </p>
        </div>
      </CardContent>
      <BuildAgainDialog
        open={confirming}
        clientName={client.name}
        onOpenChange={setConfirming}
        onAccept={accept}
      />
    </Card>
  )
}
