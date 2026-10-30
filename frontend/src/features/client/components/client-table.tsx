import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { Client } from '../api/clients'

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
            <TableCell className="font-medium">{client.name}</TableCell>
            <TableCell className="text-muted-foreground">
              {client.goal ? (
                t(`quiz.goal.options.${client.goal}.title`)
              ) : (
                <NoValue />
              )}
            </TableCell>
            <TableCell>
              {/* the text carries the state; amber only backs it up */}
              <Badge
                variant={
                  client.planState === 'PLAN_TO_REVIEW' ? 'warning' : 'outline'
                }
              >
                {t(`clients.planStates.${client.planState}`)}
              </Badge>
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
