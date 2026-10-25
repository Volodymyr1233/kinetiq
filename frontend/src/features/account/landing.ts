import type { CurrentAccount } from './api/account'

type Role = CurrentAccount['role']

const AREAS: Record<Role, string> = { TRAINER: '/trainer', CLIENT: '/app' }

export function landingFor(role: Role) {
  return AREAS[role]
}

/** The page to return to after sign-in: only a path inside the person's own area, never another site or role. */
export function safeNext(next: string | null | undefined, role: Role) {
  // "//host" and "/\host" are read by browsers as another site
  if (!next || !next.startsWith('/') || /^\/[/\\]/.test(next)) return undefined
  const area = AREAS[role]
  const inArea =
    next === area || next.startsWith(`${area}/`) || next.startsWith(`${area}?`)
  return inArea ? next : undefined
}
