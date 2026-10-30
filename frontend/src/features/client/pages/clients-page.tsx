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
import { errorMessageKey } from '@/lib/api/problem'
import { useClients } from '../api/clients'
import { ClientTable } from '../components/client-table'

export function ClientsPage() {
  const { t, i18n } = useTranslation()
  const query = useClients()
  const [search, setSearch] = useState('')
  const clients = query.data?.clients ?? []
  const needle = search.trim().toLocaleLowerCase(i18n.language)
  const shown = clients.filter((client) =>
    client.name.toLocaleLowerCase(i18n.language).includes(needle),
  )

  return (
    <div className="flex flex-col gap-6 px-6 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="font-heading text-2xl font-semibold">
            {t('clients.title')}
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
          <Link to="invites">{t('invites.open')}</Link>
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
              <Link to="invites">
                <UserPlusIcon data-icon="inline-start" aria-hidden="true" />
                {t('invites.inviteClient')}
              </Link>
            </Button>
          </EmptyContent>
        </Empty>
      ) : (
        <div className="flex flex-col gap-3">
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
          {shown.length === 0 ? (
            <Empty className="border">
              <EmptyHeader>
                <EmptyTitle>{t('clients.noMatch.title')}</EmptyTitle>
                <EmptyDescription>{t('clients.noMatch.body')}</EmptyDescription>
              </EmptyHeader>
              <EmptyContent>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setSearch('')}
                >
                  {t('clients.noMatch.clear')}
                </Button>
              </EmptyContent>
            </Empty>
          ) : (
            <div className="rounded-lg border">
              <ClientTable clients={shown} />
            </div>
          )}
        </div>
      )}
    </div>
  )
}
