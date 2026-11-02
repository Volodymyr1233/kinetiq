import { CircleAlertIcon, PlusIcon, SearchIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from '@/components/ui/input-group'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { errorMessageKey } from '@/lib/api/problem'
import {
  CATEGORIES,
  useExercises,
  type Category,
  type Exercise,
} from '@/lib/exercise-library'
import { MAX_EXERCISES } from '../plan-draft'

const ALL = 'ALL'
const SEARCH_PAUSE_MS = 300

/** "Add exercise" and its picker over the trainer's library. It stays open for the next pick and says what it added. */
export function ExercisePicker({
  id,
  disabled,
  sessionLabel,
  onPick,
}: {
  id: string
  disabled: boolean
  sessionLabel: string
  onPick: (exercise: Exercise) => void
}) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next || !disabled) setOpen(next)
      }}
    >
      <DialogTrigger asChild>
        {/* never disabled for real: focus returns here on closing, also when the last pick filled the session */}
        <Button
          id={id}
          variant="outline"
          size="sm"
          aria-disabled={disabled}
          className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
        >
          <PlusIcon data-icon="inline-start" aria-hidden="true" />
          {t('plan.entries.add')}
        </Button>
      </DialogTrigger>
      {open && (
        <PickerContent
          full={disabled}
          sessionLabel={sessionLabel}
          onPick={onPick}
        />
      )}
    </Dialog>
  )
}

function PickerContent({
  full,
  sessionLabel,
  onPick,
}: {
  full: boolean
  sessionLabel: string
  onPick: (exercise: Exercise) => void
}) {
  const { t } = useTranslation()
  const [category, setCategory] = useState<Category | typeof ALL>(ALL)
  const [search, setSearch] = useState('')
  const [added, setAdded] = useState<{ name: string; times: number }>()
  const settledSearch = useDebouncedValue(search, SEARCH_PAUSE_MS)
  const query = useExercises({
    category: category === ALL ? undefined : category,
    // clearing needs no pause: the full list is shown at once
    name: search.trim() ? settledSearch : '',
  })
  const exercises = query.data?.exercises ?? []

  function pick(exercise: Exercise) {
    if (full) return
    onPick(exercise)
    // the count changes the text, so the same exercise picked twice is announced twice
    setAdded((last) => ({
      name: exercise.name,
      times: last?.name === exercise.name ? last.times + 1 : 1,
    }))
  }

  return (
    <DialogContent
      data-density="compact"
      className="flex max-h-[calc(100dvh-2rem)] flex-col sm:max-w-140"
    >
      <DialogHeader>
        <DialogTitle>{t('plan.picker.title')}</DialogTitle>
        <DialogDescription>
          {t('plan.picker.description', { session: sessionLabel })}
        </DialogDescription>
      </DialogHeader>
      <InputGroup>
        <InputGroupAddon>
          <SearchIcon aria-hidden />
        </InputGroupAddon>
        <InputGroupInput
          type="search"
          // the longest a name can be; the backend refuses longer text
          maxLength={100}
          aria-label={t('plan.picker.search')}
          placeholder={t('plan.picker.search')}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </InputGroup>
      <ToggleGroup
        type="single"
        variant="outline"
        size="sm"
        spacing={1.5}
        className="flex-wrap"
        aria-label={t('exercises.categoryFilter')}
        value={category}
        // choosing the chosen chip again gives an empty value; one chip always stays chosen
        onValueChange={(value) =>
          value && setCategory(value as Category | typeof ALL)
        }
      >
        {[ALL, ...CATEGORIES].map((code) => (
          <ToggleGroupItem
            key={code}
            value={code}
            className="data-[state=on]:bg-primary data-[state=on]:text-primary-foreground rounded-full px-3"
          >
            {t(`exercises.categories.${code}`)}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <div className="min-h-40 flex-1 overflow-y-auto rounded-lg border">
        {query.isError ? (
          <Alert variant="destructive" className="border-0">
            <CircleAlertIcon aria-hidden />
            <AlertDescription>
              {t(errorMessageKey(query.error))}
            </AlertDescription>
            <AlertAction>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void query.refetch()}
              >
                {t('plan.retry')}
              </Button>
            </AlertAction>
          </Alert>
        ) : query.isPending ? (
          <div aria-hidden className="flex flex-col gap-2 p-3">
            {Array.from({ length: 6 }, (_, row) => (
              <Skeleton key={row} className="h-8 w-full" />
            ))}
          </div>
        ) : exercises.length === 0 ? (
          <Empty className="border-0">
            <EmptyHeader>
              <EmptyTitle>{t('plan.picker.empty.title')}</EmptyTitle>
              <EmptyDescription>{t('plan.picker.empty.body')}</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <ul className="divide-y">
            {exercises.map((exercise) => {
              const category = t(`exercises.categories.${exercise.category}`)
              const amounts = t(
                exercise.measure === 'SECONDS'
                  ? 'exercises.default.seconds'
                  : 'exercises.default.reps',
                { sets: exercise.sets, amount: exercise.amount },
              )
              return (
                <li key={exercise.id}>
                  <button
                    type="button"
                    aria-disabled={full}
                    // says all that is visible, so voice control can use any of it
                    aria-label={t('plan.picker.addOne', {
                      exercise: exercise.name,
                      category,
                      amounts,
                    })}
                    onClick={() => pick(exercise)}
                    className="hover:bg-muted focus-visible:ring-ring/50 flex w-full items-center justify-between gap-4 px-3 py-2 text-left text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-inset aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                  >
                    <span className="min-w-0">
                      <span className="block font-medium">{exercise.name}</span>
                      <span className="text-muted-foreground block text-xs">
                        {category}
                      </span>
                    </span>
                    <span className="num text-muted-foreground shrink-0">
                      {amounts}
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
      <DialogFooter className="items-center sm:justify-between">
        <p role="status" className="text-muted-foreground text-sm">
          {full
            ? t('plan.entries.limit', { count: MAX_EXERCISES })
            : added &&
              t('plan.picker.added', {
                exercise: added.name,
                session: sessionLabel,
                count: added.times,
              })}
        </p>
        <DialogClose asChild>
          <Button>{t('plan.picker.done')}</Button>
        </DialogClose>
      </DialogFooter>
    </DialogContent>
  )
}
