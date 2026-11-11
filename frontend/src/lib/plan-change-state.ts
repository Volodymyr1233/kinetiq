export type PlanChangeSide = 'TRAINER' | 'CLIENT'

type Decided = {
  state: 'PENDING' | 'ACCEPTED_BY_ONE' | 'APPLIED' | 'DECLINED' | 'EXPIRED'
  acceptedByTrainer: boolean
  acceptedByClient: boolean
}

/** What a plan change waits for, as `viewer` reads it: the label of its badge, or its end. */
export type PlanChangeStatus =
  | 'waitingForBoth'
  | 'waitingForYou'
  | 'waitingFor'
  | 'applied'
  | 'declined'
  | 'expired'

export function acceptedBy(
  planChange: Pick<Decided, 'acceptedByTrainer' | 'acceptedByClient'>,
  side: PlanChangeSide,
) {
  return side === 'TRAINER'
    ? planChange.acceptedByTrainer
    : planChange.acceptedByClient
}

/** Whether `viewer` still has to decide: the plan change is open and they have not accepted it. */
export function waitsForDecisionOf(
  planChange: Decided,
  viewer: PlanChangeSide,
) {
  return (
    (planChange.state === 'PENDING' ||
      planChange.state === 'ACCEPTED_BY_ONE') &&
    !acceptedBy(planChange, viewer)
  )
}

export function planChangeStatus(
  planChange: Decided,
  viewer: PlanChangeSide,
): PlanChangeStatus {
  switch (planChange.state) {
    case 'PENDING':
      return 'waitingForBoth'
    case 'ACCEPTED_BY_ONE':
      return acceptedBy(planChange, viewer) ? 'waitingFor' : 'waitingForYou'
    case 'APPLIED':
      return 'applied'
    case 'DECLINED':
      return 'declined'
    case 'EXPIRED':
      return 'expired'
  }
}
