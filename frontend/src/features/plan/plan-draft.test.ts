import { describe, expect, it } from 'vitest'
import type { Exercise } from '@/lib/exercise-library'
import type { Plan } from './api/plans'
import {
  addSession,
  dayName,
  DAYS,
  draftFromPlan,
  entryFromExercise,
  firstFreeDay,
  isDirty,
  moveEntry,
  newDraft,
  problemsFromServer,
  removeSession,
  requestFromDraft,
  estimatedMinutes,
  sessionTotals,
  target,
  updateSession,
  validateDraft,
  type Draft,
} from './plan-draft'

const PLANK: Exercise = {
  id: 'plank',
  name: 'Plank',
  category: 'CORE',
  equipment: '',
  measure: 'SECONDS',
  sets: 3,
  amount: 45,
  restSeconds: 60,
  cues: '',
  plansUsing: 0,
  starter: true,
}

const PLAN: Plan = {
  status: 'DRAFT',
  goal: 'LOSE_FAT',
  weeks: 8,
  version: 2,
  sessions: [
    {
      name: 'Lower',
      day: 'WEDNESDAY',
      entries: [
        {
          exerciseId: 'squat',
          exerciseName: 'Back squat',
          measure: 'REPS',
          sets: 3,
          amount: 5,
          loadKg: 62.5,
          rpe: 8,
          restSeconds: 120,
        },
        {
          exerciseId: 'plank',
          exerciseName: 'Plank',
          measure: 'SECONDS',
          sets: 2,
          amount: 45,
          rpe: 7,
          restSeconds: 60,
        },
      ],
    },
  ],
}

const withSessionsOn = (
  ...days: Draft['sessions'][number]['day'][]
): Draft => ({
  goal: 'LOSE_FAT',
  weeks: '12',
  sessions: days.map((day) => ({ key: day, name: day, day, entries: [] })),
})

describe('newDraft', () => {
  it('starts with the goal it is given, 12 weeks and no sessions', () => {
    expect(newDraft('GET_STRONGER')).toEqual({
      goal: 'GET_STRONGER',
      weeks: '12',
      sessions: [],
    })
  })
})

describe('draftFromPlan', () => {
  it('keeps numbers as text and an empty load empty', () => {
    const [squat, plank] = draftFromPlan(PLAN, 'KG', 'en').sessions[0].entries

    expect(squat).toMatchObject({ sets: '3', amount: '5', load: '62.5' })
    expect(plank).toMatchObject({ load: '', rest: '60', measure: 'SECONDS' })
    expect(plank.stored).toBeUndefined()
  })

  it('shows a load in the trainer’s unit and the language’s decimal mark', () => {
    const [squat] = draftFromPlan(PLAN, 'LB', 'pl').sessions[0].entries

    expect(squat.load).toBe('137,8')
  })
})

describe('sessions', () => {
  it('finds the first free day, Monday first', () => {
    expect(firstFreeDay([])).toBe('MONDAY')
    expect(firstFreeDay(withSessionsOn('MONDAY', 'WEDNESDAY').sessions)).toBe(
      'TUESDAY',
    )
    expect(firstFreeDay(withSessionsOn(...DAYS).sessions)).toBeUndefined()
  })

  it('adds a session in its place in the week, with an empty name', () => {
    const { draft, key } = addSession(withSessionsOn('MONDAY', 'WEDNESDAY'))

    expect(draft.sessions.map((session) => session.day)).toEqual([
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
    ])
    expect(draft.sessions[1]).toMatchObject({ key, name: '', entries: [] })
  })

  it('moves a session to its place when its day changes, keeping its key', () => {
    const draft = updateSession(
      withSessionsOn('MONDAY', 'WEDNESDAY'),
      'MONDAY',
      {
        day: 'FRIDAY',
      },
    )

    expect(draft.sessions.map((session) => session.key)).toEqual([
      'WEDNESDAY',
      'MONDAY',
    ])
  })

  it('selects the next session after a removal, or the previous when it was the last', () => {
    const draft = withSessionsOn('MONDAY', 'WEDNESDAY', 'FRIDAY')

    expect(removeSession(draft, 'WEDNESDAY').selected).toBe('FRIDAY')
    expect(removeSession(draft, 'FRIDAY').selected).toBe('WEDNESDAY')
    expect(
      removeSession(withSessionsOn('MONDAY'), 'MONDAY').selected,
    ).toBeUndefined()
  })
})

describe('entryFromExercise', () => {
  it('takes the exercise’s defaults, with RPE 8 and no load', () => {
    expect(entryFromExercise(PLANK)).toMatchObject({
      exerciseId: 'plank',
      exerciseName: 'Plank',
      measure: 'SECONDS',
      sets: '3',
      amount: '45',
      load: '',
      rpe: '8',
      rest: '60',
    })
  })
})

describe('estimatedMinutes', () => {
  it('gives the same minutes as the plan editor shows for the same entries', () => {
    const [session] = draftFromPlan(PLAN, 'KG', 'en').sessions
    const entries = session.entries.map((entry) => ({
      measure: entry.measure,
      sets: Number(entry.sets),
      amount: Number(entry.amount),
      rest: Number(entry.rest),
    }))

    expect(estimatedMinutes(entries)).toBe(sessionTotals(session).minutes)
  })
})

describe('sessionTotals', () => {
  it('adds sets × (rest + work), with 4 seconds per repetition', () => {
    const [session] = draftFromPlan(PLAN, 'KG', 'en').sessions

    // 3 × (120 + 5 × 4) + 2 × (60 + 45) = 630 seconds
    expect(sessionTotals(session)).toEqual({
      exercises: 2,
      sets: 5,
      minutes: 11,
    })
  })

  it('counts a value that is no number as 0', () => {
    const [session] = draftFromPlan(PLAN, 'KG', 'en').sessions
    session.entries[0].sets = 'x'
    session.entries[0].amount = '8,'

    expect(sessionTotals(session)).toEqual({
      exercises: 2,
      sets: 2,
      minutes: 4,
    })
  })
})

describe('isDirty', () => {
  it('says an untouched new plan is clean', () => {
    expect(isDirty(newDraft('LOSE_FAT'), newDraft('LOSE_FAT'))).toBe(false)
  })

  it('clears when an edit is undone, whatever the keys are', () => {
    const saved = draftFromPlan(PLAN, 'KG', 'en')
    const edited = draftFromPlan(PLAN, 'KG', 'en')

    expect(isDirty(edited, saved)).toBe(false)
    edited.sessions[0].entries[0].sets = '4'
    expect(isDirty(edited, saved)).toBe(true)
    edited.sessions[0].entries[0].sets = '3'
    expect(isDirty(edited, saved)).toBe(false)
  })

  it('sees a change of order', () => {
    const saved = draftFromPlan(PLAN, 'KG', 'en')
    const edited = draftFromPlan(PLAN, 'KG', 'en')
    edited.sessions[0].entries.reverse()

    expect(isDirty(edited, saved)).toBe(true)
  })
})

describe('requestFromDraft', () => {
  it('sends numbers, a trimmed name, the version and no load for an empty one', () => {
    const draft = draftFromPlan(PLAN, 'KG', 'en')
    draft.sessions[0].name = '  Lower  '

    const request = requestFromDraft(draft, 2, 'KG')

    expect(request).toMatchObject({ version: 2, goal: 'LOSE_FAT', weeks: 8 })
    expect(request.sessions[0].name).toBe('Lower')
    expect(request.sessions[0].entries[0]).toEqual({
      exerciseId: 'squat',
      sets: 3,
      amount: 5,
      loadKg: 62.5,
      rpe: 8,
      restSeconds: 120,
    })
    expect(request.sessions[0].entries[1].loadKg).toBeUndefined()
  })

  // 62.5 kg reads as 137.8 lb, which would come back as 62.5 only by luck
  it('sends an untouched load unchanged, in any unit', () => {
    const draft = draftFromPlan(
      { ...PLAN, sessions: [{ ...PLAN.sessions[0] }] },
      'LB',
      'en',
    )
    draft.sessions[0].entries[0].stored = { kg: 62.4, text: '137.8' }

    expect(requestFromDraft(draft, 2, 'LB').sessions[0].entries[0].loadKg).toBe(
      62.4,
    )
  })

  it('converts a load the trainer typed, with a comma or a dot', () => {
    const draft = draftFromPlan(PLAN, 'LB', 'pl')
    draft.sessions[0].entries[0].load = '100,5'

    expect(requestFromDraft(draft, 2, 'LB').sessions[0].entries[0].loadKg).toBe(
      45.6,
    )
  })
})

describe('dayName', () => {
  it('names the day in the UI language', () => {
    expect(dayName('MONDAY', 'en')).toBe('Monday')
    expect(dayName('SUNDAY', 'pl')).toBe('niedziela')
    expect(dayName('WEDNESDAY', 'en', 'short')).toBe('Wed')
  })
})

describe('moveEntry', () => {
  const entries = draftFromPlan(PLAN, 'KG', 'en').sessions[0].entries

  it('swaps the row with its neighbour', () => {
    expect(moveEntry(entries, 0, 1).map((entry) => entry.exerciseId)).toEqual([
      'plank',
      'squat',
    ])
  })

  it('moves a row past several others, keeping their order', () => {
    expect(
      moveEntry([...entries, entries[0]], 2, 0).map(
        (entry) => entry.exerciseId,
      ),
    ).toEqual(['squat', 'squat', 'plank'])
  })
})

describe('validateDraft', () => {
  const valid = (): Draft => {
    const draft = draftFromPlan(PLAN, 'KG', 'en')
    draft.sessions.push({
      key: 'second',
      name: 'Upper',
      day: 'FRIDAY',
      entries: [entryFromExercise(PLANK)],
    })
    return draft
  }
  const messages = (draft: Draft, level: 'draft' | 'complete' = 'draft') =>
    validateDraft(draft, level, 'KG').map((problem) => problem.message)

  it('finds nothing in a complete plan', () => {
    expect(messages(valid(), 'complete')).toEqual([])
  })

  it.each([
    ['sets', '0', 'errors.plan.sets-out-of-range'],
    ['sets', '21', 'errors.plan.sets-out-of-range'],
    ['sets', '', 'errors.plan.sets-out-of-range'],
    ['amount', '0', 'errors.plan.amount-out-of-range'],
    ['amount', '3601', 'errors.plan.amount-out-of-range'],
    ['rpe', '4', 'errors.plan.rpe-out-of-range'],
    ['rpe', '11', 'errors.plan.rpe-out-of-range'],
    ['rpe', '8.5', 'errors.plan.rpe-out-of-range'],
    ['rest', '-1', 'errors.plan.rest-out-of-range'],
    ['rest', '601', 'errors.plan.rest-out-of-range'],
    ['load', '0', 'plan.problems.load'],
    ['load', '1000', 'plan.problems.load'],
    ['load', '8.55', 'plan.problems.load'],
    ['load', 'heavy', 'plan.problems.load'],
  ] as const)('refuses %s of "%s"', (field, value, message) => {
    const draft = valid()
    draft.sessions[0].entries[0][field] = value

    expect(messages(draft)).toEqual([message])
  })

  it.each([
    ['sets', '1'],
    ['sets', '20'],
    ['amount', '3600'],
    ['rpe', '5'],
    ['rpe', '10'],
    ['rest', '0'],
    ['rest', '600'],
    ['load', ''],
    ['load', '999.9'],
    ['load', '82,5'],
  ] as const)('accepts %s of "%s"', (field, value) => {
    const draft = valid()
    draft.sessions[0].entries[0][field] = value

    expect(messages(draft)).toEqual([])
  })

  it('says the unit and the limit after conversion for a load in pounds', () => {
    const draft = valid()
    draft.sessions[0].entries[0].load = '2205'

    expect(validateDraft(draft, 'draft', 'LB')[0]).toMatchObject({
      message: 'plan.problems.load',
      values: { max: 2204.4, unit: 'lb' },
    })
    draft.sessions[0].entries[0].load = '0.1'
    expect(validateDraft(draft, 'draft', 'LB')).toHaveLength(1)
  })

  it('treats a name of only spaces as empty and one of 61 characters as too long', () => {
    const draft = valid()
    draft.sessions[0].name = '   '
    expect(messages(draft)).toEqual(['errors.plan.session-name-invalid'])

    draft.sessions[0].name = 'x'.repeat(61)
    expect(messages(draft)).toEqual(['errors.plan.session-name-invalid'])

    draft.sessions[0].name = ` ${'x'.repeat(60)} `
    expect(messages(draft)).toEqual([])
  })

  it.each(['3', '17', '', '12.5'])('refuses a length of "%s"', (weeks) => {
    expect(messages({ ...valid(), weeks })).toEqual([
      'errors.plan.weeks-out-of-range',
    ])
  })

  it('asks for completeness only at that level, each with its way to fix it', () => {
    const draft = newDraft('LOSE_FAT')
    expect(validateDraft(draft, 'draft', 'KG')).toEqual([])
    expect(validateDraft(draft, 'complete', 'KG')).toEqual([
      {
        message: 'errors.plan.too-few-sessions',
        target: target.addSession,
      },
    ])

    const empty = valid()
    empty.sessions[1].entries = []
    expect(validateDraft(empty, 'draft', 'KG')).toEqual([])
    expect(validateDraft(empty, 'complete', 'KG')).toEqual([
      {
        message: 'plan.problems.emptySession',
        session: 'second',
        target: target.addExercise('second'),
      },
    ])
  })

  it('points a target problem at its session, row and field', () => {
    const draft = valid()
    const entry = draft.sessions[0].entries[1]
    entry.rpe = '11'

    expect(validateDraft(draft, 'draft', 'KG')[0]).toMatchObject({
      session: draft.sessions[0].key,
      entry: entry.key,
      target: target.field(entry.key, 'rpe'),
    })
  })
})

describe('problemsFromServer', () => {
  const draft = draftFromPlan(PLAN, 'KG', 'en')

  it('finds the row of a field the backend names by its place', () => {
    const [entry] = draft.sessions[0].entries
    expect(
      problemsFromServer(
        {
          code: 'validation.failed',
          errors: [
            { field: 'sessions[0].entries[0].restSeconds', code: 'max' },
          ],
        },
        draft,
        {},
      ),
    ).toEqual([
      {
        message: 'errors.plan.rest-out-of-range',
        session: draft.sessions[0].key,
        entry: entry.key,
        target: target.field(entry.key, 'rest'),
      },
    ])
  })

  it('falls back to the generic text for a field it does not know', () => {
    expect(
      problemsFromServer(
        { code: 'validation.failed', errors: [{ field: 'goal', code: 'x' }] },
        draft,
        {},
      ),
    ).toEqual([{ message: 'errors.validation.failed', values: {} }])
  })

  it('gives the current week of a refused shortening and links the length', () => {
    expect(
      problemsFromServer(
        { code: 'plan.weeks-before-current', currentWeek: 6 },
        draft,
        {},
      ),
    ).toEqual([
      {
        message: 'errors.plan.weeks-before-current',
        values: { currentWeek: 6 },
        target: target.weeks,
      },
    ])
  })

  it('translates by code and never uses text it does not know', () => {
    expect(
      problemsFromServer({ code: 'plan.changed' }, draft, { name: 'Ola' }),
    ).toEqual([{ message: 'errors.plan.changed', values: { name: 'Ola' } }])
    expect(
      problemsFromServer(
        { code: 'unknown.thing', detail: 'internal' },
        draft,
        {},
      ),
    ).toEqual([{ message: 'errors.generic', values: {} }])
    expect(problemsFromServer(new TypeError('net'), draft, {})).toEqual([
      { message: 'errors.generic', values: {} },
    ])
  })
})
