import { fromKg, parseWeight, toKg, type WeightUnit } from '@/lib/weight'
import type { Exercise } from '@/lib/exercise-library'
import type { Plan, SavePlanBody } from './api/plans'

export type Goal = Plan['goal']
export type Day = Plan['sessions'][number]['day']
export type Measure = Plan['sessions'][number]['entries'][number]['measure']

// Records, so the type check fails when a value from the backend is missing here
const GOAL_ORDER = {
  BUILD_MUSCLE: true,
  GET_STRONGER: true,
  LOSE_FAT: true,
  MOVE_BETTER: true,
  GENERAL_FITNESS: true,
} satisfies Record<Goal, true>

const DAY_ORDER = {
  MONDAY: true,
  TUESDAY: true,
  WEDNESDAY: true,
  THURSDAY: true,
  FRIDAY: true,
  SATURDAY: true,
  SUNDAY: true,
} satisfies Record<Day, true>

export const GOALS = Object.keys(GOAL_ORDER) as Goal[]
export const DAYS = Object.keys(DAY_ORDER) as Day[]

export const MAX_SESSIONS = 6
export const MAX_EXERCISES = 30
export const NAME_MAX_LENGTH = 60

const NEW_PLAN_WEEKS = 12
const DEFAULT_RPE = 8
// a repetition counts as this much work when estimating a session's time
const SECONDS_PER_REP = 4

/** One row of a session while it is edited. Numbers are kept as typed, so "8," stays while typing. */
export type DraftEntry = {
  key: string
  exerciseId: string
  exerciseName: string
  measure: Measure
  sets: string
  amount: string
  load: string
  /** The stored load and how it was shown; an untouched load is sent back as it came, so pounds never drift. */
  stored?: { kg: number; text: string }
  rpe: string
  rest: string
}

export type DraftSession = {
  key: string
  name: string
  day: Day
  entries: DraftEntry[]
}

export type Draft = { goal: Goal; weeks: string; sessions: DraftSession[] }

// the API has no ids for sessions and rows, so React and the selection use these
let lastKey = 0
const nextKey = () => `draft-${++lastKey}`

const byDay = (sessions: DraftSession[]) =>
  [...sessions].sort((a, b) => DAYS.indexOf(a.day) - DAYS.indexOf(b.day))

export function formatLoad(kg: number, unit: WeightUnit, language: string) {
  return new Intl.NumberFormat(language, {
    useGrouping: false,
    maximumFractionDigits: 1,
  }).format(fromKg(kg, unit))
}

export function newDraft(goal: Goal): Draft {
  return { goal, weeks: String(NEW_PLAN_WEEKS), sessions: [] }
}

export function draftFromPlan(
  plan: Plan,
  unit: WeightUnit,
  language: string,
): Draft {
  return {
    goal: plan.goal,
    weeks: String(plan.weeks),
    sessions: plan.sessions.map((session) => ({
      key: nextKey(),
      name: session.name,
      day: session.day,
      entries: session.entries.map((entry) => {
        const load =
          entry.loadKg === undefined
            ? ''
            : formatLoad(entry.loadKg, unit, language)
        return {
          key: nextKey(),
          exerciseId: entry.exerciseId,
          exerciseName: entry.exerciseName,
          measure: entry.measure,
          sets: String(entry.sets),
          amount: String(entry.amount),
          load,
          stored:
            entry.loadKg === undefined
              ? undefined
              : { kg: entry.loadKg, text: load },
          rpe: String(entry.rpe),
          rest: String(entry.restSeconds),
        }
      }),
    })),
  }
}

/** The first day of the week without a session; `undefined` when all are taken. */
export function firstFreeDay(sessions: DraftSession[]) {
  return DAYS.find((day) => !sessions.some((session) => session.day === day))
}

/** Adds an unnamed session on the first free day; `key` is the new session's. */
export function addSession(draft: Draft) {
  const key = nextKey()
  const session: DraftSession = {
    key,
    name: '',
    day: firstFreeDay(draft.sessions) ?? 'MONDAY',
    entries: [],
  }
  return {
    key,
    draft: { ...draft, sessions: byDay([...draft.sessions, session]) },
  }
}

/** Removes the session; `selected` is the key of the next one, or the previous when it was the last. */
export function removeSession(draft: Draft, key: string) {
  const index = draft.sessions.findIndex((session) => session.key === key)
  const sessions = draft.sessions.filter((session) => session.key !== key)
  return {
    draft: { ...draft, sessions },
    selected: (sessions[index] ?? sessions[index - 1])?.key,
  }
}

export function updateSession(
  draft: Draft,
  key: string,
  change: Partial<Pick<DraftSession, 'name' | 'day' | 'entries'>>,
): Draft {
  // a session changing its day moves to its place in the week
  return {
    ...draft,
    sessions: byDay(
      draft.sessions.map((session) =>
        session.key === key ? { ...session, ...change } : session,
      ),
    ),
  }
}

export function entryFromExercise(exercise: Exercise): DraftEntry {
  return {
    key: nextKey(),
    exerciseId: exercise.id,
    exerciseName: exercise.name,
    measure: exercise.measure,
    sets: String(exercise.sets),
    amount: String(exercise.amount),
    load: '',
    rpe: String(DEFAULT_RPE),
    rest: String(exercise.restSeconds),
  }
}

const wholeNumber = (text: string) =>
  /^\d+$/.test(text.trim()) ? Number(text.trim()) : 0

/** The session's exercises, total sets and estimated time in whole minutes; a value that is no number counts as 0. */
export function sessionTotals(session: DraftSession) {
  const seconds = session.entries.reduce((sum, entry) => {
    const amount = wholeNumber(entry.amount)
    const work = entry.measure === 'SECONDS' ? amount : amount * SECONDS_PER_REP
    return sum + wholeNumber(entry.sets) * (wholeNumber(entry.rest) + work)
  }, 0)
  return {
    exercises: session.entries.length,
    sets: session.entries.reduce(
      (sum, entry) => sum + wholeNumber(entry.sets),
      0,
    ),
    minutes: Math.round(seconds / 60),
  }
}

// what a person sees: keys are the editor's own, and a save's `stored` follows from the load
function content(draft: Draft) {
  return JSON.stringify({
    goal: draft.goal,
    weeks: draft.weeks,
    sessions: draft.sessions.map((session) => ({
      name: session.name,
      day: session.day,
      entries: session.entries.map((entry) => [
        entry.exerciseId,
        entry.sets,
        entry.amount,
        entry.load,
        entry.rpe,
        entry.rest,
      ]),
    })),
  })
}

/** Tells whether the draft differs from what was last saved, so undoing an edit clears it. */
export function isDirty(draft: Draft, saved: Draft) {
  return content(draft) !== content(saved)
}

function loadKg(entry: DraftEntry, unit: WeightUnit) {
  const text = entry.load.trim()
  if (!text) return undefined
  if (entry.stored?.text === entry.load) return entry.stored.kg
  const value = parseWeight(text)
  return value === undefined ? undefined : toKg(value, unit)
}

export function requestFromDraft(
  draft: Draft,
  version: number | undefined,
  unit: WeightUnit,
): SavePlanBody {
  return {
    version,
    goal: draft.goal,
    weeks: Number(draft.weeks.trim()),
    sessions: draft.sessions.map((session) => ({
      name: session.name.trim(),
      day: session.day,
      entries: session.entries.map((entry) => ({
        exerciseId: entry.exerciseId,
        sets: Number(entry.sets.trim()),
        amount: Number(entry.amount.trim()),
        loadKg: loadKg(entry, unit),
        rpe: Number(entry.rpe.trim()),
        restSeconds: Number(entry.rest.trim()),
      })),
    })),
  }
}

/** The name of a day in the UI language: Monday is 1 January 2024. */
export function dayName(
  day: Day,
  language: string,
  width: 'short' | 'long' = 'long',
) {
  return new Intl.DateTimeFormat(language, {
    weekday: width,
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(2024, 0, 1 + DAYS.indexOf(day))))
}
