import { useEffect, useLayoutEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { Message } from '../api/messaging'
import { dayLabel, sameDay, timeOfDay } from '../message-time'
import type { OutboxEntry } from '../use-outbox'

// how near the bottom still counts as reading the newest message, in px
const NEAR_BOTTOM = 48

type Line = {
  id: string
  text: string
  mine: boolean
  at: Date
  state: 'sent' | OutboxEntry['status']
}

function linesOf(messages: Message[], outbox: OutboxEntry[], now: Date) {
  const ids = new Set(messages.map((message) => message.id))
  return [
    ...messages.map((message): Line => ({
      id: message.id,
      text: message.text,
      mine: message.mine,
      at: new Date(message.sentAt),
      state: 'sent',
    })),
    // an attempt whose answer was lost can already be back from the server; it shows once
    ...outbox
      .filter((entry) => !ids.has(entry.id))
      .map((entry): Line => ({
        id: entry.id,
        text: entry.text,
        mine: true,
        at: now,
        state: entry.status,
      })),
  ]
}

/** The messages of a conversation, oldest to newest, with their days and delivery states. It scrolls by itself. */
export function MessageList({
  messages,
  outbox,
  hasMore,
  loadingEarlier = false,
  earlierFailed = false,
  onLoadEarlier,
  onRetry,
  peerName,
  now,
}: {
  messages: Message[]
  outbox: OutboxEntry[]
  hasMore: boolean
  loadingEarlier?: boolean
  earlierFailed?: boolean
  onLoadEarlier: () => void
  onRetry: (id: string) => void
  peerName: string
  now: Date
}) {
  const { t, i18n } = useTranslation()
  const list = useRef<HTMLDivElement>(null)
  const content = useRef<HTMLDivElement>(null)
  const followsNewest = useRef(true)
  const lastScrollTop = useRef(0)
  const heightBeforeEarlier = useRef<number | null>(null)
  const outboxSize = useRef(outbox.length)
  const lines = linesOf(messages, outbox, now)
  const firstId = lines[0]?.id
  const lastId = lines[lines.length - 1]?.id
  const lastState = lines[lines.length - 1]?.state

  // earlier messages arrived above: keep the distance from the bottom, so the reader stays on the same message
  useLayoutEffect(() => {
    const element = list.current
    if (element && heightBeforeEarlier.current !== null) {
      element.scrollTop = element.scrollHeight - heightBeforeEarlier.current
      heightBeforeEarlier.current = null
      // the button is gone once nothing earlier is left, and keyboard focus must not fall out of the page
      if (document.activeElement === document.body) element.focus()
    }
  }, [firstId])

  useLayoutEffect(() => {
    const element = list.current
    // writing a message always brings the reader back to the newest
    if (outbox.length > outboxSize.current) followsNewest.current = true
    outboxSize.current = outbox.length
    if (element && followsNewest.current) {
      element.scrollTop = element.scrollHeight
    }
    // the state too: "Not sent" with its Retry is taller than "Sending…"
  }, [lastId, lastState, outbox.length])

  // a growing composer or an on-screen keyboard shrinks the list, and a late web font makes its content taller
  useEffect(() => {
    const element = list.current
    if (!element || !content.current) return
    const observer = new ResizeObserver(() => {
      if (followsNewest.current) element.scrollTop = element.scrollHeight
    })
    observer.observe(element)
    observer.observe(content.current)
    return () => observer.disconnect()
  }, [])

  return (
    <div
      ref={list}
      role="log"
      aria-live="polite"
      aria-busy={loadingEarlier}
      aria-label={t('messaging.logLabel', { name: peerName })}
      // scrollable content must be reachable by keyboard
      tabIndex={0}
      // relative, because hidden speaker labels would otherwise escape the overflow and scroll the whole page
      onScroll={({ currentTarget: element }) => {
        const distance =
          element.scrollHeight - element.scrollTop - element.clientHeight
        // only moving up leaves the newest: content that grew below, as with a late web font, does not
        if (distance < NEAR_BOTTOM) followsNewest.current = true
        else if (element.scrollTop < lastScrollTop.current)
          followsNewest.current = false
        lastScrollTop.current = element.scrollTop
      }}
      className="focus-visible:ring-ring/50 relative min-h-0 flex-1 overflow-y-auto px-4 py-3 outline-none focus-visible:ring-3 focus-visible:ring-inset"
    >
      <div ref={content} className="flex flex-col gap-1.5">
        {hasMore && (
          <Button
            variant="outline"
            size="sm"
            className="min-h-12 shrink-0 self-center"
            disabled={loadingEarlier}
            onClick={() => {
              const element = list.current
              followsNewest.current = false
              heightBeforeEarlier.current = element
                ? element.scrollHeight - element.scrollTop
                : null
              onLoadEarlier()
            }}
          >
            {t('messaging.loadEarlier')}
          </Button>
        )}
        {earlierFailed && (
          <p role="alert" className="text-destructive text-center text-sm">
            {t('messaging.errors.loadEarlier')}
          </p>
        )}
        {lines.map((line, index) => {
          const previous = lines[index - 1]
          const next = lines[index + 1]
          const endsRun =
            next === undefined ||
            next.mine !== line.mine ||
            next.state !== line.state ||
            !sameDay(next.at, line.at)
          const time = timeOfDay(line.at, i18n.language)
          return (
            <div key={line.id} className="flex shrink-0 flex-col gap-1.5">
              {(previous === undefined || !sameDay(previous.at, line.at)) && (
                <p className="text-muted-foreground my-2 text-center text-xs">
                  {dayLabel(line.at, now, i18n.language, t)}
                </p>
              )}
              <p
                className={cn(
                  'rounded-xl px-3.5 py-2.5 text-[15px]/[21px] wrap-anywhere whitespace-pre-wrap',
                  'max-w-[80%]',
                  line.mine
                    ? 'bg-primary text-primary-foreground self-end rounded-br-sm'
                    : 'bg-muted self-start rounded-bl-sm',
                  line.state === 'sending' && 'opacity-70',
                  line.state === 'failed' &&
                    'outline-destructive outline-[1.5px] outline-offset-2',
                )}
              >
                <span className="sr-only">
                  {line.mine
                    ? t('messaging.speakerMe')
                    : t('messaging.speaker', { name: peerName })}{' '}
                </span>
                {line.text}
              </p>
              {line.state === 'sending' && (
                <p className="text-muted-foreground self-end text-xs">
                  {t('messaging.sending')}
                </p>
              )}
              {line.state === 'failed' && (
                <p className="text-destructive flex items-center gap-1 self-end text-xs">
                  {t('messaging.notSent')}
                  <Button
                    variant="link"
                    size="sm"
                    className="text-destructive min-h-12 px-1 text-xs underline"
                    onClick={() => {
                      onRetry(line.id)
                      // the button goes away, and keyboard focus must not fall out of the page
                      list.current?.focus()
                    }}
                  >
                    {t('messaging.retry')}
                  </Button>
                </p>
              )}
              {line.state === 'sent' && endsRun && (
                <p
                  className={cn(
                    'text-muted-foreground text-xs',
                    line.mine ? 'self-end' : 'self-start',
                  )}
                >
                  {line.mine
                    ? t('messaging.sent', { time })
                    : t('messaging.from', { name: peerName, time })}
                </p>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
