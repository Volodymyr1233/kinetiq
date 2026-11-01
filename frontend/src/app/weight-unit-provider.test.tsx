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
  return client
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

  it('gives the next account its own unit after signing out and in, without a reload', async () => {
    const client = renderWith({ ...SIGNED_IN.TRAINER, units: 'LB' })

    client.clear()
    client.setQueryData(currentAccountQuery.queryKey, {
      ...SIGNED_IN.TRAINER,
      units: 'KG',
    })

    expect(await screen.findByText('KG')).toBeInTheDocument()
  })
})
