import { useEffect, useState } from 'react'
import { useRequestConfirmationLink } from './api/account'

// matches the backend's one email per account per minute
const RESEND_LOCK_MS = 60_000

/** Requests a new confirmation link and keeps the button locked for a minute after each one. */
export function useResendLink() {
  const request = useRequestConfirmationLink()
  const [locked, setLocked] = useState(false)

  useEffect(() => {
    if (!locked) return
    const timer = setTimeout(() => setLocked(false), RESEND_LOCK_MS)
    return () => clearTimeout(timer)
  }, [locked])

  return {
    request,
    locked,
    send: (email: string) =>
      request.mutate(email, { onSuccess: () => setLocked(true) }),
  }
}
