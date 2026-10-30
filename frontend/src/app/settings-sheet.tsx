import { useQuery } from '@tanstack/react-query'
import { SlidersHorizontalIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { currentAccountQuery } from '@/features/account/api/account'
import { AccountAvatar } from '@/features/account/components/account-avatar'
import { DisplaySettings } from '@/features/account/components/display-settings'
import { SignOutButton } from '@/features/account/components/sign-out-button'

/** The client's Settings button and the sheet it opens: display settings and "Sign out". */
export function SettingsSheet() {
  const { t } = useTranslation()
  const { data: account } = useQuery(currentAccountQuery)

  return (
    <Sheet>
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
        </div>
      </SheetContent>
    </Sheet>
  )
}
