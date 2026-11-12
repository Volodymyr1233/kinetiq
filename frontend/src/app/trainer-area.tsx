import {
  BookIcon,
  ListChecksIcon,
  MessageCircleIcon,
  SlidersHorizontalIcon,
  UsersIcon,
} from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { matchPath, NavLink, Outlet, useLocation } from 'react-router'
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
  SidebarTrigger,
  useSidebar,
} from '@/components/ui/sidebar'
import { currentAccountQuery } from '@/features/account/api/account'
import { AccountAvatar } from '@/components/account-avatar'
import { SignOutButton } from '@/features/account/components/sign-out-button'
import { useUnreadCount } from '@/features/messaging/api/messaging'
import { UnreadCount } from '@/features/messaging/components/unread-count'
import { useDraftedPlans } from '@/features/plan/api/drafted-plans'
import { usePlanChangeApprovals } from '@/features/plan/api/plan-change-approvals'
import { waitsForDecisionOf } from '@/lib/plan-change-state'
import { Badge } from '@/components/ui/badge'

// drafted plans and the plan changes that wait for the trainer; like the unread count, the whole number is hidden text
// that joins the link's name
function ApprovalsCount() {
  const { t } = useTranslation()
  const drafts = useDraftedPlans().data
  const changes = usePlanChangeApprovals().data
  const count =
    (drafts?.items.length ?? 0) +
    (changes?.items.filter((change) => waitsForDecisionOf(change, 'TRAINER'))
      .length ?? 0)
  if (count <= 0) return null
  return (
    <>
      <span className="sr-only">{t('nav.approvalsCount', { count })}</span>
      <Badge
        aria-hidden
        variant="secondary"
        className="num ml-auto h-[18px] min-w-[18px] px-[5px]"
      >
        {count}
      </Badge>
    </>
  )
}

const NAV_ITEMS = [
  { path: 'clients', labelKey: 'nav.clients', Icon: UsersIcon },
  { path: 'approvals', labelKey: 'nav.approvals', Icon: ListChecksIcon },
  { path: 'messages', labelKey: 'nav.messages', Icon: MessageCircleIcon },
  { path: 'exercises', labelKey: 'nav.exercises', Icon: BookIcon },
  { path: 'settings', labelKey: 'nav.settings', Icon: SlidersHorizontalIcon },
] as const

// ponytail: on a narrow screen the sidebar is a sheet behind a button, so a trainer's page never scrolls sideways;
// story #179 brings the real phone layout with its bottom tab bar
function TrainerSidebar({ activePath }: { activePath?: string }) {
  const { t } = useTranslation()
  const { isMobile, setOpenMobile } = useSidebar()
  const { data: account } = useQuery(currentAccountQuery)
  const { data: unread } = useUnreadCount()

  return (
    <Sidebar
      collapsible={isMobile ? 'offcanvas' : 'none'}
      mobileLabel={t('nav.menu')}
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
                  <SidebarMenuButton asChild isActive={activePath === path}>
                    <NavLink to={path} onClick={() => setOpenMobile(false)}>
                      <Icon aria-hidden />
                      <span>{t(labelKey)}</span>
                      {path === 'approvals' && <ApprovalsCount />}
                      {path === 'messages' && (
                        <UnreadCount
                          count={unread?.count ?? 0}
                          className="ml-auto"
                        />
                      )}
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
  )
}

export function TrainerArea() {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const active = NAV_ITEMS.find((item) =>
    matchPath({ path: `/trainer/${item.path}`, end: false }, pathname),
  )
  const title = active ? t(active.labelKey) : ''

  return (
    <SidebarProvider>
      <TrainerSidebar activePath={active?.path} />
      <div className="flex min-w-0 flex-1 flex-col">
        <header
          data-slot="trainer-header"
          className="bg-background sticky top-0 z-10 flex h-14 items-center gap-2 border-b px-6"
        >
          <SidebarTrigger aria-label={t('nav.open')} className="md:hidden" />
          <p className="text-sm font-medium">{title}</p>
        </header>
        <main className="flex-1">
          <Outlet />
        </main>
      </div>
    </SidebarProvider>
  )
}
