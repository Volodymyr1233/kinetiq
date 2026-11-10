import { useState } from 'react'
import type { About } from './api/messaging'

export type OutboxEntry = {
  id: string
  conversation: string
  text: string
  // the logged set the message is about, sent again with every retry
  about?: About
  status: 'sending' | 'failed'
}

type Send = (entry: {
  id: string
  conversation: string
  text: string
  about?: About
}) => Promise<unknown>

/**
 * Holds what the person sent until the server confirms it, in the page's memory only: a failed text can hold health
 * details, so it stays out of browser storage and is gone when the page closes (research R8).
 */
export function useOutbox(send: Send) {
  const [entries, setEntries] = useState<OutboxEntry[]>([])

  const attempt = (entry: OutboxEntry) => {
    send(entry).then(
      () =>
        setEntries((current) => current.filter(({ id }) => id !== entry.id)),
      () =>
        setEntries((current) =>
          current.map((other) =>
            other.id === entry.id ? { ...other, status: 'failed' } : other,
          ),
        ),
    )
  }

  return {
    entries,
    submit: (conversation: string, text: string, about?: About) => {
      const entry: OutboxEntry = {
        id: crypto.randomUUID(),
        conversation,
        text,
        about,
        status: 'sending',
      }
      setEntries((current) => [...current, entry])
      attempt(entry)
    },
    // the same id, so the server stores it once even if the first attempt did arrive
    retry: (id: string) => {
      const entry = entries.find((other) => other.id === id)
      if (entry?.status !== 'failed') return
      setEntries((current) =>
        current.map((other) =>
          other.id === id ? { ...other, status: 'sending' } : other,
        ),
      )
      attempt(entry)
    },
  }
}
