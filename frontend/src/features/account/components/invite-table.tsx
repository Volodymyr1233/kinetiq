import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import type { Invite } from '../api/invites'
import { daysLeft, sentDay } from '../invite-format'

const HINT_ID = 'copy-new-link-hint'

export function InviteTable({
  invites,
  copyingId,
  copiedId,
  onCopyNewLink,
  onRevoke,
}: {
  invites: Invite[]
  copyingId?: string
  copiedId?: string
  onCopyNewLink: (invite: Invite) => void
  onRevoke: (invite: Invite) => void
}) {
  const { t, i18n } = useTranslation()
  const now = new Date()

  return (
    <TooltipProvider>
      {/* the hint is read with every "Copy new link" button; the tooltip shows it on hover and focus */}
      <p id={HINT_ID} hidden>
        {t('invites.copyNewLinkHint')}
      </p>
      <Table data-density="compact">
        <TableHeader>
          <TableRow>
            <TableHead>{t('invites.columns.email')}</TableHead>
            <TableHead>{t('invites.columns.sent')}</TableHead>
            <TableHead>{t('invites.columns.expires')}</TableHead>
            <TableHead>
              <span className="sr-only">{t('invites.columns.actions')}</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invites.map((invite) => {
            const days = daysLeft(invite.expiresAt, now)
            const copying = copyingId === invite.id
            return (
              <TableRow key={invite.id}>
                <TableCell className="font-medium">{invite.email}</TableCell>
                <TableCell className="text-muted-foreground">
                  {sentDay(invite.sentAt, now, i18n.language) ??
                    t('invites.today')}
                </TableCell>
                <TableCell>
                  {/* the text itself says "In 1 day"; amber only backs it up */}
                  <Badge variant={days === 1 ? 'warning' : 'outline'}>
                    {t('invites.daysLeft', { count: days })}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={copying}
                          aria-busy={copying}
                          aria-describedby={HINT_ID}
                          onClick={() => onCopyNewLink(invite)}
                        >
                          {copying && (
                            <Spinner
                              data-icon="inline-start"
                              aria-hidden="true"
                            />
                          )}
                          {copiedId === invite.id
                            ? t('invites.copied')
                            : t('invites.copyNewLink')}
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>
                        {t('invites.copyNewLinkHint')}
                      </TooltipContent>
                    </Tooltip>
                    <Button
                      variant="outline"
                      size="sm"
                      aria-label={t('invites.revokeFor', {
                        email: invite.email,
                      })}
                      onClick={() => onRevoke(invite)}
                    >
                      {t('invites.revoke')}
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </TooltipProvider>
  )
}
