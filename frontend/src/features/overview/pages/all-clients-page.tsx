import { CircleAlertIcon, SearchIcon, UserPlusIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
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
import { errorMessageKey } from '@/lib/api/problem'
import { STATUSES, useOverview, type ClientStatus } from '../api/overview'
import { nextSort, sortClients, type Sort } from '../client-sort'
import { ClientTable } from '../components/client-table'

type Filter = ClientStatus | 'ALL'

export function AllClientsPage() {
  const { t, i18n } = useTranslation()
  const query = useOverview()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<Filter>('ALL')
  const [sort, setSort] = useState<Sort>({
    key: 'name',
    direction: 'ascending',
  })
  const clients = query.data?.clients ?? []
  const needle = search.trim().toLocaleLowerCase(i18n.language)
  // the filters count the clients the search leaves, so each count is the rows its filter shows
  const found = clients.filter((client) =>
    client.name.toLocaleLowerCase(i18n.language).includes(needle),
  )
  const count = (status: Filter) =>
    status === 'ALL'
      ? found.length
      : found.filter((client) => client.status === status).length
  const shown = sortClients(
    found.filter((client) => filter === 'ALL' || client.status === filter),
    sort,
    i18n.language,
  )
  const number = new Intl.NumberFormat(i18n.language)

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="font-heading text-2xl font-semibold">
            {t('overview.all.title')}
          </h1>
          <p role="status" className="text-muted-foreground text-sm">
            {query.data
              ? t('clients.count', {
                  shown: shown.length,
                  count: clients.length,
                })
              : query.isPending && t('clients.loading')}
          </p>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to="/trainer/clients/invites">{t('invites.open')}</Link>
        </Button>
      </div>

      {query.isError ? (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button
              variant="outline"
              size="sm"
              onClick={() => void query.refetch()}
            >
              {t('clients.retry')}
            </Button>
          </AlertAction>
        </Alert>
      ) : query.isPending ? (
        <div aria-hidden className="flex flex-col gap-2">
          {Array.from({ length: 5 }, (_, row) => (
            <Skeleton key={row} className="h-9 w-full" />
          ))}
        </div>
      ) : clients.length === 0 ? (
        <Empty className="bg-muted border border-dashed">
          <EmptyHeader>
            <EmptyTitle>{t('clients.empty.title')}</EmptyTitle>
            <EmptyDescription>{t('clients.empty.body')}</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button asChild>
              <Link to="/trainer/clients/invites">
                <UserPlusIcon data-icon="inline-start" aria-hidden="true" />
                {t('invites.inviteClient')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <InputGroup className="w-65" data-density="compact">
              <InputGroupAddon>
                <SearchIcon aria-hidden />
              </InputGroupAddon>
              <InputGroupInput
                type="search"
                // the longest a name can be
                maxLength={100}
                aria-label={t('clients.search.label')}
                placeholder={t('clients.search.placeholder')}
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </InputGroup>
            <ToggleGroup
              type="single"
              variant="outline"
              size="sm"
              aria-label={t('overview.all.filter')}
              value={filter}
              // choosing the active filter again keeps it, so one is always on
              onValueChange={(value) => value && setFilter(value as Filter)}
              className="flex-wrap"
            >
              {(['ALL', ...STATUSES] as Filter[]).map((status) => (
                <ToggleGroupItem key={status} value={status}>
                  {status === 'ALL'
                    ? t('overview.all.everyone')
                    : t(`overview.status.${status}`)}{' '}
                  <span className="num text-muted-foreground">
                    {number.format(count(status))}
                  </span>
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          </div>
          {shown.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyTitle>{t('overview.all.noMatch.title')}</EmptyTitle>
                <EmptyDescription>
                  {t('overview.all.noMatch.body')}
                </EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setSearch('')
                    setFilter('ALL')
                  }}
                >
                  {t('overview.all.noMatch.clear')}
                </Button>
              </EmptyContent>
            </Empty>
          ) : (
            <div className="rounded-lg border">
              <ClientTable
                clients={shown}
                full
                sort={sort}
                onSort={(key) => setSort(nextSort(sort, key))}
              />
            </div>
          )}
        </div>
      )}
    </div>
  )
}
