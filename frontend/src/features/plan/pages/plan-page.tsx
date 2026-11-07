import { CircleAlertIcon } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { Fragment, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useOutletContext } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyContent,
  EmptyHeader,
  EmptyTitle,
} from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { useFollowDrafting, usePlan } from '../api/plans'
import { DraftPanel } from '../components/draft-panel'
import { PlanEditor } from '../components/plan-editor'

type Client = components['schemas']['ClientResponse']

export function PlanPage() {
  const client = useOutletContext<Client>()
  // nothing of one client's editor may stay for the next
  return (
    <Fragment key={client.id}>
      <DraftingEndAnnouncement planState={client.planState} />
      <ClientPlan client={client} />
    </Fragment>
  )
}

// always there, because a live region announces changes only once it exists; the tab moves on with no action to
// follow, after a first drafting and after a rebuild alike, whether the plan changed or not
function DraftingEndAnnouncement({
  planState,
}: {
  planState: Client['planState']
}) {
  const { t } = useTranslation()
  const [sawDrafting, setSawDrafting] = useState(false)
  if (planState === 'PLAN_DRAFTING' && !sawDrafting) setSawDrafting(true)
  return (
    <p role="status" className="sr-only">
      {sawDrafting &&
        planState !== 'PLAN_DRAFTING' &&
        t(`clients.planStates.${planState}`)}
    </p>
  )
}

function ClientPlan({ client }: { client: Client }) {
  const { t } = useTranslation()
  const noQuiz = client.planState === 'NO_QUIZ'
  // an untouched new plan is not stored, so leaving the tab shows "Build plan" again
  const [building, setBuilding] = useState(false)
  // a trainer already building by hand keeps their edits; a save then says the plan is being drafted
  const drafting = client.planState === 'PLAN_DRAFTING' && !building
  // asked for during a drafting too: a rebuild shows the previous draft under it
  const query = usePlan(client.id, !noQuiz)
  const { refetch } = query
  const queryClient = useQueryClient()
  // the drafting stays on screen until the plan read after it has arrived, so the editor never opens on the old one
  const [following, setFollowing] = useState(drafting)
  if (drafting && !following) setFollowing(true)
  // the version Build again was accepted at: the same one after the drafting means it could not build (research R8)
  const [rebuildFrom, setRebuildFrom] = useState<number>()
  const [rebuildFailed, setRebuildFailed] = useState(false)

  useEffect(() => {
    if (drafting || !following) return
    let current = true
    void refetch().then((result) => {
      if (!current) return
      setRebuildFailed(
        rebuildFrom !== undefined &&
          !result.isError &&
          result.data?.version === rebuildFrom,
      )
      setRebuildFrom(undefined)
      setFollowing(false)
    })
    return () => {
      current = false
    }
  }, [drafting, following, refetch, rebuildFrom])

  const showDrafting = drafting || following
  if (showDrafting && !query.data) {
    return <DraftingState client={client} />
  }

  if (noQuiz) {
    return (
      <p className="text-muted-foreground py-4 text-sm">
        {t('plan.empty.noQuiz', { name: client.name })}
      </p>
    )
  }

  // a refresh that fails while the editor is open leaves the editor alone
  if (query.data === undefined) {
    return query.isError ? (
      <Alert variant="destructive" className="mt-4">
        <CircleAlertIcon aria-hidden />
        <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        <AlertAction>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void query.refetch()}
          >
            {t('plan.retry')}
          </Button>
        </AlertAction>
      </Alert>
    ) : (
      <PlanLoading />
    )
  }

  if (query.data === null && !building) {
    return (
      <Empty className="mt-4 border">
        <EmptyHeader>
          <EmptyTitle>{t('plan.empty.noPlan')}</EmptyTitle>
        </EmptyHeader>
        <EmptyContent>
          <Button onClick={() => setBuilding(true)}>{t('plan.build')}</Button>
        </EmptyContent>
      </Empty>
    )
  }

  const plan = query.data
  return (
    <div className="flex flex-wrap items-start gap-6">
      <div className="flex min-w-0 flex-[999_1_600px] flex-col">
        {showDrafting && <RebuildingLine client={client} />}
        {rebuildFailed && (
          <Alert variant="destructive" role="alert" className="mt-4">
            <CircleAlertIcon aria-hidden />
            <AlertDescription>{t('plan.rebuild.failed')}</AlertDescription>
          </Alert>
        )}
        {/* the editor copies its plan once, so the one after a drafting is a new editor */}
        <PlanEditor
          key={showDrafting ? 'drafting' : 'editing'}
          client={client}
          plan={plan}
          readOnly={showDrafting}
        />
      </div>
      {plan?.status === 'DRAFT' && (
        <DraftPanel
          client={client}
          plan={plan}
          drafting={showDrafting}
          onBuildAgain={() => {
            setRebuildFrom(plan.version)
            setRebuildFailed(false)
            // the client was read again before this runs, though the page may not have it yet; a drafting that has
            // already ended, as one with no drafting service does, is followed all the same
            const fresh = queryClient.getQueryData<Client>([
              'clients',
              client.id,
            ])
            if (fresh?.planState !== 'PLAN_DRAFTING') setFollowing(true)
          }}
        />
      )}
    </div>
  )
}

function RebuildingLine({ client }: { client: Client }) {
  const { t } = useTranslation()
  useFollowDrafting(client.id)
  return (
    <div className="flex flex-col gap-2 pt-4">
      <p role="status" className="text-muted-foreground text-sm">
        {t('plan.rebuild.drafting', { name: client.name })}
      </p>
      <Skeleton aria-hidden className="h-2 w-full" />
    </div>
  )
}

function DraftingState({ client }: { client: Client }) {
  const { t } = useTranslation()
  useFollowDrafting(client.id)
  return (
    <div className="flex flex-col gap-2 pt-4">
      <p role="status" className="text-muted-foreground text-sm">
        {t('plan.drafting', { name: client.name })}
      </p>
      <PlanSkeleton />
    </div>
  )
}

function PlanLoading() {
  const { t } = useTranslation()
  return (
    <>
      <span className="sr-only" role="status">
        {t('plan.loading')}
      </span>
      <PlanSkeleton />
    </>
  )
}

function PlanSkeleton() {
  return (
    <div className="flex flex-col gap-6 pt-4">
      <div aria-hidden className="flex flex-wrap items-end gap-4">
        <Skeleton className="h-14 w-56" />
        <Skeleton className="h-14 w-32" />
        <Skeleton className="ml-auto h-9 w-48" />
      </div>
      <Skeleton aria-hidden className="h-9 w-96 max-w-full" />
      <div aria-hidden className="flex flex-wrap items-start gap-6">
        <div className="flex min-w-0 flex-[999_1_600px] flex-col gap-2">
          {Array.from({ length: 5 }, (_, row) => (
            <Skeleton key={row} className="h-9 w-full" />
          ))}
        </div>
        <Skeleton className="h-40 max-w-full flex-[1_1_280px]" />
      </div>
    </div>
  )
}
