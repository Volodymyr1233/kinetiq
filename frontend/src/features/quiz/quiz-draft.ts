import { draftSchema, emptyDraft, type QuizDraft } from './quiz-schema'

const KEY = 'kinetiq.quiz'

// the tab's storage only: answers survive a reload but not closing the tab (spec, Clarifications)
export function loadDraft(): QuizDraft {
  try {
    const stored = sessionStorage.getItem(KEY)
    return stored ? draftSchema.parse(JSON.parse(stored)) : emptyDraft
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
