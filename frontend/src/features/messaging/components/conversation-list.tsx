import { SearchIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { AccountAvatar } from '@/components/account-avatar'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from '@/components/ui/input-group'
import { cn } from '@/lib/utils'
import type { Conversation } from '../api/messaging'
import { filterConversations } from '../conversation-filter'
import { listTime } from '../message-time'
import { UnreadCount } from './unread-count'

/** The trainer's conversations with a search by client name; each row is the address of its conversation. */
export function ConversationList({
  conversations,
  openId,
  now,
}: {
  conversations: Conversation[]
  openId?: string
  now: Date
}) {
  const { t, i18n } = useTranslation()
  const [query, setQuery] = useState('')
  const shown = filterConversations(conversations, query)

  return (
    <nav
      aria-label={t('messaging.trainer.listLabel')}
      className="flex min-h-0 flex-1 flex-col"
    >
      <div className="p-4">
        <InputGroup>
          <InputGroupAddon>
            <SearchIcon aria-hidden />
          </InputGroupAddon>
          <InputGroupInput
            type="search"
            aria-label={t('messaging.trainer.searchLabel')}
            placeholder={t('messaging.trainer.searchPlaceholder')}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </InputGroup>
      </div>
      {shown.length === 0 && (
        <p role="status" className="text-muted-foreground px-4 text-sm">
          {t('messaging.trainer.noMatch')}
        </p>
      )}
      <ul className="min-h-0 flex-1 overflow-y-auto">
        {shown.map((conversation) => {
          const { clientId, name, unreadCount, lastMessage } = conversation
          const unread = unreadCount > 0
          return (
            <li key={clientId}>
              <Link
                to={`/trainer/messages/${clientId}`}
                aria-current={clientId === openId ? 'true' : undefined}
                className={cn(
                  'hover:bg-accent focus-visible:ring-ring/50 grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-0.5 border-b px-4 py-3 outline-none focus-visible:ring-3 focus-visible:ring-inset',
                  clientId === openId && 'bg-accent',
                )}
              >
                <span className="row-span-2">
                  <AccountAvatar name={name} />
                </span>
                <span
                  className={cn(
                    'col-start-2 row-start-1 truncate text-sm',
                    unread ? 'font-semibold' : 'font-medium',
                  )}
                >
                  {name}
                </span>
                {/* right after the name in the document, so a screen reader hears "Anna, 2 unread" first */}
                <span className="col-start-3 row-start-2 flex justify-end">
                  <UnreadCount count={unreadCount} />
                </span>
                <span
                  className={cn(
                    'col-start-2 row-start-2 truncate text-sm',
                    unread ? 'font-medium' : 'text-muted-foreground',
                  )}
                >
                  {!lastMessage
                    ? t('messaging.trainer.noMessages')
                    : lastMessage.sentByMe
                      ? t('messaging.trainer.you', {
                          text: lastMessage.preview,
                        })
                      : lastMessage.preview}
                </span>
                {lastMessage && (
                  <span className="text-muted-foreground col-start-3 row-start-1 justify-self-end text-xs">
                    {listTime(
                      new Date(lastMessage.sentAt),
                      now,
                      i18n.language,
                      t,
                    )}
                  </span>
                )}
              </Link>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
