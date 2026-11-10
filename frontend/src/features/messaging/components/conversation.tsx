import { useTranslation } from 'react-i18next'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { firstName } from '@/lib/first-name'
import { messagesOf, useMessages, type About } from '../api/messaging'
import { useFirstUnread } from '../use-first-unread'
import type { OutboxEntry } from '../use-outbox'
import { Composer } from './composer'
import { LoadError } from './load-error'
import { MessageList } from './message-list'

/** One open conversation: its messages and the composer, for either person. It marks itself read while on screen. */
export function Conversation({
  clientId,
  name,
  outbox,
  onSend,
  onRetry,
  composerHint,
  ask,
}: {
  clientId: string
  name: string
  outbox: OutboxEntry[]
  onSend: (text: string, about?: About) => void
  onRetry: (id: string) => void
  composerHint?: string
  ask?: About
}) {
  const { t } = useTranslation()
  const query = useMessages(clientId)
  const peerName = firstName(name)
  const { firstUnreadId, positioning } = useFirstUnread(clientId, query)
  const messages = messagesOf(query.data)

  return (
    <>
      {query.data === undefined && query.isError ? (
        <div className="min-h-0 flex-1 p-4">
          <LoadError error={query.error} onRetry={() => void query.refetch()} />
        </div>
      ) : query.data === undefined || positioning ? (
        <div className="flex min-h-0 flex-1 flex-col gap-2 p-4">
          <span role="status" className="sr-only">
            {t('messaging.loading')}
          </span>
          <Skeleton aria-hidden className="h-10 w-3/5" />
          <Skeleton aria-hidden className="h-10 w-1/2 self-end" />
          <Skeleton aria-hidden className="h-16 w-3/4" />
        </div>
      ) : messages.length === 0 && outbox.length === 0 ? (
        <Empty className="min-h-0 flex-1">
          <EmptyHeader>
            <EmptyTitle>{t('messaging.empty.title')}</EmptyTitle>
            <EmptyDescription>
              {t('messaging.empty.body', { name: peerName })}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <MessageList
          messages={messages}
          firstUnreadId={firstUnreadId}
          outbox={outbox}
          hasMore={query.hasNextPage}
          loadingEarlier={query.isFetchingNextPage}
          earlierFailed={query.isFetchNextPageError}
          onLoadEarlier={() => void query.fetchNextPage()}
          onRetry={onRetry}
          peerName={peerName}
          // the moment of the last refresh, so "Today" turns over with the next one
          now={new Date(query.dataUpdatedAt)}
        />
      )}
      <Composer
        name={peerName}
        hint={composerHint}
        draftKey={clientId}
        ask={ask}
        onSend={onSend}
      />
    </>
  )
}
