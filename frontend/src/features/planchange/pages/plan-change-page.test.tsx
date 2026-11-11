import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, renderAppAt, SIGNED_IN } from '@/test/page'
import type { PlanChange } from '../api/plan-change'

const CLIENT_ID = '00000000-0000-4000-8000-000000000003'
const SQUAT = '00000000-0000-4000-9100-000000000011'

const BASE = {
  clientId: CLIENT_ID,
  clientName: 'Ola Kowalska',
  state: 'PENDING',
  acceptedByTrainer: false,
  acceptedByClient: false,
  effectiveWeek: '2026-11-16',
  windowStart: '2026-10-26',
  windowEnd: '2026-11-08',
  moves: [] as PlanChange['moves'],
} satisfies Partial<PlanChange>

const HIGH_RPE: PlanChange = {
  ...BASE,
  id: '00000000-0000-4000-9600-000000000001',
  rule: 'HIGH_RPE',
  title: 'Back squat: cut volume by 13%',
  reason: 'Average RPE was 8.4 and 8.6 in the last 2 weeks, above 8.',
  targets: [
    {
      sessionDay: 'TUESDAY',
      sessionName: 'Full body A',
      position: 0,
      exerciseId: SQUAT,
      exerciseName: 'Back squat',
      measure: 'REPS',
      target: 'AMOUNT',
      oldValue: 8,
      newValue: 7,
    },
  ],
  concernedExercises: [{ id: SQUAT, name: 'Back squat' }],
  figures: [
    { kind: 'MEAN_RPE', weekStart: '2026-10-26', value: 8.4 },
    { kind: 'MEAN_RPE', weekStart: '2026-11-02', value: 8.6 },
  ],
}

const STALLED_WEIGHT: PlanChange = {
  ...BASE,
  id: '00000000-0000-4000-9600-000000000002',
  rule: 'STALLED_WEIGHT',
  title: 'Back squat: raise load by 5%',
  reason: 'Back squat rose 1.3% in 3 weeks, less than 5%.',
  targets: [
    {
      sessionDay: 'THURSDAY',
      sessionName: 'Lower body',
      position: 0,
      exerciseId: SQUAT,
      exerciseName: 'Back squat',
      measure: 'REPS',
      target: 'LOAD_KG',
      oldValue: 80,
      newValue: 84,
    },
  ],
  concernedExercises: [{ id: SQUAT, name: 'Back squat' }],
  figures: [
    {
      kind: 'BEST_LOAD_KG',
      exerciseId: SQUAT,
      weekStart: '2026-10-19',
      value: 80,
    },
    {
      kind: 'BEST_LOAD_KG',
      exerciseId: SQUAT,
      weekStart: '2026-10-26',
      value: 80,
    },
    {
      kind: 'BEST_LOAD_KG',
      exerciseId: SQUAT,
      weekStart: '2026-11-02',
      value: 81,
    },
  ],
}

const LOW_COMPLETION: PlanChange = {
  ...BASE,
  id: '00000000-0000-4000-9600-000000000003',
  rule: 'LOW_COMPLETION',
  title: 'Cut to 5 sessions a week',
  reason:
    '50% of planned sessions were finished in the last 2 weeks, below 70%.',
  targets: [],
  removedSession: { day: 'SATURDAY', name: 'Full body' },
  moves: [
    {
      exerciseId: SQUAT,
      exerciseName: 'Back squat',
      fromDay: 'SATURDAY',
      toDay: 'MONDAY',
    },
  ],
  concernedExercises: [],
  figures: [
    { kind: 'SESSIONS_PLANNED', weekStart: '2026-10-26', value: 4 },
    { kind: 'SESSIONS_FINISHED', weekStart: '2026-10-26', value: 2 },
    { kind: 'SESSIONS_PLANNED', weekStart: '2026-11-02', value: 6 },
    { kind: 'SESSIONS_FINISHED', weekStart: '2026-11-02', value: 3 },
  ],
}

function renderPlanChange(
  planChange: PlanChange | undefined,
  account: object = SIGNED_IN.TRAINER,
) {
  return renderAppAt(
    `/trainer/plan-changes/${planChange?.id ?? '00000000-0000-4000-9600-000000000099'}`,
    account,
    (request) =>
      planChange &&
      new URL(request.url).pathname === `/api/v1/plan-changes/${planChange.id}`
        ? json(planChange)
        : problem(404, { status: 404, code: 'plan-change.not-found' }),
  )
}

const cells = (row: HTMLElement) =>
  within(row)
    .getAllByRole('cell')
    .map((cell) => cell.textContent)

describe('PlanChangePage', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('shows the title, the reason, the state and each changed value now and new, for high RPE', async () => {
    renderPlanChange(HIGH_RPE)

    expect(
      await screen.findByRole('heading', { name: HIGH_RPE.title, level: 1 }),
    ).toBeInTheDocument()
    expect(screen.getByText(HIGH_RPE.reason)).toBeInTheDocument()
    expect(screen.getByText('Waiting for both')).toBeInTheDocument()
    const [, row] = screen.getAllByRole('row')
    expect(cells(row)).toEqual([
      'Back squat',
      'Full body A, Tuesday',
      '8 reps',
      '7 reps',
      '-13%',
    ])
    expect(
      screen.getByText(
        /Only exercises with an average RPE above 8 change: Back squat/,
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'Takes effect Mon, Nov 16 once you and Ola Kowalska accept.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Average RPE: Oct 26: 8.4, Nov 2: 8.6. Rule threshold 8.',
      }),
    ).toBeInTheDocument()
  })

  it('shows the old and new load and its rise, for stalled weight', async () => {
    renderPlanChange(STALLED_WEIGHT)

    await screen.findByRole('heading', { name: STALLED_WEIGHT.title, level: 1 })
    const [, row] = screen.getAllByRole('row')
    expect(cells(row)).toEqual([
      'Back squat',
      'Lower body, Thursday',
      '80 kg',
      '84 kg',
      '+5%',
    ])
  })

  it('draws the best load of every stalled exercise', async () => {
    const BENCH = '00000000-0000-4000-9100-000000000001'
    renderPlanChange({
      ...STALLED_WEIGHT,
      concernedExercises: [
        { id: SQUAT, name: 'Back squat' },
        { id: BENCH, name: 'Bench press' },
      ],
      figures: [
        ...STALLED_WEIGHT.figures,
        {
          kind: 'BEST_LOAD_KG',
          exerciseId: BENCH,
          weekStart: '2026-10-19',
          value: 60,
        },
        {
          kind: 'BEST_LOAD_KG',
          exerciseId: BENCH,
          weekStart: '2026-11-02',
          value: 61,
        },
      ],
    })

    expect(
      await screen.findByRole('img', {
        name: 'Best load, Back squat: Oct 19: 80 kg, Oct 26: 80 kg, Nov 2: 81 kg. Rule threshold 84 kg.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Best load, Bench press: Oct 19: 60 kg, Nov 2: 61 kg. Rule threshold 63 kg.',
      }),
    ).toBeInTheDocument()
  })

  it('shows the removed session and the moved exercise, for low completion', async () => {
    renderPlanChange(LOW_COMPLETION)

    await screen.findByRole('heading', { name: LOW_COMPLETION.title, level: 1 })
    expect(
      screen.getByRole('columnheader', { name: 'Plan' }),
    ).toBeInTheDocument()
    const [, removed, moved] = screen.getAllByRole('row')
    expect(cells(removed)).toEqual([
      'Full body',
      'Saturday',
      'Planned',
      'Removed',
      '1 session less',
    ])
    expect(cells(moved)).toEqual([
      'Back squat',
      'Saturday',
      'Saturday',
      'Monday',
      'Moved',
    ])
    expect(
      screen.getByRole('img', {
        name: 'Sessions finished: Oct 26: 50%, Nov 2: 50%. Rule threshold 70%.',
      }),
    ).toBeInTheDocument()
  })

  it('says the plan change is not found, for an unknown one or another trainer’s client’s', async () => {
    renderPlanChange(undefined)

    expect(await screen.findByText('Plan change not found')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Back to all clients' }),
    ).toHaveAttribute('href', '/trainer/clients')
  })

  it('names the trainer’s language in the table and dates, in Polish', async () => {
    await i18n.changeLanguage('pl')
    renderPlanChange(HIGH_RPE, { ...SIGNED_IN.TRAINER, language: 'PL' })

    expect(await screen.findByText('Czeka na obie strony')).toBeInTheDocument()
    const [, row] = screen.getAllByRole('row')
    expect(cells(row)).toEqual([
      'Back squat',
      'Full body A, wtorek',
      '8 powtórzeń',
      '7 powtórzeń',
      '-13%',
    ])
  })
})
