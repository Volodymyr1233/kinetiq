import { useCachedAccount } from '@/features/account/api/account'
import { useConversationEvents } from '@/features/messaging/use-conversation-events'

/**
 * Keeps conversations live on every screen of a signed-in person. It sits above the routes, because the quiz's
 * waiting state and the logger are not inside either area; signing out, or another account, ends its stream.
 */
export function ConversationEventsSync() {
  useConversationEvents(useCachedAccount()?.id)
  return null
}
