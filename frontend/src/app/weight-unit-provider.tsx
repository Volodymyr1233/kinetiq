import type { ReactNode } from 'react'
import { useCachedAccount } from '@/features/account/api/account'
import { WeightUnitContext } from '@/lib/weight'

/** Gives every screen the signed-in person's weight unit, so features need not know about accounts. */
export function WeightUnitProvider({ children }: { children: ReactNode }) {
  const account = useCachedAccount()

  return (
    <WeightUnitContext value={account?.units ?? 'KG'}>
      {children}
    </WeightUnitContext>
  )
}
