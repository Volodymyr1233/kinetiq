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
  newDraft,
  removeSession,
  requestFromDraft,
  sessionTotals,
  updateSession,
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
