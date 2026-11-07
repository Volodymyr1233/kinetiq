import { CircleAlertIcon, PlusIcon } from 'lucide-react'
import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { LeaveGuard } from '@/components/leave-guard'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
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
import { useDialogFocus } from '@/hooks/use-dialog-focus'
import type { components } from '@/lib/api/schema'
import { cn } from '@/lib/utils'
import { useWeightUnit } from '@/lib/weight'
import { usePublishPlan, useSavePlan, type Plan } from '../api/plans'
import {
  addSession,
  dayName,
  draftFromPlan,
  GOALS,
  isDirty,
  MAX_SESSIONS,
  newDraft,
  problemsFromServer,
  removeSession,
  requestFromDraft,
  target,
  updateSession,
  validateDraft,
  type Draft,
  type DraftSession,
  type Goal,
  type Problem,
} from '../plan-draft'
import { AutomaticDraftNote } from './automatic-draft-note'
import { SessionPanel } from './session-panel'
import { ValidationSummary } from './validation-summary'

type Client = components['schemas']['ClientResponse']

const letter = (index: number) => String.fromCharCode(65 + index)

/**
 * Holds a local draft until Save. The plan seeds it once; after that the query is not read again, so a refresh in the
 * background never overwrites what the trainer is typing. `readOnly` shows the plan with every change, Save and
 * Publish unavailable, while its sessions can still be read one by one.
 */
export function PlanEditor({
  client,
  plan,
  readOnly = false,
}: {
  client: Client
  plan: Plan | null
  readOnly?: boolean
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const unit = useWeightUnit()
  const savePlan = useSavePlan(client.id)
  const publishPlan = usePublishPlan(client.id)
  const [start] = useState<Draft>(() =>
    // the plan tab offers a new plan only after the quiz, which gives the client a goal
    plan
      ? draftFromPlan(plan, unit, i18n.language)
      : newDraft(client.goal ?? GOALS[0]),
  )
  const [saved, setSaved] = useState({ plan, draft: start })
  const [draft, setDraft] = useState(start)
  const [selectedKey, setSelectedKey] = useState(start.sessions[0]?.key)
  const [attempt, setAttempt] = useState<'save' | 'publish'>()
  const [refusals, setRefusals] = useState<Problem[]>([])
  const [leftFields, setLeftFields] = useState<ReadonlySet<string>>(new Set())
  const [deletingKey, setDeletingKey] = useState<string>()
  const [confirmingPublish, setConfirmingPublish] = useState(false)
  // saving and publishing are one visible step
  const [publishing, setPublishing] = useState(false)
  const [focusRequest, setFocusRequest] = useState<{ run: () => void }>()
  const deleteFocus = useDialogFocus()
  const publishFocus = useDialogFocus()
  const tabsRef = useRef<HTMLDivElement>(null)
  const selected = draft.sessions.find((session) => session.key === selectedKey)
  const published = saved.plan?.status === 'PUBLISHED'

  const dirty = isDirty(draft, saved.draft)
  const busy = savePlan.isPending || publishing
  const problems = validateDraft(
    draft,
    published || attempt === 'publish' ? 'complete' : 'draft',
    unit,
  )
  const shownProblems = [...(attempt ? problems : []), ...refusals]
  // nothing is left of a failed attempt, so a later slip shows at its field only
  if (attempt && shownProblems.length === 0) setAttempt(undefined)

  // focus can only land once the session or row it is meant for is on screen
  useEffect(() => {
    focusRequest?.run()
  }, [focusRequest])

  const focusOn = (elementId: string) =>
    setFocusRequest({ run: () => document.getElementById(elementId)?.focus() })

  function change(next: (current: Draft) => Draft) {
    setDraft(next)
    // what the server said about a field no longer holds once anything may have fixed it
    setRefusals((current) => current.filter((problem) => !problem.target))
  }

  function sessionLabel(key: string) {
    const index = draft.sessions.findIndex((session) => session.key === key)
    return t('plan.session.withLetter', {
      letter: letter(index),
      name: draft.sessions[index]?.name.trim() || t('plan.session.unnamed'),
    })
  }

  function errorOf(elementId: string) {
    const refusal = refusals.find((problem) => problem.target === elementId)
    const problem =
      refusal ??
      (attempt || leftFields.has(elementId)
        ? problems.find((candidate) => candidate.target === elementId)
        : undefined)
    return problem && t(problem.message, problem.values)
  }

  const leaveField = (elementId: string) =>
    setLeftFields((current) => new Set(current).add(elementId))

  function onAddSession() {
    const { draft: next, key } = addSession(draft)
    change(() => next)
    setSelectedKey(key)
    focusOn(target.name(key))
  }

  function onChangeSession(
    key: string,
    values: Partial<Pick<DraftSession, 'name' | 'day' | 'entries'>>,
  ) {
    change((current) => updateSession(current, key, values))
  }

  function deleteSession(key: string) {
    const { draft: next, selected: nextKey } = removeSession(draft, key)
    change(() => next)
    setSelectedKey(nextKey)
    const focusAfter = () =>
      nextKey
        ? tabsRef.current
            ?.querySelector<HTMLElement>('[role="tab"][aria-selected="true"]')
            ?.focus()
        : document.getElementById(target.addSession)?.focus()
    return focusAfter
  }

  function onDeleteSession(session: DraftSession) {
    if (session.entries.length > 0) {
      setDeletingKey(session.key)
    } else {
      setFocusRequest({ run: deleteSession(session.key) })
    }
  }

  function onGoTo(problem: Problem) {
    if (problem.session) setSelectedKey(problem.session)
    if (problem.target) focusOn(problem.target)
  }

  // the new draft has new keys, so the selection follows the session by its day, which is unique
  function keepSaved(stored: Plan) {
    const next = draftFromPlan(stored, unit, i18n.language)
    const day = selected?.day
    setSaved({ plan: stored, draft: next })
    setDraft(next)
    setSelectedKey(
      next.sessions.find((session) => session.day === day)?.key ??
        next.sessions[0]?.key,
    )
    setAttempt(undefined)
    setRefusals([])
    setLeftFields(new Set())
  }

  async function store() {
    const stored = await savePlan.mutateAsync(
      requestFromDraft(draft, saved.plan?.version, unit),
    )
    keepSaved(stored)
    return stored
  }

  function refused(error: unknown) {
    setRefusals(problemsFromServer(error, draft, { name: client.name }))
    focusOn(target.summary)
  }

  // both stop at the summary when the draft has problems, and start the summary afresh
  function beginAttempt(kind: 'save' | 'publish') {
    setAttempt(kind)
    setRefusals([])
    const found = validateDraft(
      draft,
      published || kind === 'publish' ? 'complete' : 'draft',
      unit,
    )
    if (found.length > 0) focusOn(target.summary)
    return found.length === 0
  }

  function onSave() {
    if (busy || !dirty || !beginAttempt('save')) return
    store().then(() => toast.success(t('plan.toast.saved')), refused)
  }

  function onPublish() {
    if (busy || !beginAttempt('publish')) return
    setConfirmingPublish(true)
  }

  async function publish() {
    setConfirmingPublish(false)
    // focus goes where the outcome sends it, not back to a button that is busy
    publishFocus.afterClosing(() => {})
    setPublishing(true)
    try {
      const base = dirty || !saved.plan ? await store() : saved.plan
      keepSaved(await publishPlan.mutateAsync(base.version))
      toast.success(t('plan.toast.published'))
      focusOn(target.save)
    } catch (error) {
      refused(error)
    } finally {
      setPublishing(false)
    }
  }

  const status = saved.plan?.status ?? 'DRAFT'
  const deleting = draft.sessions.find((session) => session.key === deletingKey)
  const weeksError = errorOf(target.weeks)
  // kept while the dialog fades out, so its title does not lose the session
  const [shownDeleting, setShownDeleting] = useState(deleting)
  if (deleting && deleting !== shownDeleting) setShownDeleting(deleting)

  return (
    // the margin keeps whatever takes focus clear of the sticky page header
    <div className="flex flex-col gap-6 pt-4 [&_*]:scroll-mt-16">
      <LeaveGuard dirty={dirty} body={t('leave.planBody')} />
      {/* a failed draft says so in the panel beside the editor */}
      {saved.plan?.automaticDraft === 'BUILT' && (
        <AutomaticDraftNote clientName={client.name} />
      )}
      {/* a disabled fieldset turns off every control inside, the way the platform does it */}
      <fieldset
        disabled={readOnly}
        className="flex min-w-0 flex-wrap items-start gap-x-4 gap-y-3"
      >
        <Field className="w-56 max-w-full">
          <FieldLabel htmlFor={`${id}-goal`}>
            {t('plan.fields.goal')}
          </FieldLabel>
          <Select
            value={draft.goal}
            onValueChange={(goal) =>
              change((current) => ({ ...current, goal: goal as Goal }))
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
        <Field className="w-36" data-invalid={!!weeksError}>
          <FieldLabel htmlFor={target.weeks}>
            {t('plan.fields.weeks')}
          </FieldLabel>
          <InputGroup>
            <InputGroupInput
              id={target.weeks}
              inputMode="numeric"
              autoComplete="off"
              className="num"
              aria-invalid={!!weeksError}
              aria-describedby={
                weeksError ? `${target.weeks}-error` : undefined
              }
              value={draft.weeks}
              onChange={(event) => {
                const weeks = event.target.value
                change((current) => ({ ...current, weeks }))
              }}
              onBlur={() => leaveField(target.weeks)}
            />
            <InputGroupAddon align="inline-end">
              <InputGroupText>{t('plan.fields.weeksUnit')}</InputGroupText>
            </InputGroupAddon>
          </InputGroup>
          {weeksError && (
            <FieldError id={`${target.weeks}-error`}>{weeksError}</FieldError>
          )}
        </Field>
        <div className="flex flex-wrap items-center gap-3 md:mt-[1.625rem] md:h-9">
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
        <div className="flex flex-wrap items-center gap-3 md:mt-[1.625rem] md:ml-auto">
          <span role="status" className="text-muted-foreground text-sm">
            {t(dirty ? 'plan.unsaved' : 'plan.nothingToSave')}
          </span>
          {/* never disabled for real: a disabled button drops focus, and focus lands here after publishing */}
          <Button
            id={target.save}
            variant={published ? 'default' : 'outline'}
            aria-disabled={!dirty || busy}
            aria-busy={savePlan.isPending && !publishing}
            className={cn((!dirty || busy) && 'cursor-not-allowed opacity-50')}
            onClick={onSave}
          >
            <BusyLabel
              busy={savePlan.isPending && !publishing}
              label={t('plan.save')}
              busyLabel={t('plan.saving')}
            />
          </Button>
          {!published && (
            <Button
              aria-disabled={busy}
              aria-busy={publishing}
              className={cn(busy && 'cursor-not-allowed opacity-50')}
              onClick={onPublish}
            >
              <BusyLabel
                busy={publishing}
                label={t('plan.publish')}
                busyLabel={t('plan.publishing')}
              />
            </Button>
          )}
        </div>
      </fieldset>

      {published && (
        <p className="text-muted-foreground text-sm">
          {t('plan.publishedNote')}
        </p>
      )}

      {shownProblems.length > 0 && (
        <ValidationSummary
          draft={draft}
          problems={shownProblems}
          title={
            attempt && problems.length > 0
              ? t(
                  attempt === 'publish'
                    ? 'plan.problems.publishTitle'
                    : 'plan.problems.saveTitle',
                )
              : undefined
          }
          sessionLabel={sessionLabel}
          onGoTo={onGoTo}
        />
      )}

      {draft.sessions.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-lg border border-dashed p-6">
          <p className="text-muted-foreground text-sm">
            {t('plan.sessions.empty')}
          </p>
          <Button
            id={target.addSession}
            variant="outline"
            disabled={readOnly}
            onClick={onAddSession}
          >
            <PlusIcon data-icon="inline-start" aria-hidden="true" />
            {t('plan.sessions.add')}
          </Button>
        </div>
      ) : (
        <Tabs value={selected?.key} onValueChange={setSelectedKey}>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <div ref={tabsRef} className="max-w-full">
              <TabsList
                variant="line"
                aria-label={t('plan.sessions.label')}
                className="h-auto! max-w-full flex-wrap justify-start"
              >
                {draft.sessions.map((session, index) => {
                  const hasProblem = shownProblems.some(
                    (problem) => problem.session === session.key,
                  )
                  return (
                    <TabsTrigger
                      key={session.key}
                      value={session.key}
                      className="max-w-full flex-none whitespace-normal"
                    >
                      <span className="num text-muted-foreground">
                        {letter(index)}
                      </span>
                      <span className="wrap-anywhere">
                        {session.name.trim() || t('plan.session.unnamed')}
                      </span>
                      <span className="text-muted-foreground font-normal">
                        · {dayName(session.day, i18n.language, 'short')}
                      </span>
                      {hasProblem && (
                        <>
                          <CircleAlertIcon
                            aria-hidden="true"
                            className="text-destructive"
                          />
                          <span className="sr-only">
                            {t('plan.sessions.hasProblem')}
                          </span>
                        </>
                      )}
                    </TabsTrigger>
                  )
                })}
              </TabsList>
            </div>
            <Button
              id={target.addSession}
              variant="outline"
              size="sm"
              disabled={readOnly || draft.sessions.length >= MAX_SESSIONS}
              onClick={onAddSession}
            >
              <PlusIcon data-icon="inline-start" aria-hidden="true" />
              {t('plan.sessions.add')}
            </Button>
            {draft.sessions.length >= MAX_SESSIONS && (
              <span className="text-muted-foreground text-sm">
                {t('plan.sessions.limit', { count: MAX_SESSIONS })}
              </span>
            )}
          </div>
          {selected && (
            <fieldset disabled={readOnly} className="contents">
              <SessionPanel
                key={selected.key}
                session={selected}
                sessions={draft.sessions}
                unit={unit}
                readOnly={readOnly}
                errorOf={errorOf}
                onLeaveField={leaveField}
                onChange={(values) => onChangeSession(selected.key, values)}
                onDelete={() => onDeleteSession(selected)}
              />
            </fieldset>
          )}
        </Tabs>
      )}

      <AlertDialog
        open={!!deleting}
        onOpenChange={(open) => {
          if (!open) setDeletingKey(undefined)
        }}
      >
        <AlertDialogContent {...deleteFocus.contentProps}>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('plan.session.deleteDialog.title', {
                session:
                  shownDeleting?.name.trim() || t('plan.session.unnamed'),
              })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t('plan.session.deleteDialog.body', {
                count: shownDeleting?.entries.length ?? 0,
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('plan.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              onClick={() => {
                if (deleting)
                  deleteFocus.afterClosing(deleteSession(deleting.key))
                setDeletingKey(undefined)
              }}
            >
              {t('plan.session.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={confirmingPublish} onOpenChange={setConfirmingPublish}>
        <AlertDialogContent {...publishFocus.contentProps}>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t('plan.publishDialog.title', { name: client.name })}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t('plan.publishDialog.body')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('plan.cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={() => void publish()}>
              {t('plan.publish')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

// the idle label stays in the layout, so the button keeps its width while busy
function BusyLabel({
  busy,
  label,
  busyLabel,
}: {
  busy: boolean
  label: ReactNode
  busyLabel: ReactNode
}) {
  if (!busy) return label
  return (
    <span className="grid">
      <span aria-hidden="true" className="invisible col-start-1 row-start-1">
        {label}
      </span>
      <span className="col-start-1 row-start-1 flex items-center justify-center gap-1.5">
        <Spinner aria-hidden="true" />
        {busyLabel}
      </span>
    </span>
  )
}
