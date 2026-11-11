import { useState } from 'react'
import type { DeclineReason } from './client-plan-change'

type Decision = {
  isPending: boolean
  mutate: (
    variables: { id: string },
    options: { onSuccess: () => void; onError: (error: unknown) => void },
  ) => void
}

type KeepDecision = {
  isPending: boolean
  mutateAsync: (variables: {
    id: string
    reason: DeclineReason
    note?: string
  }) => Promise<unknown>
}

/**
 * What a decision page does with its three mutations: whether any is running, the last refusal (the screen itself keeps
 * the last state the server confirmed), and the handlers the decision panel calls. Both people's pages use it.
 */
export function usePlanChangeDecisions(
  accept: Decision,
  undo: Decision,
  keep: KeepDecision,
) {
  const [error, setError] = useState<unknown>()
  const run = (mutation: Decision, id: string) =>
    mutation.mutate(
      { id },
      { onSuccess: () => setError(undefined), onError: setError },
    )

  return {
    error,
    busy: accept.isPending || undo.isPending || keep.isPending,
    accept: (id: string) => run(accept, id),
    undo: (id: string) => run(undo, id),
    /** Resolves to whether the plan change was declined. */
    keep: (id: string, reason: DeclineReason, note: string | undefined) =>
      keep
        .mutateAsync({ id, reason, note })
        .then(() => {
          setError(undefined)
          return true
        })
        .catch((failure: unknown) => {
          setError(failure)
          return false
        }),
  }
}
