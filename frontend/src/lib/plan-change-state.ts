import type { TFunction } from 'i18next'
import { formatDay } from '@/lib/dates'

export type PlanChangeSide = 'TRAINER' | 'CLIENT'

type Decided = {
  state: 'PENDING' | 'ACCEPTED_BY_ONE' | 'APPLIED' | 'DECLINED' | 'EXPIRED'
  acceptedByTrainer: boolean
  acceptedByClient: boolean
  /** Set once the plan uses an applied plan change; an applied one without it is upcoming. */
  tookEffectAt?: string
}

/** What a plan change waits for, as `viewer` reads it: the label of its badge, or its end. */
export type PlanChangeStatus =
  | 'waitingForBoth'
  | 'waitingForYou'
  | 'waitingFor'
  | 'upcoming'
  | 'inEffect'
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
      return planChange.tookEffectAt === undefined ? 'upcoming' : 'inEffect'
    case 'DECLINED':
      return 'declined'
    case 'EXPIRED':
      return 'expired'
  }
}

/** The badge variant of each status: waiting is information, applied is good news, the rest only closes. */
export const planChangeBadgeVariant = {
  waitingForBoth: 'info',
  waitingForYou: 'info',
  waitingFor: 'info',
  upcoming: 'info',
  inEffect: 'success',
  declined: 'secondary',
  expired: 'outline',
} as const satisfies Record<PlanChangeStatus, string>

/**
 * The words of a plan change's badge: what it waits for, or how it ended. An applied one names its Monday, the day it
 * takes effect or took effect on.
 */
export function planChangeBadgeLabel(
  t: TFunction,
  planChange: Decided & { effectiveWeek: string },
  viewer: PlanChangeSide,
  language: string,
  name?: string,
) {
  const status = planChangeStatus(planChange, viewer)
  const date =
    status === 'upcoming' || status === 'inEffect'
      ? formatDay(planChange.effectiveWeek, language)
      : undefined
  return t(`planChange.badge.${status}`, { name, date })
}
