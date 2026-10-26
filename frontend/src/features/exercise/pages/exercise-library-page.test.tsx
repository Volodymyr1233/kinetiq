import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Toaster } from 'sonner'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, renderPage, stubApi, type Reply } from '@/test/page'
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
    starter: true,
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

const OWN = exercise({
  id: '5',
  name: 'Wall sit',
  equipment: '',
  measure: 'SECONDS',
  amount: 45,
  starter: false,
})

// answers like the backend: filters by category and name, counts the whole library
function listing(entries: Exercise[], request: Request) {
  const params = new URL(request.url).searchParams
  const category = params.get('category')
  const name = params.get('name')?.toLowerCase() ?? ''
  const exercises = entries.filter(
    (entry) =>
      (!category || entry.category === category) &&
      entry.name.toLowerCase().includes(name),
  )
  return Response.json({ total: entries.length, exercises })
}

function library(request: Request) {
  return listing(LIBRARY, request)
}

// a backend that stores what is saved; `save` answers a save instead when given
function backend(save?: Reply) {
  const entries = [...LIBRARY, OWN]
  return async (request: Request) => {
    if (request.method === 'GET') return listing(entries, request)
    if (save) return save(request)
    const body = (await request.clone().json()) as Exercise
    if (request.method === 'POST') {
      const created = { ...body, id: '6', plansUsing: 0, starter: false }
      entries.push(created)
      return json(created, 201)
    }
    const id = new URL(request.url).pathname.split('/').pop()
    const index = entries.findIndex((entry) => entry.id === id)
    entries[index] = { ...entries[index], ...body }
    return json(entries[index])
  }
}

function saves(requests: Request[]) {
  return requests.filter((request) => request.method !== 'GET')
}

function dialog() {
  return within(screen.getByRole('dialog'))
}

async function renderWith(reply: Reply) {
  const requests = stubApi(reply)
  renderPage(
    <>
      <ExerciseLibraryPage />
      <Toaster />
    </>,
  )
  await screen.findByRole('button', { name: 'Back squat' })
  return requests
}

async function openAdd(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: 'Add exercise' }))
  return dialog()
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

describe('ExerciseLibraryPage adding and editing', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('opens "Add exercise" with the defaults of the board and focus on Name', async () => {
    const user = userEvent.setup()
    await renderWith(backend())

    const add = await openAdd(user)

    expect(
      add.getByRole('heading', { name: 'Add exercise' }),
    ).toBeInTheDocument()
    expect(add.getByRole('textbox', { name: 'Name' })).toHaveFocus()
    expect(add.getByRole('combobox', { name: 'Category' })).toHaveTextContent(
      'Legs',
    )
    expect(
      within(add.getByRole('radiogroup', { name: 'Measure' })).getByRole(
        'radio',
        { name: 'Repetitions' },
      ),
    ).toBeChecked()
    expect(add.getByRole('textbox', { name: 'Sets' })).toHaveValue('3')
    expect(add.getByRole('textbox', { name: 'Reps' })).toHaveValue('10')
    expect(add.getByRole('textbox', { name: 'Rest' })).toHaveValue('90')
  })

  it('asks for a name before sending anything', async () => {
    const user = userEvent.setup()
    const requests = await renderWith(backend())
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), '   ')
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    const name = add.getByRole('textbox', { name: 'Name' })
    expect(name).toHaveAccessibleDescription('Enter a name.')
    expect(name).toHaveAttribute('aria-invalid', 'true')
    expect(name).toHaveFocus()
    expect(saves(requests)).toHaveLength(0)
  })

  it('adds the exercise, resets the filters, chooses it and announces it', async () => {
    const user = userEvent.setup()
    const requests = await renderWith(backend())
    await user.click(screen.getByRole('radio', { name: 'Core' }))
    await user.type(screen.getByRole('searchbox'), 'plank')
    const add = await openAdd(user)

    await user.type(
      add.getByRole('textbox', { name: 'Name' }),
      ' Goblet squat ',
    )
    await user.type(
      add.getByRole('textbox', { name: 'Equipment (optional)' }),
      'Dumbbell',
    )
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    expect(
      await screen.findByText('Goblet squat added to the library.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await saves(requests)[0].json()).toEqual({
      name: 'Goblet squat',
      category: 'LEGS',
      measure: 'REPS',
      equipment: 'Dumbbell',
      sets: 3,
      amount: 10,
      restSeconds: 90,
      cues: '',
    })
    expect(screen.getByRole('radio', { name: 'All' })).toBeChecked()
    expect(screen.getByRole('searchbox')).toHaveValue('')
    expect(
      await detail().findByRole('heading', { name: 'Goblet squat' }),
    ).toBeInTheDocument()
  })

  it('shows a taken name on the name field and keeps the dialog', async () => {
    const user = userEvent.setup()
    await renderWith(
      backend(() => problem(409, { code: 'exercise.name-taken' })),
    )
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), 'plank')
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    const name = add.getByRole('textbox', { name: 'Name' })
    await waitFor(() =>
      expect(name).toHaveAccessibleDescription(
        'An exercise with this name is already in the library.',
      ),
    )
    expect(name).toHaveFocus()
    expect(name).toHaveValue('plank')

    await user.type(name, 's')

    expect(name).not.toHaveAccessibleDescription()
  })

  it('relabels the amount for seconds, keeps typed numbers and lists the timed default', async () => {
    const user = userEvent.setup()
    await renderWith(backend())
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), 'Hollow hold')
    await user.clear(add.getByRole('textbox', { name: 'Reps' }))
    await user.type(add.getByRole('textbox', { name: 'Reps' }), '45')
    await user.click(add.getByRole('radio', { name: 'Seconds' }))

    expect(add.getByRole('textbox', { name: 'Seconds' })).toHaveValue('45')
    await user.click(add.getByRole('radio', { name: 'Repetitions' }))
    expect(add.getByRole('textbox', { name: 'Reps' })).toHaveValue('45')
    await user.click(add.getByRole('radio', { name: 'Seconds' }))
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    const row = (
      await screen.findByRole('button', { name: 'Hollow hold' })
    ).closest('tr') as HTMLElement
    expect(within(row).getByText('3 × 45 s')).toBeInTheDocument()
  })

  it('marks a starter exercise and offers no Edit', async () => {
    await renderWith(backend())

    expect(detail().getByText('Starter')).toBeInTheDocument()
    expect(
      detail().queryByRole('button', { name: 'Edit' }),
    ).not.toBeInTheDocument()
  })

  it('edits an own exercise in a filled dialog whose measure is fixed', async () => {
    const user = userEvent.setup()
    const requests = await renderWith(backend())
    await user.click(screen.getByRole('button', { name: 'Wall sit' }))

    expect(detail().queryByText('Starter')).not.toBeInTheDocument()
    expect(detail().getByText('Not set')).toBeInTheDocument()
    await user.click(detail().getByRole('button', { name: 'Edit' }))
    const edit = dialog()
    expect(
      edit.getByRole('heading', { name: 'Edit exercise' }),
    ).toBeInTheDocument()
    expect(edit.getByRole('textbox', { name: 'Name' })).toHaveValue('Wall sit')
    expect(edit.getByRole('textbox', { name: 'Seconds' })).toHaveValue('45')
    expect(edit.queryByRole('radio')).not.toBeInTheDocument()
    expect(edit.getByText('Seconds', { selector: 'p' })).toBeInTheDocument()

    await user.clear(edit.getByRole('textbox', { name: 'Rest' }))
    await user.type(edit.getByRole('textbox', { name: 'Rest' }), '60')
    await user.clear(edit.getByRole('textbox', { name: 'Cues (optional)' }))
    await user.type(
      edit.getByRole('textbox', { name: 'Cues (optional)' }),
      'Back flat.',
    )
    await user.click(edit.getByRole('button', { name: 'Save exercise' }))

    expect(await screen.findByText('Wall sit saved.')).toBeInTheDocument()
    const put = saves(requests)[0]
    expect(put.method).toBe('PUT')
    expect(new URL(put.url).pathname).toBe('/api/v1/exercises/5')
    expect(await put.json()).not.toHaveProperty('measure')
    expect(await detail().findByText('Back flat.')).toBeInTheDocument()
    expect(detail().getByText('60 s')).toBeInTheDocument()
  })

  it('shows a field refused by the server with the message the dialog uses', async () => {
    const user = userEvent.setup()
    await renderWith(
      backend(() =>
        problem(400, {
          code: 'validation.failed',
          errors: [
            { field: 'restSeconds', code: 'max' },
            { field: 'sets', code: 'max' },
          ],
        }),
      ),
    )
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), 'Goblet squat')
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    const sets = add.getByRole('textbox', { name: 'Sets' })
    await waitFor(() =>
      expect(sets).toHaveAccessibleDescription('Enter 1 to 20 sets.'),
    )
    expect(sets).toHaveFocus()
    expect(add.queryByText(/details are not valid/)).toBeNull()
  })

  it('keeps the dialog and the entries when saving fails for an unknown reason', async () => {
    const user = userEvent.setup()
    await renderWith(
      backend(() => problem(500, { code: 'internal.unknown-code' })),
    )
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), 'Goblet squat')
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    expect(await add.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Try again.',
    )
    expect(add.getByRole('textbox', { name: 'Name' })).toHaveValue(
      'Goblet squat',
    )
  })

  it('shows the limit as an alert and keeps the entries', async () => {
    const user = userEvent.setup()
    await renderWith(
      backend(() => problem(409, { code: 'exercise.limit-reached' })),
    )
    const add = await openAdd(user)

    await user.type(add.getByRole('textbox', { name: 'Name' }), 'Goblet squat')
    await user.click(add.getByRole('button', { name: 'Save exercise' }))

    expect(await add.findByRole('alert')).toHaveTextContent(
      "You've reached the limit of 200 own exercises.",
    )
    expect(add.getByRole('textbox', { name: 'Name' })).toHaveValue(
      'Goblet squat',
    )
  })

  it('closes the dialog and refreshes the list when the exercise is no longer editable', async () => {
    const user = userEvent.setup()
    const requests = await renderWith(
      backend(() => problem(404, { code: 'exercise.not-found' })),
    )
    await user.click(screen.getByRole('button', { name: 'Wall sit' }))
    await user.click(detail().getByRole('button', { name: 'Edit' }))
    const before = requests.length

    await user.click(dialog().getByRole('button', { name: 'Save exercise' }))

    expect(
      await screen.findByText("This exercise can't be edited."),
    ).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(requests.length).toBeGreaterThan(before + 1))
  })

  it('blocks a second save while the first is on its way', async () => {
    const user = userEvent.setup()
    let answer: (response: Response) => void = () => {}
    const requests = await renderWith(
      backend(
        () =>
          new Promise<Response>((resolve) => {
            answer = resolve
          }),
      ),
    )
    const add = await openAdd(user)
    await user.type(add.getByRole('textbox', { name: 'Name' }), 'Goblet squat')
    const save = add.getByRole('button', { name: 'Save exercise' })

    await user.click(save)
    await waitFor(() => expect(save).toBeDisabled())
    expect(save).toHaveAttribute('aria-busy', 'true')
    await user.click(save)

    expect(saves(requests)).toHaveLength(1)
    answer(problem(500, { code: 'internal.error' }))
    await waitFor(() => expect(save).toBeEnabled())
  })

  it('closes on Escape and returns focus to the button that opened it', async () => {
    const user = userEvent.setup()
    const requests = await renderWith(backend())
    await openAdd(user)

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add exercise' })).toHaveFocus()
    expect(saves(requests)).toHaveLength(0)
  })

  it('offers to add an exercise or clear the filters when nothing matches', async () => {
    const user = userEvent.setup()
    await renderWith(backend())

    await user.type(screen.getByRole('searchbox'), 'zzz')

    const empty = (await screen.findByText('No exercises match')).closest(
      '[data-slot=empty]',
    ) as HTMLElement
    expect(
      within(empty).getByRole('button', { name: 'Add exercise' }),
    ).toBeInTheDocument()
    expect(
      within(empty).getByRole('button', { name: 'Clear filters' }),
    ).toBeInTheDocument()
  })

  it('shows the dialog in Polish', async () => {
    await i18n.changeLanguage('pl')
    const user = userEvent.setup()
    await renderWith(backend())

    await user.click(screen.getByRole('button', { name: 'Dodaj ćwiczenie' }))

    expect(
      dialog().getByRole('heading', { name: 'Dodaj ćwiczenie' }),
    ).toBeInTheDocument()
    expect(dialog().getByRole('textbox', { name: 'Nazwa' })).toBeInTheDocument()
    expect(
      dialog().getByRole('button', { name: 'Zapisz ćwiczenie' }),
    ).toBeInTheDocument()
  })
})
