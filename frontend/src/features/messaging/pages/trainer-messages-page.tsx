import { ArrowLeftIcon, UserPlusIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { AccountAvatar } from '@/components/account-avatar'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import {
  useConversations,
  useSendMessage,
  type Conversation as Row,
} from '../api/messaging'
import { Conversation } from '../components/conversation'
import { ConversationList } from '../components/conversation-list'
import { LoadError } from '../components/load-error'
import { useOutbox } from '../use-outbox'

function BackToList() {
  const { t } = useTranslation()
  return (
    // a narrow window shows the list or the conversation, never both
    <Button variant="ghost" size="sm" className="self-start md:hidden" asChild>
      <Link to="/trainer/messages">
        <ArrowLeftIcon data-icon="inline-start" aria-hidden="true" />
        {t('messaging.trainer.backToList')}
      </Link>
    </Button>
  )
}

function ConversationHeader({ conversation }: { conversation: Row }) {
  const { t } = useTranslation()
  const { clientId, name, goal, currentWeek, planWeeks } = conversation
  const details = [
    goal && t(`quiz.goal.options.${goal}.title`),
    // a finished plan has no current week to tell
    currentWeek !== undefined &&
      planWeeks !== undefined &&
      currentWeek <= planWeeks &&
      t('plan.week', { week: currentWeek, weeks: planWeeks }),
  ].filter(Boolean)

  return (
    <header className="flex shrink-0 flex-wrap items-center justify-between gap-x-3 gap-y-2 border-b px-4 py-3">
      <BackToList />
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <AccountAvatar name={name} />
        <div className="min-w-0">
          <h2 className="truncate font-semibold">{name}</h2>
          {details.length > 0 && (
            <p className="text-muted-foreground truncate text-sm">
              {details.join(' · ')}
            </p>
          )}
        </div>
      </div>
      <Button variant="outline" size="sm" asChild>
        <Link to={`/trainer/clients/${clientId}`}>
          {t('messaging.trainer.openProfile')}
        </Link>
      </Button>
    </header>
  )
}

export function TrainerMessagesPage() {
  const { t } = useTranslation()
  const { clientId } = useParams()
  const conversations = useConversations()
  const { mutateAsync: send } = useSendMessage()
  // kept here, so a message that failed in one conversation is still there after looking at another
  const outbox = useOutbox(({ conversation, id, text }) =>
    send({ clientId: conversation, id, text }),
  )
  const rows = conversations.data?.conversations
  const open = rows?.find((row) => row.clientId === clientId)

  if (rows?.length === 0) {
    return (
      <div className="flex flex-col gap-6 px-6 py-8">
        <h1 className="sr-only">{t('nav.messages')}</h1>
        <Empty className="bg-muted border border-dashed">
          <EmptyHeader>
            <EmptyTitle>{t('messaging.trainer.noClients.title')}</EmptyTitle>
            <EmptyDescription>
              {t('messaging.trainer.noClients.body')}
            </EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button asChild>
              <Link to="/trainer/clients/invites">
                <UserPlusIcon data-icon="inline-start" aria-hidden="true" />
                {t('messaging.trainer.noClients.action')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      </div>
    )
  }

  const loadError = rows === undefined && conversations.isError && (
    <div className="p-4">
      <LoadError
        error={conversations.error}
        onRetry={() => void conversations.refetch()}
      />
    </div>
  )
  const loading = rows === undefined && !conversations.isError && (
    <div className="flex flex-col gap-3 p-4">
      <span role="status" className="sr-only">
        {t('messaging.trainer.loading')}
      </span>
      {Array.from({ length: 5 }, (_, row) => (
        <Skeleton key={row} aria-hidden className="h-12 w-full" />
      ))}
    </div>
  )

  return (
    <div className="flex h-[calc(100dvh-3.5rem)]">
      <h1 className="sr-only">{t('nav.messages')}</h1>
      <div
        className={cn(
          'w-full flex-col border-r md:flex md:w-80 md:shrink-0',
          clientId === undefined ? 'flex' : 'hidden',
        )}
      >
        {loadError}
        {loading}
        {rows && (
          <ConversationList
            conversations={rows}
            openId={clientId}
            now={new Date(conversations.dataUpdatedAt)}
          />
        )}
      </div>
      <section
        aria-label={
          open
            ? t('messaging.trainer.conversationWith', { name: open.name })
            : undefined
        }
        className={cn(
          'min-w-0 flex-1 flex-col md:flex',
          clientId === undefined ? 'hidden' : 'flex',
        )}
      >
        {clientId === undefined ? (
          <Empty className="flex-1">
            <EmptyHeader>
              <EmptyTitle>{t('messaging.trainer.choose')}</EmptyTitle>
            </EmptyHeader>
          </Empty>
        ) : open ? (
          <>
            <ConversationHeader conversation={open} />
            {/* a new conversation gets its own empty composer and starts at its newest message */}
            <Conversation
              key={open.clientId}
              clientId={open.clientId}
              name={open.name}
              outbox={outbox.entries.filter(
                ({ conversation }) => conversation === open.clientId,
              )}
              onSend={(text) => outbox.submit(open.clientId, text)}
              onRetry={outbox.retry}
              composerHint={t('messaging.trainer.shortcutHint')}
            />
          </>
        ) : rows ? (
          <Empty className="flex-1">
            <EmptyHeader>
              <EmptyTitle>{t('messaging.trainer.notFound.title')}</EmptyTitle>
              <EmptyDescription>
                {t('messaging.trainer.notFound.body')}
              </EmptyDescription>
            </EmptyHeader>
            <EmptyContent>
              <BackToList />
            </EmptyContent>
          </Empty>
        ) : (
          <div className="md:hidden">
            {loadError}
            {loading}
          </div>
        )}
      </section>
    </div>
  )
}
