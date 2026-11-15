import { describe, expect, it } from 'vitest'
import {
  planChangeBadgeLabel,
  planChangeStatus,
  waitsForDecisionOf,
} from './plan-change-state'

const applied = {
  state: 'APPLIED' as const,
  acceptedByTrainer: true,
  acceptedByClient: true,
}

describe('planChangeStatus', () => {
  it('reads an applied plan change as upcoming until it took effect and as in effect after', () => {
    expect(planChangeStatus(applied, 'TRAINER')).toBe('upcoming')
    expect(
      planChangeStatus(
        { ...applied, tookEffectAt: '2026-11-16T00:05:00Z' },
        'CLIENT',
      ),
    ).toBe('inEffect')
  })

  it('keeps the other states as they were', () => {
    const open = { acceptedByTrainer: false, acceptedByClient: false }
    expect(planChangeStatus({ ...open, state: 'PENDING' }, 'TRAINER')).toBe(
      'waitingForBoth',
    )
    expect(planChangeStatus({ ...open, state: 'DECLINED' }, 'TRAINER')).toBe(
      'declined',
    )
    expect(planChangeStatus({ ...open, state: 'EXPIRED' }, 'TRAINER')).toBe(
      'expired',
    )
  })
})

describe('waitsForDecisionOf', () => {
  it('does not wait for anybody once the plan change is applied, upcoming or not', () => {
    expect(waitsForDecisionOf(applied, 'CLIENT')).toBe(false)
    expect(
      waitsForDecisionOf(
        { ...applied, tookEffectAt: '2026-11-16T00:05:00Z' },
        'TRAINER',
      ),
    ).toBe(false)
  })
})

describe('planChangeBadgeLabel', () => {
  // the key stands for the words, so the date handed to it can be read
  const t = ((key: string, options: { date?: string }) =>
    `${key}|${options.date ?? ''}`) as never

  it('names the Monday of an upcoming one and the day an in-effect one took effect', () => {
    expect(
      planChangeBadgeLabel(
        t,
        { ...applied, effectiveWeek: '2026-11-16' },
        'TRAINER',
        'en',
      ),
    ).toBe('planChange.badge.upcoming|Mon, Nov 16')
    expect(
      planChangeBadgeLabel(
        t,
        {
          ...applied,
          effectiveWeek: '2026-11-16',
          tookEffectAt: '2026-11-16T12:00:00Z',
        },
        'TRAINER',
        'en',
      ),
    ).toBe('planChange.badge.inEffect|Mon, Nov 16')
  })
})
