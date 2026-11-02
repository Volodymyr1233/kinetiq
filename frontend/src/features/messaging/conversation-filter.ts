import type { Conversation } from './api/messaging'

// NFD splits "ń" into "n" and a combining mark; "ł" has no decomposition, so it is mapped by hand
function normalised(text: string) {
  return text
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replaceAll('ł', 'l')
}

/** Keeps the conversations whose client name holds the query, ignoring case and diacritics. It never looks at messages. */
export function filterConversations(
  conversations: Conversation[],
  query: string,
) {
  const needle = normalised(query)
  return conversations.filter((conversation) =>
    normalised(conversation.name).includes(needle),
  )
}
