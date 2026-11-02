import { CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AccountAvatar } from '@/components/account-avatar'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import { useConversations, useSendMessage } from '../api/messaging'
import { Conversation } from '../components/conversation'
import { useOutbox } from '../use-outbox'

export function ClientMessagesPage() {
  const { t } = useTranslation()
  const conversations = useConversations()
  const { mutateAsync: send } = useSendMessage()
  const outbox = useOutbox(({ conversation, id, text }) =>
    send({ clientId: conversation, id, text }),
  )
  // a client has one conversation, with their trainer, so the tab opens straight into it
  const conversation = conversations.data?.conversations[0]

  if (
    conversations.data !== undefined ? !conversation : conversations.isError
  ) {
    return (
      <div className="p-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>
            {t(errorMessageKey(conversations.error))}
          </AlertDescription>
          <AlertAction>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void conversations.refetch()}
            >
              {t('messaging.errors.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  if (!conversation) {
    return (
      <div className="flex flex-col gap-3 p-4">
        <span role="status" className="sr-only">
          {t('messaging.loading')}
        </span>
        <Skeleton aria-hidden className="h-10 w-48" />
        <Skeleton aria-hidden className="h-10 w-3/5" />
        <Skeleton aria-hidden className="h-10 w-1/2 self-end" />
      </div>
    )
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <header className="flex shrink-0 items-center gap-3 border-b px-4 pb-3">
        <AccountAvatar name={conversation.name} />
        <div className="min-w-0">
          <h2 className="truncate font-semibold">{conversation.name}</h2>
          <p className="text-muted-foreground text-sm">
            {t('messaging.yourTrainer')}
          </p>
        </div>
      </header>
      <Conversation
        clientId={conversation.clientId}
        name={conversation.name}
        unread={conversation.unreadCount}
        outbox={outbox.entries}
        onSend={(text) => outbox.submit(conversation.clientId, text)}
        onRetry={outbox.retry}
      />
    </div>
  )
}
