import { errorMessageKey, problemCode } from '@/lib/api/problem'
import type { Exercise } from '@/lib/exercise-library'
import { fromKg, parseWeight, toKg, type WeightUnit } from '@/lib/weight'
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
// the smallest load above 0 at the stored precision of 0.1 kg
const MIN_LOAD_KG = 0.1
const MAX_LOAD_KG = 999.9

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

/** The elements a problem can send the trainer to; the keys make them unique, and the page has one editor. */
export const target = {
  weeks: 'plan-weeks',
  addSession: 'plan-add-session',
  summary: 'plan-summary',
  save: 'plan-save',
  name: (session: string) => `plan-name-${session}`,
  addExercise: (session: string) => `plan-add-exercise-${session}`,
  field: (entry: string, field: EntryField) => `plan-${field}-${entry}`,
  move: (entry: string) => `plan-move-${entry}`,
  remove: (entry: string) => `plan-remove-${entry}`,
}

export type EntryField = 'sets' | 'amount' | 'load' | 'rpe' | 'rest'

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

/** Moves the row one place up (-1) or down (+1); a move past either end changes nothing. */
export function moveEntry(
  entries: DraftEntry[],
  from: number,
  to: number,
): DraftEntry[] {
  const moved = [...entries]
  moved.splice(to, 0, ...moved.splice(from, 1))
  return moved
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

/** One thing to fix: its text and values, where it is, and the element that fixes it. */
export type Problem = {
  message: string
  values?: Record<string, string | number>
  session?: string
  entry?: string
  target?: string
}

const inRange = (text: string, min: number, max: number) => {
  const trimmed = text.trim()
  return (
    /^\d+$/.test(trimmed) && Number(trimmed) >= min && Number(trimmed) <= max
  )
}

// the limit holds in kilograms, as loads are stored, so in pounds it is 2204.4 and 0.1 lb rounds to nothing
function validLoad(entry: DraftEntry, unit: WeightUnit) {
  const text = entry.load.trim()
  if (!text || entry.stored?.text === entry.load) return true
  const value = parseWeight(text)
  if (value === undefined) return false
  const kg = toKg(value, unit)
  return kg > 0 && kg <= MAX_LOAD_KG
}

function entryProblems(
  session: DraftSession,
  entry: DraftEntry,
  unit: WeightUnit,
): Problem[] {
  const checks: [EntryField, boolean, string, Problem['values']?][] = [
    ['sets', inRange(entry.sets, 1, 20), 'errors.plan.sets-out-of-range'],
    [
      'amount',
      inRange(entry.amount, 1, 3600),
      'errors.plan.amount-out-of-range',
    ],
    [
      'load',
      validLoad(entry, unit),
      'plan.problems.load',
      {
        min: fromKg(MIN_LOAD_KG, unit),
        max: fromKg(MAX_LOAD_KG, unit),
        unit: unit.toLowerCase(),
      },
    ],
    ['rpe', inRange(entry.rpe, 5, 10), 'errors.plan.rpe-out-of-range'],
    ['rest', inRange(entry.rest, 0, 600), 'errors.plan.rest-out-of-range'],
  ]
  return checks
    .filter(([, valid]) => !valid)
    .map(([field, , message, values]) => ({
      message,
      values,
      session: session.key,
      entry: entry.key,
      target: target.field(entry.key, field),
    }))
}

/**
 * What stops this draft from being saved, or with `complete` from being published or saved as a published plan. The
 * rules are the backend's, repeated so that a trainer sees all problems at once and next to their fields.
 */
export function validateDraft(
  draft: Draft,
  level: 'draft' | 'complete',
  unit: WeightUnit,
): Problem[] {
  const problems: Problem[] = []
  if (!inRange(draft.weeks, 4, 16)) {
    problems.push({
      message: 'errors.plan.weeks-out-of-range',
      target: target.weeks,
    })
  }
  if (level === 'complete' && draft.sessions.length < 2) {
    problems.push({
      message: 'errors.plan.too-few-sessions',
      target: target.addSession,
    })
  }
  for (const session of draft.sessions) {
    if (!session.name.trim() || session.name.trim().length > NAME_MAX_LENGTH) {
      problems.push({
        message: 'errors.plan.session-name-invalid',
        session: session.key,
        target: target.name(session.key),
      })
    }
    if (level === 'complete' && session.entries.length === 0) {
      problems.push({
        message: 'plan.problems.emptySession',
        session: session.key,
        target: target.addExercise(session.key),
      })
    }
    for (const entry of session.entries) {
      problems.push(...entryProblems(session, entry, unit))
    }
  }
  return problems
}

const SERVER_ENTRY_FIELDS: Record<string, [EntryField, string]> = {
  sets: ['sets', 'errors.plan.sets-out-of-range'],
  amount: ['amount', 'errors.plan.amount-out-of-range'],
  loadKg: ['load', 'errors.plan.load-out-of-range'],
  rpe: ['rpe', 'errors.plan.rpe-out-of-range'],
  restSeconds: ['rest', 'errors.plan.rest-out-of-range'],
}

// the backend names a field by its place in the request, which follows the draft's order
function problemFromField(field: string, draft: Draft): Problem | undefined {
  if (field === 'weeks') {
    return {
      message: 'errors.plan.weeks-out-of-range',
      target: target.weeks,
    }
  }
  const place = field.match(
    /^sessions\[(\d+)]\.(?:name|entries\[(\d+)]\.(\w+))$/,
  )
  const session = place && draft.sessions[Number(place[1])]
  if (!session) return undefined
  if (place[2] === undefined) {
    return {
      message: 'errors.plan.session-name-invalid',
      session: session.key,
      target: target.name(session.key),
    }
  }
  const entry = session.entries[Number(place[2])]
  const known = SERVER_ENTRY_FIELDS[place[3]]
  return entry && known
    ? {
        message: known[1],
        session: session.key,
        entry: entry.key,
        target: target.field(entry.key, known[0]),
      }
    : undefined
}

/** A refusal from the server as problems, translated by its code; server text is never shown. */
export function problemsFromServer(
  error: unknown,
  draft: Draft,
  values: Record<string, string | number>,
): Problem[] {
  const code = problemCode(error)
  const detail =
    typeof error === 'object' && error !== null
      ? (error as { errors?: { field: string }[]; currentWeek?: number })
      : {}
  if (code === 'validation.failed' && Array.isArray(detail.errors)) {
    const found = detail.errors.map((fieldError) =>
      problemFromField(fieldError.field, draft),
    )
    if (found.length > 0 && found.every(Boolean)) return found as Problem[]
  }
  if (code === 'plan.weeks-before-current') {
    return [
      {
        message: errorMessageKey(error),
        values: { currentWeek: detail.currentWeek ?? 0 },
        target: target.weeks,
      },
    ]
  }
  return [{ message: errorMessageKey(error), values }]
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
