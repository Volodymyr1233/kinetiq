import { CheckIcon, CircleAlertIcon, LockIcon } from 'lucide-react'
import { useId, useState, type ReactNode } from 'react'
import { Trans, useTranslation } from 'react-i18next'
import { Link, Navigate } from 'react-router'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardTitle,
} from '@/components/ui/card'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { fromKg, useWeightUnit } from '@/lib/weight'
import {
  useSaveNote,
  useSessionSummary,
  type SessionSummary,
} from '../api/session-summary'

const NOTE_MAX = 500

/**
 * The session done screen at its own address: the figures of a finished session and the note for the trainer.
 * `trainerName` is the trainer's first name, missing until it is read and when it cannot be.
 */
export function SummaryPage({
  sessionId,
  trainerName,
  notFound,
}: {
  sessionId: string
  trainerName?: string
  // the client area's page for something that is not theirs or does not exist
  notFound: ReactNode
}) {
  const { t } = useTranslation()
  const query = useSessionSummary(sessionId)
  const summary = query.data

  if (summary === undefined) {
    if (query.isPending) {
      return (
        <main className="mx-auto flex max-w-[640px] flex-col gap-4 px-4 py-6">
          <p role="status" className="sr-only">
            {t('sessionlog.summary.loading')}
          </p>
          <div aria-hidden className="flex flex-col gap-4">
            <Skeleton className="h-5 w-40" />
            <Skeleton className="h-9 w-64" />
            <Skeleton className="h-5 w-48" />
            <div className="grid grid-cols-2 gap-3">
              {Array.from({ length: 4 }, (_, i) => (
                <Skeleton key={i} className="h-24" />
              ))}
            </div>
            <Skeleton className="h-28 w-full" />
          </div>
        </main>
      )
    }
    const code = problemCode(query.error)
    if (code === 'session-log.not-found') return notFound
    // not finished: there is nothing to summarise yet, so the client goes on logging
    if (code === 'session-log.not-finished') {
      return <Navigate replace to="/app/session" />
    }
    return (
      <main className="mx-auto flex min-h-svh max-w-[640px] flex-col justify-center gap-4 px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('sessionlog.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </main>
    )
  }

  return <Summary summary={summary} trainerName={trainerName} />
}

// the backend's rule (session-log.locked); the screen follows it, and the server still decides
const EDIT_WINDOW_MS = 24 * 60 * 60 * 1000

function Summary({
  summary,
  trainerName,
}: {
  summary: SessionSummary
  trainerName?: string
}) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const number = (value: number, minDigits: number, maxDigits = minDigits) =>
    new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: minDigits,
      maximumFractionDigits: maxDigits,
    }).format(value)

  // a plain date, read in UTC so no time zone can move the day
  const [year, month, day] = summary.date.split('-').map(Number)
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  }).format(new Date(Date.UTC(year, month - 1, day)))

  const trainer = trainerName ?? t('sessionlog.summary.yourTrainer')

  const prSet = (pr: SessionSummary['prs'][number]) =>
    pr.loadKg !== undefined
      ? t('sessionlog.summary.prLoad', {
          load: number(fromKg(pr.loadKg, unit), 0, 1),
          unit: t(`plan.units.${unit}`),
          amount: pr.amount,
        })
      : t(
          pr.measure === 'SECONDS'
            ? 'sessionlog.summary.prSeconds'
            : 'sessionlog.summary.prReps',
          { count: pr.amount },
        )

  return (
    <main className="mx-auto flex max-w-[640px] flex-col gap-5 px-4 pt-6 pb-8">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant="success">
          <CheckIcon aria-hidden />
          {t('sessionlog.summary.done')}
        </Badge>
        <span className="text-muted-foreground text-sm">{date}</span>
      </div>
      <h1 className="font-heading text-3xl font-semibold break-words">
        {t('sessionlog.summary.title', { name: summary.name })}
      </h1>
      {(summary.prs.length > 0 || summary.weeksInARow > 0) && (
        <div className="flex flex-wrap gap-2">
          {summary.prs.map((pr, index) => (
            <Badge
              key={pr.exerciseId}
              // lime marks one element per view, so further PRs are outlined
              variant={index === 0 ? 'brand' : 'outline'}
              className="h-auto min-h-5 text-left wrap-anywhere whitespace-normal"
            >
              <span>
                <Trans
                  i18nKey="sessionlog.summary.pr"
                  values={{ exercise: pr.name, set: prSet(pr) }}
                  components={{ num: <span className="num" /> }}
                />
              </span>
            </Badge>
          ))}
          {summary.weeksInARow > 0 && (
            <Badge variant="outline">
              <span>
                <Trans
                  i18nKey="sessionlog.summary.weeksInARow"
                  count={summary.weeksInARow}
                  components={{ num: <span className="num" /> }}
                />
              </span>
            </Badge>
          )}
        </div>
      )}
      <section
        aria-label={t('sessionlog.summary.figures')}
        className="grid grid-cols-2 gap-3"
      >
        <Metric label={t('sessionlog.summary.sets')}>
          {t('sessionlog.summary.setsValue', {
            logged: summary.setsLogged,
            planned: summary.setsPlanned,
          })}
        </Metric>
        <Metric label={t('sessionlog.summary.volume')}>
          {summary.volumeKg === undefined
            ? '—'
            : t('sessionlog.summary.volumeValue', {
                volume: number(fromKg(summary.volumeKg, unit), 0, 1),
                unit: t(`plan.units.${unit}`),
              })}
        </Metric>
        <Metric label={t('sessionlog.summary.rpe')}>
          {number(summary.averageRpe, 1)}
        </Metric>
        <Metric label={t('sessionlog.summary.time')}>
          {t('sessionlog.summary.minutes', { count: summary.minutes })}
        </Metric>
      </section>
      <NoteField summary={summary} trainer={trainer} />
      <div className="flex flex-col gap-2">
        <Button size="touch" asChild>
          <Link to="/app/today">{t('sessionlog.summary.backToToday')}</Link>
        </Button>
        <Button variant="outline" size="touch" asChild>
          <Link to="/app/messages">
            {t('sessionlog.summary.message', { name: trainer })}
          </Link>
        </Button>
      </div>
    </main>
  )
}

function Metric({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Card size="sm" variant="metric">
      <CardContent className="gap-1">
        <CardDescription>{label}</CardDescription>
        <CardTitle className="num">{children}</CardTitle>
      </CardContent>
    </Card>
  )
}

function NoteField({
  summary,
  trainer,
}: {
  summary: SessionSummary
  trainer: string
}) {
  const { t } = useTranslation()
  const save = useSaveNote(summary.sessionLogId)
  const id = useId()
  const saved = summary.note ?? ''
  const [text, setText] = useState(saved)
  // locked on arrival, or refused by the server while the page is open
  const [locked, setLocked] = useState(
    () => Date.now() >= Date.parse(summary.startedAt) + EDIT_WINDOW_MS,
  )
  const [confirmed, setConfirmed] = useState(false)

  const lockedNow = locked
  const tooLong = text.trim().length > NOTE_MAX
  const unchanged = text.trim() === saved
  const error = save.error
  const message =
    error !== null && problemCode(error) !== 'session-log.locked'
      ? t(errorMessageKey(error))
      : tooLong
        ? t('errors.session-log.note-invalid')
        : undefined

  return (
    <form
      className="flex flex-col gap-3"
      onSubmit={(event) => {
        event.preventDefault()
        if (tooLong || unchanged) return
        setConfirmed(false)
        save.mutate(text, {
          onSuccess: () => setConfirmed(true),
          onError: (e) => {
            if (problemCode(e) === 'session-log.locked') setLocked(true)
          },
        })
      }}
    >
      <Field data-invalid={message !== undefined || undefined}>
        <FieldLabel htmlFor={id}>
          {t('sessionlog.summary.noteLabel', { name: trainer })}
        </FieldLabel>
        <Textarea
          id={id}
          value={text}
          readOnly={lockedNow}
          aria-invalid={message !== undefined || undefined}
          aria-describedby={`${id}-help ${id}-count`}
          placeholder={t('sessionlog.summary.notePlaceholder')}
          onChange={(event) => {
            setText(event.target.value)
            setConfirmed(false)
            save.reset()
          }}
        />
        <FieldDescription id={`${id}-help`}>
          {t('sessionlog.summary.noteHelp', { name: trainer })}
        </FieldDescription>
        <p
          id={`${id}-count`}
          className="num text-muted-foreground text-end text-xs"
        >
          {t('sessionlog.summary.noteCount', {
            length: text.length,
            max: NOTE_MAX,
          })}
        </p>
        {message !== undefined && <FieldError>{message}</FieldError>}
      </Field>
      {lockedNow ? (
        <Alert variant="info" role={undefined}>
          <LockIcon aria-hidden />
          <AlertDescription>
            {t('sessionlog.summary.noteLocked')}
          </AlertDescription>
        </Alert>
      ) : (
        <Button
          type="submit"
          variant="outline"
          size="touch"
          disabled={unchanged || tooLong || save.isPending}
        >
          {t('sessionlog.summary.saveNote')}
        </Button>
      )}
      <p role="status" className="text-muted-foreground text-sm">
        {confirmed ? t('sessionlog.summary.noteSaved') : ''}
      </p>
    </form>
  )
}
