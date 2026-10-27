import { useQuery } from '@tanstack/react-query'
import { MailIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { errorMessageKey } from '@/lib/api/problem'
import { currentAccountQuery } from '../api/account'
import { useResendLink } from '../use-resend-link'

/**
 * Asks a client who joined with a copied link to confirm their email, without blocking anything. It disappears once the
 * account is confirmed: the current account is fetched again when the person returns to the tab.
 */
export function ConfirmEmailBanner() {
  const { t } = useTranslation()
  const { data: account } = useQuery(currentAccountQuery)
  const { request, locked, send } = useResendLink()

  if (account?.emailConfirmed !== false) return null

  return (
    <div className="bg-muted mb-3 flex items-center gap-2.5 px-4 py-1 text-sm">
      <MailIcon aria-hidden="true" className="size-4 shrink-0" />
      <p className="min-w-0 flex-1 break-words">
        {t('account.banner.text', { email: account.email })}
      </p>
      <Button
        variant="link"
        className="min-h-11 shrink-0 px-2"
        disabled={locked || request.isPending}
        onClick={() => send(account.email)}
      >
        {t(locked ? 'account.banner.sent' : 'account.banner.resend')}
      </Button>
      <p role="status" className="sr-only">
        {request.isError
          ? t(errorMessageKey(request.error))
          : locked && t('account.banner.resent')}
      </p>
    </div>
  )
}
