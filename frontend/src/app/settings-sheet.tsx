import { useQuery } from '@tanstack/react-query'
import { SlidersHorizontalIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import {
  accountDeletionQuery,
  currentAccountQuery,
} from '@/features/account/api/account'
import { DeleteAccountDialog } from '@/features/account/components/delete-account-dialog'
import { AccountAvatar } from '@/components/account-avatar'
import { DisplaySettings } from '@/features/account/components/display-settings'
import { SignOutButton } from '@/features/account/components/sign-out-button'
import { firstName } from '@/lib/first-name'

/**
 * The client's settings sheet: display settings, "Sign out", deleting the account and the privacy notice.
 * `children` holds its `SettingsButton`, by default the button alone; a page that remounts inside keeps the sheet open.
 */
export function SettingsSheet({
  children = <SettingsButton />,
}: {
  children?: ReactNode
}) {
  const { t } = useTranslation()
  const { data: account } = useQuery(currentAccountQuery)

  return (
    <Sheet>
      {children}
      <SheetContent
        side="right"
        className="gap-6 data-[side=right]:w-full data-[side=right]:sm:max-w-sm"
      >
        <SheetHeader>
          <SheetTitle>{t('nav.settings')}</SheetTitle>
        </SheetHeader>
        <div className="flex flex-col gap-6 px-4 pb-6">
          {account && (
            <>
              <div className="flex min-w-0 items-center gap-3">
                <AccountAvatar name={account.name} size="lg" />
                <div className="min-w-0">
                  <p className="truncate font-semibold">{account.name}</p>
                  <p className="text-muted-foreground truncate text-sm">
                    {account.email}
                  </p>
                </div>
              </div>
              <DisplaySettings account={account} />
              <p className="text-muted-foreground text-center text-sm">
                {t('settings.sheet.autosave')}
              </p>
            </>
          )}
          <SignOutButton variant="outline" size="touch" className="w-full" />
          <ClientDeleteAccount />
          <Link
            to="/privacy"
            className="text-muted-foreground hover:text-foreground flex min-h-12 items-center justify-center text-sm underline underline-offset-3"
          >
            {t('settings.privacyNotice')}
          </Link>
        </div>
      </SheetContent>
    </Sheet>
  )
}

/** The button that opens the `SettingsSheet` it is placed in. */
export function SettingsButton() {
  const { t } = useTranslation()

  return (
    <SheetTrigger asChild>
      <Button
        variant="ghost"
        size="icon-lg"
        className="size-12"
        aria-label={t('nav.settings')}
      >
        <SlidersHorizontalIcon aria-hidden="true" />
      </Button>
    </SheetTrigger>
  )
}

// read when the sheet opens, so the confirmation can name the trainer
function ClientDeleteAccount() {
  const { t } = useTranslation()
  const { data } = useQuery(accountDeletionQuery)
  const trainer = data?.trainerName ? firstName(data.trainerName) : undefined

  return (
    <DeleteAccountDialog
      description={
        trainer
          ? t('settings.delete.clientDescription', { trainer })
          : t('settings.delete.clientDescriptionNoName')
      }
      trigger={{
        variant: 'ghost',
        size: 'touch',
        className: 'text-destructive hover:text-destructive w-full',
      }}
    />
  )
}
