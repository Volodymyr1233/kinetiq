import { useTranslation } from 'react-i18next'
import { AccountAvatar } from '@/components/account-avatar'
import { Skeleton } from '@/components/ui/skeleton'
import { useConversations, useSendMessage, type About } from '../api/messaging'
import { Conversation } from '../components/conversation'
import { LoadError } from '../components/load-error'
import { useOutbox } from '../use-outbox'

/** The client's conversation with their trainer. With `ask`, the next message is about that logged set. */
export function ClientMessagesPage({ ask }: { ask?: About }) {
  const { t } = useTranslation()
  const conversations = useConversations()
  const { mutateAsync: send } = useSendMessage()
  const outbox = useOutbox(({ conversation, id, text, about }) =>
    send({ clientId: conversation, id, text, about }),
  )
  // a client has one conversation, with their trainer, so the tab opens straight into it
  const conversation = conversations.data?.conversations[0]

  if (
    conversations.data !== undefined ? !conversation : conversations.isError
  ) {
    return (
      <div className="p-4">
        <LoadError
          error={conversations.error}
          onRetry={() => void conversations.refetch()}
        />
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
        outbox={outbox.entries}
        ask={ask}
        onSend={(text, about) =>
          outbox.submit(conversation.clientId, text, about)
        }
        onRetry={outbox.retry}
        aboutHref={(set) =>
          `/app/session/${set.sessionLogId}?set=${set.entryPosition}-${set.setNumber}`
        }
      />
    </div>
  )
}
