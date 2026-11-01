import { CircleAlertIcon, PlusIcon } from 'lucide-react'
import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
  InputGroupText,
} from '@/components/ui/input-group'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { errorMessageKey } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { useWeightUnit } from '@/lib/weight'
import { useSavePlan, type Plan } from '../api/plans'
import {
  addSession,
  dayName,
  draftFromPlan,
  GOALS,
  isDirty,
  MAX_SESSIONS,
  newDraft,
  removeSession,
  requestFromDraft,
  updateSession,
  type Draft,
  type DraftSession,
  type Goal,
} from '../plan-draft'
import { SessionPanel } from './session-panel'

type Client = components['schemas']['ClientResponse']

/**
 * Holds a local draft until Save. The plan seeds it once; after that the query is not read again, so a refresh in the
 * background never overwrites what the trainer is typing.
 */
export function PlanEditor({
  client,
  plan,
}: {
  client: Client
  plan: Plan | null
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const unit = useWeightUnit()
  const savePlan = useSavePlan(client.id)
  const [start] = useState<Draft>(() =>
    // the plan tab offers a new plan only after the quiz, which gives the client a goal
    plan
      ? draftFromPlan(plan, unit, i18n.language)
      : newDraft(client.goal ?? GOALS[0]),
  )
  const [saved, setSaved] = useState({ plan, draft: start })
  const [draft, setDraft] = useState(start)
  const [selectedKey, setSelectedKey] = useState(start.sessions[0]?.key)
  const [focusNameOf, setFocusNameOf] = useState<string>()
  const selected = draft.sessions.find((session) => session.key === selectedKey)

  const dirty = isDirty(draft, saved.draft)
  const atLimit = draft.sessions.length >= MAX_SESSIONS

  function onAddSession() {
    const { draft: next, key } = addSession(draft)
    setDraft(next)
    setSelectedKey(key)
    setFocusNameOf(key)
  }

  function onChangeSession(
    key: string,
    change: Partial<Pick<DraftSession, 'name' | 'day' | 'entries'>>,
  ) {
    setDraft((current) => updateSession(current, key, change))
  }

  function onDeleteSession(key: string) {
    const { draft: next, selected: nextKey } = removeSession(draft, key)
    setDraft(next)
    setSelectedKey(nextKey)
  }

  function save() {
    savePlan.mutate(requestFromDraft(draft, saved.plan?.version, unit), {
      onSuccess: (stored) => {
        const next = draftFromPlan(stored, unit, i18n.language)
        // the new draft has new keys; the selection follows the session by its day, which is unique
        const day = selected?.day
        setSaved({ plan: stored, draft: next })
        setDraft(next)
        setSelectedKey(
          next.sessions.find((session) => session.day === day)?.key ??
            next.sessions[0]?.key,
        )
        toast.success(t('plan.toast.saved'))
      },
    })
  }

  const status = saved.plan?.status ?? 'DRAFT'

  return (
    <div className="flex flex-col gap-6 pt-4">
      <div className="flex flex-wrap items-end gap-x-4 gap-y-3">
        <Field className="w-56 max-w-full">
          <FieldLabel htmlFor={`${id}-goal`}>
            {t('plan.fields.goal')}
          </FieldLabel>
          <Select
            value={draft.goal}
            onValueChange={(goal) =>
              setDraft((current) => ({ ...current, goal: goal as Goal }))
            }
          >
            <SelectTrigger id={`${id}-goal`} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {GOALS.map((goal) => (
                <SelectItem key={goal} value={goal}>
                  {t(`quiz.goal.options.${goal}.title`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Field>
        <Field className="w-32">
          <FieldLabel htmlFor={`${id}-weeks`}>
            {t('plan.fields.weeks')}
          </FieldLabel>
          <InputGroup>
            <InputGroupInput
              id={`${id}-weeks`}
              inputMode="numeric"
              autoComplete="off"
              className="num"
              value={draft.weeks}
              onChange={(event) => {
                const weeks = event.target.value
                setDraft((current) => ({ ...current, weeks }))
              }}
            />
            <InputGroupAddon align="inline-end">
              <InputGroupText>{t('plan.fields.weeksUnit')}</InputGroupText>
            </InputGroupAddon>
          </InputGroup>
        </Field>
        <div className="flex h-9 flex-wrap items-center gap-3">
          <Badge variant={status === 'PUBLISHED' ? 'success' : 'outline'}>
            {t(`plan.status.${status}`)}
          </Badge>
          {saved.plan?.currentWeek !== undefined && (
            <span className="text-muted-foreground text-sm">
              {saved.plan.currentWeek > saved.plan.weeks
                ? t('plan.finished', { weeks: saved.plan.weeks })
                : t('plan.week', {
                    week: saved.plan.currentWeek,
                    weeks: saved.plan.weeks,
                  })}
            </span>
          )}
        </div>
        <div className="ml-auto flex flex-wrap items-center gap-3">
          <span
            role="status"
            className="text-muted-foreground text-sm empty:hidden"
          >
            {dirty ? t('plan.unsaved') : t('plan.nothingToSave')}
          </span>
          <Button
            disabled={!dirty || savePlan.isPending}
            aria-busy={savePlan.isPending}
            onClick={save}
          >
            {savePlan.isPending && (
              <Spinner data-icon="inline-start" aria-hidden="true" />
            )}
            {t(savePlan.isPending ? 'plan.saving' : 'plan.save')}
          </Button>
        </div>
      </div>

      {savePlan.isError && (
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>
            {t(errorMessageKey(savePlan.error), { name: client.name })}
          </AlertDescription>
        </Alert>
      )}

      {draft.sessions.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-lg border border-dashed p-6">
          <p className="text-muted-foreground text-sm">
            {t('plan.sessions.empty')}
          </p>
          <Button variant="outline" onClick={onAddSession}>
            <PlusIcon data-icon="inline-start" aria-hidden="true" />
            {t('plan.sessions.add')}
          </Button>
        </div>
      ) : (
        <Tabs value={selected?.key} onValueChange={setSelectedKey}>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <TabsList
              variant="line"
              aria-label={t('plan.sessions.label')}
              className="h-auto! flex-wrap justify-start"
            >
              {draft.sessions.map((session, index) => (
                <TabsTrigger
                  key={session.key}
                  value={session.key}
                  className="max-w-full flex-none whitespace-normal"
                >
                  <span className="num text-muted-foreground">
                    {String.fromCharCode(65 + index)}
                  </span>
                  <span>
                    {session.name.trim() || t('plan.session.unnamed')}
                  </span>
                  <span className="text-muted-foreground font-normal">
                    · {dayName(session.day, i18n.language, 'short')}
                  </span>
                </TabsTrigger>
              ))}
            </TabsList>
            <Button
              variant="outline"
              size="sm"
              disabled={atLimit}
              onClick={onAddSession}
            >
              <PlusIcon data-icon="inline-start" aria-hidden="true" />
              {t('plan.sessions.add')}
            </Button>
            {atLimit && (
              <span className="text-muted-foreground text-sm">
                {t('plan.sessions.limit', { count: MAX_SESSIONS })}
              </span>
            )}
          </div>
          {selected && (
            <SessionPanel
              key={selected.key}
              session={selected}
              sessions={draft.sessions}
              unit={unit}
              autoFocusName={focusNameOf === selected.key}
              onChange={(change) => onChangeSession(selected.key, change)}
              onDelete={() => onDeleteSession(selected.key)}
            />
          )}
        </Tabs>
      )}
    </div>
  )
}
