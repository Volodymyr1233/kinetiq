import { fromKg, parseWeight, toKg, type WeightUnit } from '@/lib/weight'
import { draftSchema, emptyDraft, type QuizDraft } from './quiz-schema'

const KEY = 'kinetiq.quiz'

// the tab's storage only: answers survive a reload but not closing the tab (spec, Clarifications)
export function loadDraft(unit: WeightUnit): QuizDraft {
  try {
    const stored = sessionStorage.getItem(KEY)
    return stored
      ? inUnit(draftSchema.parse(JSON.parse(stored)), unit)
      : emptyDraft
  } catch {
    return emptyDraft
  }
}

export function saveDraft(draft: QuizDraft) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(draft))
  } catch {
    // storage can be unavailable (private mode, quota); the quiz then works in memory only
  }
}

/** Removes the draft once the answers are stored on the server (research R11). */
export function clearDraft() {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // storage can be unavailable (private mode); then there is no draft to remove
  }
}

// a weight typed before the person switched units shows in the new one (spec 42, User Story 4); a draft kept
// without a unit was typed in the unit it is read in
function inUnit(draft: QuizDraft, unit: WeightUnit): QuizDraft {
  const typed = parseWeight(draft.answers.weight)
  if (typed === undefined || !draft.weightUnit || draft.weightUnit === unit) {
    return draft
  }
  const converted = String(fromKg(toKg(typed, draft.weightUnit), unit))
  const weight = draft.answers.weight.includes(',')
    ? converted.replace('.', ',')
    : converted
  return { ...draft, answers: { ...draft.answers, weight }, weightUnit: unit }
}
