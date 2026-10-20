import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { problem, renderPage, stubApi } from '@/test/page'
import type { Exercise } from '../api/exercises'
import { ExerciseLibraryPage } from './exercise-library-page'

function exercise(fields: Partial<Exercise> & Pick<Exercise, 'id' | 'name'>) {
  return {
    category: 'LEGS',
    equipment: 'Barbell',
    measure: 'REPS',
    sets: 3,
    amount: 5,
    restSeconds: 120,
    cues: 'Brace first.',
    plansUsing: 0,
    ...fields,
  } satisfies Exercise
}

const LIBRARY: Exercise[] = [
  exercise({ id: '1', name: 'Back squat', cues: 'Knees follow toes.' }),
  exercise({
    id: '2',
    name: 'Bench press',
    category: 'CHEST',
    sets: 4,
    amount: 8,
    plansUsing: 1,
  }),
  exercise({
    id: '3',
    name: 'Plank',
    category: 'CORE',
    equipment: 'Bodyweight',
    measure: 'SECONDS',
    amount: 45,
    restSeconds: 60,
    cues: '',
    plansUsing: 2,
  }),
  exercise({ id: '4', name: 'Split squat', plansUsing: 5 }),
]

// answers like the backend: filters by category and name, counts the whole library
function library(request: Request) {
  const params = new URL(request.url).searchParams
  const category = params.get('category')
  const name = params.get('name')?.toLowerCase() ?? ''
  const exercises = LIBRARY.filter(
    (entry) =>
      (!category || entry.category === category) &&
      entry.name.toLowerCase().includes(name),
  )
  return Response.json({ total: LIBRARY.length, exercises })
}

function namesOf(requests: Request[]) {
  return requests.map((request) =>
    new URL(request.url).searchParams.get('name'),
  )
}

function rowNames() {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getByRole('button').textContent)
}

function detail() {
  return within(screen.getByRole('complementary'))
}

async function renderLoaded() {
  const requests = stubApi(library)
  renderPage(<ExerciseLibraryPage />)
  await screen.findByRole('button', { name: 'Back squat' })
  return requests
}

describe('ExerciseLibraryPage', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('lists exercises with their default and plans, and counts them', async () => {
    await renderLoaded()

    expect(screen.getByRole('status')).toHaveTextContent('4 of 4 exercises')
    const bench = screen.getAllByRole('row')[2]
    expect(within(bench).getByText('4 × 8')).toBeInTheDocument()
    expect(within(bench).getByText('1 plan')).toBeInTheDocument()
    const plank = screen.getAllByRole('row')[3]
    expect(within(plank).getByText('3 × 45 s')).toBeInTheDocument()
    expect(
      within(screen.getAllByRole('row')[1]).getByText('Not used'),
    ).toBeInTheDocument()
  })

  it('shows the first exercise in the panel, then the one that was chosen', async () => {
    const user = userEvent.setup()
    await renderLoaded()

    expect(
      detail().getByRole('heading', { name: 'Back squat' }),
    ).toBeInTheDocument()
    expect(detail().getByText('Not used yet')).toBeInTheDocument()
    expect(detail().getByText('Knees follow toes.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Back squat' })).toHaveAttribute(
      'aria-current',
      'true',
    )

    await user.click(screen.getByRole('button', { name: 'Plank' }))

    expect(detail().getByRole('heading', { name: 'Plank' })).toBeInTheDocument()
    expect(detail().getByText('60 s')).toBeInTheDocument()
    expect(detail().getByText('2 plans')).toBeInTheDocument()
    expect(detail().queryByText('Cues')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Plank' })).toHaveAttribute(
      'aria-current',
      'true',
    )
    expect(
      screen.getByRole('button', { name: 'Back squat' }),
    ).not.toHaveAttribute('aria-current')
  })

  it('narrows the list to a category and keeps exactly one chip checked', async () => {
    const user = userEvent.setup()
    await renderLoaded()
    const chips = within(screen.getByRole('radiogroup', { name: 'Category' }))

    expect(chips.getByRole('radio', { name: 'All' })).toBeChecked()
    await user.click(chips.getByRole('radio', { name: 'Core' }))

    await waitFor(() => expect(rowNames()).toEqual(['Plank']))
    expect(screen.getByRole('status')).toHaveTextContent('1 of 4 exercises')
    expect(chips.getAllByRole('radio', { checked: true })).toHaveLength(1)

    await user.click(chips.getByRole('radio', { name: 'Core' }))

    expect(chips.getByRole('radio', { name: 'Core' })).toBeChecked()
  })

  it('searches once the typing pauses', async () => {
    const user = userEvent.setup()
    const requests = await renderLoaded()

    await user.type(
      screen.getByRole('searchbox', { name: 'Search exercises' }),
      'squat',
    )

    await waitFor(() =>
      expect(rowNames()).toEqual(['Back squat', 'Split squat']),
    )
    expect(namesOf(requests)).toEqual([null, 'squat'])
  })

  it('takes at most 100 characters in the search field', async () => {
    const user = userEvent.setup()
    await renderLoaded()
    const search = screen.getByRole('searchbox')

    await user.click(search)
    await user.paste('a'.repeat(101))

    expect(search).toHaveValue('a'.repeat(100))
  })

  it('moves the choice to the first exercise left when the chosen one is filtered out', async () => {
    const user = userEvent.setup()
    await renderLoaded()

    await user.click(screen.getByRole('button', { name: 'Bench press' }))
    await user.type(screen.getByRole('searchbox'), 'squat')

    await waitFor(() =>
      expect(
        detail().getByRole('heading', { name: 'Back squat' }),
      ).toBeInTheDocument(),
    )
  })

  it('offers to clear the filters when nothing matches', async () => {
    const user = userEvent.setup()
    await renderLoaded()

    await user.type(screen.getByRole('searchbox'), 'zzz')

    expect(await screen.findByText('No exercises match')).toBeInTheDocument()
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Clear filters' }))

    // at once, without waiting for the search pause
    expect(rowNames()).toHaveLength(4)
    expect(screen.getByRole('searchbox')).toHaveValue('')
  })

  it.each([
    ['internal.unknown-code', 'Something went wrong. Try again.'],
    ['request.unauthenticated', 'Sign in to continue.'],
  ])(
    'explains a failure with code %s and loads again on "Try again"',
    async (code, message) => {
      let failing = true
      const requests = stubApi((request) =>
        failing ? problem(401, { code }) : library(request),
      )
      const user = userEvent.setup()
      renderPage(<ExerciseLibraryPage />)

      expect(await screen.findByRole('alert')).toHaveTextContent(message)
      failing = false
      await user.click(screen.getByRole('button', { name: 'Try again' }))

      expect(
        await screen.findByRole('button', { name: 'Back squat' }),
      ).toBeInTheDocument()
      expect(requests).toHaveLength(2)
    },
  )

  it('asks in Polish and shows Polish labels and plural forms', async () => {
    await i18n.changeLanguage('pl')
    const requests = await renderLoaded()

    expect(requests[0].headers.get('Accept-Language')).toBe('pl')
    expect(
      screen.getByRole('radio', { name: 'Klatka piersiowa' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('4 z 4 ćwiczeń')
    const plans = screen
      .getAllByRole('row')
      .slice(1)
      .map((row) => row.lastElementChild?.textContent)
    expect(plans).toEqual(['Nieużywane', '1 plan', '2 plany', '5 planów'])
  })
})
