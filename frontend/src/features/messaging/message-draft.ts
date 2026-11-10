import type { About } from './api/messaging'

/** What the composer holds for a conversation and was not sent yet: its text, and the set it will be about. */
export type MessageDraft = { text: string; about?: About }

const EMPTY: MessageDraft = { text: '' }

const keyOf = (clientId: string) => `kinetiq.message-draft.${clientId}`

// the tab's storage, which belongs to one account: a draft outlives a change of tab, a link and a reload, but not
// closing the tab or signing out
export function loadMessageDraft(clientId: string): MessageDraft {
  try {
    const stored: unknown = JSON.parse(
      sessionStorage.getItem(keyOf(clientId)) ?? 'null',
    )
    return typeof stored === 'object' &&
      stored !== null &&
      'text' in stored &&
      typeof stored.text === 'string'
      ? (stored as MessageDraft)
      : EMPTY
  } catch {
    return EMPTY
  }
}

export function saveMessageDraft(clientId: string, draft: MessageDraft) {
  try {
    if (draft.text === '' && draft.about === undefined) {
      sessionStorage.removeItem(keyOf(clientId))
    } else {
      sessionStorage.setItem(keyOf(clientId), JSON.stringify(draft))
    }
  } catch {
    // storage can be unavailable (private mode, quota); the draft then lasts as long as the composer
  }
}
