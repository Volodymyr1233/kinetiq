import { ChevronDownIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { OwnPlan } from '../api/client-plan'

type Session = OwnPlan['sessions'][number]

/** One session of the week, closed to its name and opened to its exercises and their targets. */
export function SessionCard({ session }: { session: Session }) {
  const { t } = useTranslation()
  const unit = useWeightUnit()

  return (
    <Collapsible className="bg-card text-card-foreground rounded-xl border">
      <CollapsibleTrigger className="group focus-visible:ring-ring/50 flex min-h-12 w-full items-center justify-between gap-3 rounded-xl px-4 py-2 text-left outline-none focus-visible:ring-3">
        <span className="flex min-w-0 flex-col">
          <span className="font-medium break-words">{session.name}</span>
          <span className="text-muted-foreground text-sm">
            {t('plan.client.exercises', { count: session.entries.length })}
          </span>
        </span>
        <ChevronDownIcon
          aria-hidden
          className="text-muted-foreground size-5 shrink-0 transition-transform group-data-[state=open]:rotate-180 motion-reduce:transition-none"
        />
      </CollapsibleTrigger>
      <CollapsibleContent>
        <ol className="flex flex-col divide-y border-t">
          {session.entries.map((entry, index) => (
            <li
              // the same exercise can appear twice in a session, so its place is its key
              key={index}
              className="flex flex-col gap-1 px-4 py-3"
            >
              <span className="font-medium break-words">
                {entry.exerciseName}
              </span>
              <span className="text-muted-foreground flex flex-wrap gap-x-3 gap-y-0.5 text-sm">
                <span className="num">
                  {t('plan.client.entry.target', {
                    sets: entry.sets,
                    amount: entry.amount,
                    unit: t(`plan.entries.measures.${entry.measure}`),
                  })}
                </span>
                <span className="num">
                  {entry.loadKg === undefined
                    ? t('plan.client.entry.noLoad')
                    : t('plan.client.entry.load', {
                        load: fromKg(entry.loadKg, unit),
                        unit: t(`plan.units.${unit}`),
                      })}
                </span>
                <span className="num">
                  {t('plan.client.entry.rpe', { rpe: entry.rpe })}
                </span>
                <span className="num">
                  {t('plan.client.entry.rest', { seconds: entry.restSeconds })}
                </span>
              </span>
            </li>
          ))}
        </ol>
      </CollapsibleContent>
    </Collapsible>
  )
}
