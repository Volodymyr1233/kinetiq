import type { Notification } from './api/notifications'

export type Area = 'client' | 'trainer'

/** The screen a notification is about in an area, or none when it points nowhere. */
export function targetOf(
  area: Area,
  notification: Pick<Notification, 'kind' | 'clientId'>,
): string | undefined {
  const { kind, clientId } = notification
  if (area === 'client') {
    return kind === 'NEW_MESSAGE' ? '/app/messages' : '/app/plan'
  }
  switch (kind) {
    case 'NEW_MESSAGE':
      return clientId === undefined
        ? '/trainer/messages'
        : `/trainer/messages/${clientId}`
    case 'RECOVERY_DAY':
      return clientId === undefined
        ? '/trainer/clients'
        : `/trainer/clients/${clientId}`
    case 'CLIENT_DELETED':
      return undefined
    default:
      return '/trainer/approvals'
  }
}
