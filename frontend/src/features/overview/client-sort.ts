import { STATUSES, type OverviewClient } from './api/overview'

export type SortKey = 'name' | 'lastLog' | 'sessions' | 'rpe' | 'status'
export type SortDirection = 'ascending' | 'descending'
export type Sort = { key: SortKey; direction: SortDirection }

// the order a column takes when it is first chosen: names A to Z, the newest log, the lowest share done, the highest
// RPE and the most urgent status first
export const FIRST_DIRECTION: Record<SortKey, SortDirection> = {
  name: 'ascending',
  lastLog: 'descending',
  sessions: 'ascending',
  rpe: 'descending',
  status: 'ascending',
}

const VALUE: Record<SortKey, (client: OverviewClient) => number | undefined> = {
  name: () => 0,
  lastLog: (client) =>
    client.lastLoggedAt === undefined
      ? undefined
      : Date.parse(client.lastLoggedAt),
  sessions: (client) =>
    client.sessionsDue === undefined
      ? undefined
      : (client.sessionsDone ?? 0) / client.sessionsDue,
  rpe: (client) => client.averageRpe,
  status: (client) => STATUSES.indexOf(client.status),
}

/** The next sort after a column is chosen: the same column turns around, another starts in its first direction. */
export function nextSort(sort: Sort, key: SortKey): Sort {
  if (sort.key !== key) return { key, direction: FIRST_DIRECTION[key] }
  return {
    key,
    direction: sort.direction === 'ascending' ? 'descending' : 'ascending',
  }
}

/** Sorts clients by a column; a client without a value stays last either way, and a tie goes by name. */
export function sortClients(
  clients: OverviewClient[],
  sort: Sort,
  locale: string,
): OverviewClient[] {
  const sign = sort.direction === 'ascending' ? 1 : -1
  const byName = (a: OverviewClient, b: OverviewClient) =>
    a.name.localeCompare(b.name, locale)
  return [...clients].sort((a, b) => {
    if (sort.key === 'name') return sign * byName(a, b)
    const first = VALUE[sort.key](a)
    const second = VALUE[sort.key](b)
    if (first === undefined || second === undefined) {
      return first === second ? byName(a, b) : first === undefined ? 1 : -1
    }
    return sign * (first - second) || byName(a, b)
  })
}
