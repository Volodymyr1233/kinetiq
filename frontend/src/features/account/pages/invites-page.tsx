import { CircleAlertIcon, UserPlusIcon } from 'lucide-react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import {
  useInvites,
  useReplaceInviteLink,
  useRevokeInvite,
  type Invite,
} from '../api/invites'
import { InviteDialog, NewLinkDialog } from '../components/invite-dialog'
import { InviteTable } from '../components/invite-table'
import { RevokeInviteDialog } from '../components/revoke-invite-dialog'
import { copyText } from '../copy-text'

const COPIED_FOR_MS = 2000

// design system: errors stay until dismissed, confirmations go after 4 seconds
function showError(message: string) {
  toast.error(message, { duration: Infinity, closeButton: true })
}

export function InvitesPage() {
  const { t } = useTranslation()
  const query = useInvites()
  const replaceLink = useReplaceInviteLink()
  const revoke = useRevokeInvite()
  const [copiedId, setCopiedId] = useState<string>()
  const [revoking, setRevoking] = useState<Invite>()
  const heading = useRef<HTMLHeadingElement>(null)
  const [uncopied, setUncopied] = useState<{ email: string; link: string }>()
  const invites = query.data?.invites ?? []

  function copyNewLink(invite: Invite) {
    if (replaceLink.isPending) return
    // started inside the click, so Safari lets the link be written once it arrives
    const link = replaceLink.mutateAsync(invite.id)
    copyText(link).then(
      () => {
        setCopiedId(invite.id)
        setTimeout(() => setCopiedId(undefined), COPIED_FOR_MS)
        toast.success(t('invites.newLinkCopied'))
      },
      // a refused request is reported by its code; a link the clipboard refused is shown to copy by hand
      () =>
        link.then(
          (value) => setUncopied({ email: invite.email, link: value }),
          (error: unknown) => showError(t(errorMessageKey(error))),
        ),
    )
  }

  function confirmRevoke(invite: Invite) {
    revoke.mutate(invite.id, {
      onSuccess: () => toast.success(t('invites.revokeDialog.revoked')),
      onError: (error) => showError(t(errorMessageKey(error))),
      onSettled: () => setRevoking(undefined),
    })
  }

  const inviteButton = (variant: 'default' | 'outline') => (
    <InviteDialog>
      <Button variant={variant} size={variant === 'outline' ? 'sm' : 'default'}>
        {variant === 'default' && (
          <UserPlusIcon data-icon="inline-start" aria-hidden="true" />
        )}
        {t('invites.inviteClient')}
      </Button>
    </InviteDialog>
  )

  return (
    <div className="flex max-w-240 flex-col gap-6 px-6 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex max-w-prose flex-col gap-1">
          <h1
            ref={heading}
            tabIndex={-1}
            className="font-heading text-2xl font-semibold outline-none"
          >
            {t('invites.title')}
          </h1>
          <p className="text-muted-foreground text-sm">
            {t('invites.description')}
          </p>
        </div>
        {inviteButton('default')}
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
              {t('invites.retry')}
            </Button>
          </AlertAction>
        </Alert>
      ) : query.isPending ? (
        <div className="flex flex-col gap-2">
          <span className="sr-only" role="status">
            {t('invites.loading')}
          </span>
          {Array.from({ length: 3 }, (_, row) => (
            <Skeleton key={row} aria-hidden className="h-9 w-full" />
          ))}
        </div>
      ) : invites.length === 0 ? (
        <Empty className="bg-muted border border-dashed">
          <EmptyHeader>
            <EmptyDescription>{t('invites.empty')}</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>{inviteButton('outline')}</EmptyContent>
        </Empty>
      ) : (
        <div className="rounded-lg border">
          <InviteTable
            invites={invites}
            copyingId={
              replaceLink.isPending ? replaceLink.variables : undefined
            }
            copiedId={copiedId}
            onCopyNewLink={copyNewLink}
            onRevoke={(invite) => {
              revoke.reset()
              setRevoking(invite)
            }}
          />
        </div>
      )}

      <RevokeInviteDialog
        invite={revoking}
        busy={revoke.isPending}
        // after a request the row may be gone, so focus goes to the heading instead of its button
        revoked={revoke.isSuccess || revoke.isError}
        onRevoked={() => heading.current?.focus()}
        onConfirm={confirmRevoke}
        onCancel={() => setRevoking(undefined)}
      />
      {uncopied && (
        <NewLinkDialog
          email={uncopied.email}
          link={uncopied.link}
          onClose={() => setUncopied(undefined)}
        />
      )}
    </div>
  )
}
