import { UserRoundIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AccountAvatar } from '@/components/account-avatar'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { acceptedBy, type PlanChangeSide } from '@/lib/plan-change-state'
import { cn } from '@/lib/utils'

type Decided = {
  acceptedByTrainer: boolean
  acceptedByClient: boolean
  declinedBy?: PlanChangeSide
}

function Person({
  name,
  state,
  you = false,
}: {
  name: string
  state: 'accepted' | 'undecided' | 'kept'
  you?: boolean
}) {
  const { t } = useTranslation()
  return (
    <li className="flex items-center gap-3">
      {you ? (
        <Avatar aria-hidden="true">
          <AvatarFallback className="text-foreground">
            <UserRoundIcon aria-hidden className="size-4" />
          </AvatarFallback>
        </Avatar>
      ) : (
        <AccountAvatar name={name} />
      )}
      {/* the state is said in words; the dot only repeats it */}
      <span>
        {t('planChange.people.row', {
          name,
          state: t(`planChange.people.${state}`),
        })}
      </span>
      <span
        aria-hidden
        className={cn(
          'size-2 rounded-full',
          state === 'accepted' && 'bg-success',
          state === 'kept' && 'bg-foreground',
          state === 'undecided' && 'bg-muted-foreground',
        )}
      />
    </li>
  )
}

/** The two people of a plan change and what each has done, as `viewer` reads it ("You · accepted"). */
export function PlanChangePeople({
  planChange,
  viewer,
  otherName,
}: {
  planChange: Decided
  viewer: PlanChangeSide
  otherName: string
}) {
  const { t } = useTranslation()
  const other = viewer === 'TRAINER' ? 'CLIENT' : 'TRAINER'
  const stateOf = (side: PlanChangeSide) =>
    planChange.declinedBy === side
      ? 'kept'
      : acceptedBy(planChange, side)
        ? 'accepted'
        : 'undecided'

  return (
    <ul
      aria-label={t('planChange.people.heading')}
      className="flex flex-col gap-2 text-sm"
    >
      <Person name={t('planChange.people.you')} state={stateOf(viewer)} you />
      <Person name={otherName} state={stateOf(other)} />
    </ul>
  )
}
