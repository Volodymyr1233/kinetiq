import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { currentAccountQuery } from '@/features/account/api/account'
import { WeightUnitContext } from '@/lib/weight'

/** Gives every screen the signed-in person's weight unit, so features need not know about accounts. */
export function WeightUnitProvider({ children }: { children: ReactNode }) {
  // never fetches: the guards and sign-in put the account into the cache, and public pages must not ask
  const { data: account } = useQuery({ ...currentAccountQuery, enabled: false })

  return (
    <WeightUnitContext value={account?.units ?? 'KG'}>
      {children}
    </WeightUnitContext>
  )
}
