import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { Client } from '../api/clients'
import { PlanStateBadge } from './plan-state-badge'

// a value that is not there yet reads as "None yet", not as a bare dash
function NoValue() {
  const { t } = useTranslation()
  return (
    <>
      <span aria-hidden>—</span>
      <span className="sr-only">{t('clients.noValue')}</span>
    </>
  )
}

export function ClientTable({ clients }: { clients: Client[] }) {
  const { t } = useTranslation()

  return (
    <Table data-density="compact">
      <TableHeader>
        <TableRow>
          <TableHead>{t('clients.columns.name')}</TableHead>
          <TableHead>{t('clients.columns.goal')}</TableHead>
          <TableHead>{t('clients.columns.plan')}</TableHead>
          <TableHead>{t('clients.columns.lastLog')}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {clients.map((client) => (
          <TableRow key={client.id}>
            <TableCell className="font-medium">
              <Link
                to={client.id}
                className="focus-visible:ring-ring/50 rounded-sm underline-offset-4 hover:underline focus-visible:ring-3 focus-visible:outline-none"
              >
                {client.name}
              </Link>
            </TableCell>
            <TableCell className="text-muted-foreground">
              {client.goal ? (
                t(`quiz.goal.options.${client.goal}.title`)
              ) : (
                <NoValue />
              )}
            </TableCell>
            <TableCell>
              <PlanStateBadge state={client.planState} />
            </TableCell>
            {/* logs arrive with their feature */}
            <TableCell className="text-muted-foreground">
              <NoValue />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
