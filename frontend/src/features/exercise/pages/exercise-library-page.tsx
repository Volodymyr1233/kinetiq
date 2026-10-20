import { CircleAlertIcon, SearchIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
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
import { CATEGORIES, useExercises, type Category } from '../api/exercises'
import { ExerciseDetail } from '../components/exercise-detail'
import { ExerciseTable } from '../components/exercise-table'

const ALL = 'ALL'
const SEARCH_PAUSE_MS = 300

export function ExerciseLibraryPage() {
  const { t } = useTranslation()
  const [category, setCategory] = useState<Category | typeof ALL>(ALL)
  const [search, setSearch] = useState('')
  const [chosenId, setChosenId] = useState<string>()
  const settledSearch = useDebouncedValue(search, SEARCH_PAUSE_MS)
  // clearing needs no pause: the full list is shown at once, not after the old text is asked for
  const name = search.trim() ? settledSearch : ''
  const query = useExercises({
    category: category === ALL ? undefined : category,
    name,
  })
  const exercises = query.data?.exercises ?? []
  // a choice that the filters hid falls back to the first exercise still listed
  const chosen =
    exercises.find((exercise) => exercise.id === chosenId) ?? exercises[0]

  function clearFilters() {
    setCategory(ALL)
    setSearch('')
  }

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">
          {t('exercises.title')}
        </h1>
        <p role="status" className="text-muted-foreground text-sm">
          {query.data
            ? t('exercises.count', {
                shown: exercises.length,
                count: query.data.total,
              })
            : query.isPending && t('exercises.loading')}
        </p>
      </div>
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
      <div className="flex flex-wrap items-start gap-6">
        <div className="flex min-w-0 flex-[999_1_560px] flex-col gap-3">
          <InputGroup className="w-65" data-density="compact">
            <InputGroupAddon>
              <SearchIcon aria-hidden />
            </InputGroupAddon>
            <InputGroupInput
              type="search"
              // the longest a name can be; the backend refuses longer text
              maxLength={100}
              aria-label={t('exercises.search.label')}
              placeholder={t('exercises.search.placeholder')}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </InputGroup>
          {query.isError ? (
            <Alert variant="destructive">
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
                  {t('exercises.error.retry')}
                </Button>
              </AlertAction>
            </Alert>
          ) : query.isPending ? (
            <div aria-hidden className="flex flex-col gap-2">
              {Array.from({ length: 8 }, (_, row) => (
                <Skeleton key={row} className="h-8 w-full" />
              ))}
            </div>
          ) : exercises.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyTitle>{t('exercises.empty.title')}</EmptyTitle>
                <EmptyDescription>{t('exercises.empty.body')}</EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button variant="outline" size="sm" onClick={clearFilters}>
                  {t('exercises.empty.clear')}
                </Button>
              </EmptyContent>
            </Empty>
          ) : (
            <div className="rounded-lg border">
              <ExerciseTable
                exercises={exercises}
                chosenId={chosen?.id}
                onChoose={setChosenId}
              />
            </div>
          )}
        </div>
        {chosen && !query.isError && (
          <aside className="max-w-full flex-[1_1_300px]">
            <ExerciseDetail exercise={chosen} />
          </aside>
        )}
      </div>
    </div>
  )
}
