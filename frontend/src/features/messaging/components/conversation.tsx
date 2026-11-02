import { CircleAlertIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import { useMessages } from '../api/messaging'
import { useMarkReadWhileVisible } from '../use-mark-read-while-visible'
import type { OutboxEntry } from '../use-outbox'
import { Composer } from './composer'
import { MessageList } from './message-list'

// "Marek Nowak" talks as "Marek", as on the boards
const firstName = (name: string) => name.trim().split(/\s+/)[0] ?? name

/** One open conversation: its messages and the composer, for either person. It marks itself read while on screen. */
export function Conversation({
  clientId,
  name,
  unread,
  outbox,
  onSend,
  onRetry,
  composerHint,
}: {
  clientId: string
  name: string
  unread: number
  outbox: OutboxEntry[]
  onSend: (text: string) => void
  onRetry: (id: string) => void
  composerHint?: string
}) {
  const { t } = useTranslation()
  const query = useMessages(clientId)
  const peerName = firstName(name)
  useMarkReadWhileVisible(clientId, unread)
  // the newest page comes first in the cache and last on screen
  const messages = [...(query.data?.pages ?? [])]
    .reverse()
    .flatMap((page) => page.messages)

  return (
    <>
      {query.data === undefined && query.isError ? (
        <div className="min-h-0 flex-1 p-4">
          <Alert variant="destructive">
            <CircleAlertIcon aria-hidden />
            <AlertDescription>
              {t(errorMessageKey(query.error))}
            </AlertDescription>
            <AlertAction>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void query.refetch()}
              >
                {t('messaging.errors.retry')}
              </Button>
            </AlertAction>
          </Alert>
        </div>
      ) : query.data === undefined ? (
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
      <Composer name={peerName} hint={composerHint} onSend={onSend} />
    </>
  )
}
