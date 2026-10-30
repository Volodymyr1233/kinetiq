import {
  BookIcon,
  ListChecksIcon,
  MessageCircleIcon,
  SlidersHorizontalIcon,
  UsersIcon,
} from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import {
  matchPath,
  NavLink,
  Outlet,
  useLocation,
  useOutletContext,
} from 'react-router'
import { PlaceholderPage } from '@/app/placeholder-page'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from '@/components/ui/sidebar'
import { currentAccountQuery } from '@/features/account/api/account'
import { AccountAvatar } from '@/features/account/components/account-avatar'
import { SignOutButton } from '@/features/account/components/sign-out-button'

const NAV_ITEMS = [
  { path: 'clients', labelKey: 'nav.clients', Icon: UsersIcon },
  { path: 'approvals', labelKey: 'nav.approvals', Icon: ListChecksIcon },
  { path: 'messages', labelKey: 'nav.messages', Icon: MessageCircleIcon },
  { path: 'exercises', labelKey: 'nav.exercises', Icon: BookIcon },
  { path: 'settings', labelKey: 'nav.settings', Icon: SlidersHorizontalIcon },
] as const

export function TrainerArea() {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const { data: account } = useQuery(currentAccountQuery)
  const active = NAV_ITEMS.find((item) =>
    matchPath({ path: `/trainer/${item.path}`, end: false }, pathname),
  )
  const title = active ? t(active.labelKey) : ''

  return (
    <SidebarProvider>
      <Sidebar
        collapsible="none"
        className="border-sidebar-border sticky top-0 h-svh border-r"
      >
        <SidebarHeader className="flex-row items-center gap-2 p-4 font-semibold">
          {/* ink tile with a lime letter, as in the mockups */}
          <span className="bg-brand-foreground text-brand flex size-8 items-center justify-center rounded-md">
            K
          </span>
          Kinetiq
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <nav aria-label={t('areas.trainer')}>
              <SidebarMenu>
                {NAV_ITEMS.map(({ path, labelKey, Icon }) => (
                  <SidebarMenuItem key={path}>
                    <SidebarMenuButton asChild isActive={active?.path === path}>
                      <NavLink to={path}>
                        <Icon aria-hidden />
                        <span>{t(labelKey)}</span>
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </nav>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter className="border-sidebar-border gap-3 border-t p-4">
          {account && (
            <div className="flex min-w-0 items-center gap-2.5">
              <AccountAvatar name={account.name} />
              <div className="min-w-0 text-sm">
                <p className="truncate font-medium">{account.name}</p>
                <p className="text-muted-foreground text-xs">
                  {t(`account.roles.${account.role}`)}
                </p>
              </div>
            </div>
          )}
          <SignOutButton variant="outline" className="w-full" />
        </SidebarFooter>
      </Sidebar>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="bg-background sticky top-0 z-10 flex h-14 items-center border-b px-6">
          <p className="text-sm font-medium">{title}</p>
        </header>
        <main className="flex-1">
          <Outlet context={title} />
        </main>
      </div>
    </SidebarProvider>
  )
}

// the five sections show this until their features are built
export function TrainerPlaceholder() {
  return <PlaceholderPage title={useOutletContext<string>()} />
}
