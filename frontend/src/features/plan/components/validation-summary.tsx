import { CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { target, type Draft, type Problem } from '../plan-draft'

/**
 * Everything that stopped a save or publish, above the sessions. It takes focus on a failed attempt and follows the
 * draft as problems get fixed. A line with a target links to it.
 */
export function ValidationSummary({
  draft,
  problems,
  title,
  sessionLabel,
  onGoTo,
}: {
  draft: Draft
  problems: Problem[]
  title?: string
  sessionLabel: (key: string) => string
  onGoTo: (problem: Problem) => void
}) {
  const { t } = useTranslation()

  function line(problem: Problem) {
    const text = t(problem.message, {
      ...problem.values,
      session: problem.session && sessionLabel(problem.session),
    })
    if (!problem.session) return text
    const session = sessionLabel(problem.session)
    if (problem.message === 'plan.problems.emptySession') return text
    const exercise = draft.sessions
      .find((candidate) => candidate.key === problem.session)
      ?.entries.find((entry) => entry.key === problem.entry)?.exerciseName
    return exercise
      ? t('plan.problems.inEntry', { session, exercise, problem: text })
      : t('plan.problems.inSession', { session, problem: text })
  }

  return (
    <Alert
      id={target.summary}
      tabIndex={-1}
      variant="destructive"
      className="focus-visible:ring-ring/50 outline-none focus-visible:ring-[3px]"
    >
      <CircleAlertIcon aria-hidden />
      {title && <AlertTitle>{title}</AlertTitle>}
      <AlertDescription>
        <ul className="flex flex-col gap-1">
          {problems.map((problem, index) => (
            <li key={`${problem.target ?? problem.message}-${index}`}>
              {problem.target ? (
                <a
                  href={`#${problem.target}`}
                  className="focus-visible:ring-ring/50 rounded-sm underline underline-offset-4 outline-none focus-visible:ring-[3px]"
                  onClick={(event) => {
                    event.preventDefault()
                    onGoTo(problem)
                  }}
                >
                  {line(problem)}
                </a>
              ) : (
                line(problem)
              )}
            </li>
          ))}
        </ul>
      </AlertDescription>
    </Alert>
  )
}
