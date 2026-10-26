import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { WeightUnitProvider } from '@/app/weight-unit-provider'
import {
  currentAccountQuery,
  type CurrentAccount,
} from '@/features/account/api/account'
import { useWeightUnit } from '@/lib/weight'
import { SIGNED_IN } from '@/test/page'

function ShowUnit() {
  return <p>{useWeightUnit()}</p>
}

function renderWith(account?: CurrentAccount) {
  const client = new QueryClient()
  if (account) client.setQueryData(currentAccountQuery.queryKey, account)
  render(
    <QueryClientProvider client={client}>
      <WeightUnitProvider>
        <ShowUnit />
      </WeightUnitProvider>
    </QueryClientProvider>,
  )
}

describe('WeightUnitProvider', () => {
  it("gives the signed-in person's unit", () => {
    renderWith({ ...SIGNED_IN.CLIENT, units: 'LB' })

    expect(screen.getByText('LB')).toBeInTheDocument()
  })

  it('gives kilograms while nobody is signed in', () => {
    renderWith()

    expect(screen.getByText('KG')).toBeInTheDocument()
  })
})
