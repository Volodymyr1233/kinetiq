import { screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import {
  json,
  pathsOf,
  problem,
  renderPage,
  stubApi,
  type Reply,
} from '@/test/page'
import { DraftCard } from './draft-card'

const SUMMARY = {
  trainingDays: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
  weeks: 12,
  goal: 'BUILD_MUSCLE',
}

const valueOf = (label: string) => screen.getByText(label).nextElementSibling

describe('DraftCard', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the training days, length and focus, and says the trainer can change them', async () => {
    stubApi(() => json(SUMMARY))
    renderPage(<DraftCard />)

    expect(await screen.findByText('Draft')).toBeInTheDocument()
    expect(screen.getByText('Waiting for your trainer')).toBeInTheDocument()
    expect(valueOf('Training days')).toHaveTextContent('Mon, Wed, Fri')
    expect(valueOf('Length')).toHaveTextContent('12 weeks')
    expect(valueOf('Focus')).toHaveTextContent('Build muscle')
    expect(
      screen.getByText('Your trainer can still change these.'),
    ).toBeInTheDocument()
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    stubApi(() => json(SUMMARY))
    renderPage(<DraftCard />)

    expect(await screen.findByText('Szkic')).toBeInTheDocument()
    expect(valueOf('Dni treningowe')).toHaveTextContent('pon., śr. i pt.')
    expect(valueOf('Długość')).toHaveTextContent('12 tygodni')
    expect(valueOf('Cel')).toHaveTextContent('Budowa mięśni')
    expect(
      screen.getByText('Trener może to jeszcze zmienić.'),
    ).toBeInTheDocument()
  })

  it.each<[string, Reply]>([
    [
      'there is no summary',
      () => problem(404, { status: 404, code: 'plan.not-found' }),
    ],
    [
      'the read fails',
      () => problem(500, { status: 500, code: 'internal.error' }),
    ],
  ])('shows nothing when %s', async (_, reply) => {
    const requests = stubApi(reply)
    const { container } = renderPage(<DraftCard />)

    await waitFor(() =>
      expect(pathsOf(requests)).toContain('/api/v1/plan/draft-summary'),
    )

    expect(container).toBeEmptyDOMElement()
  })
})
