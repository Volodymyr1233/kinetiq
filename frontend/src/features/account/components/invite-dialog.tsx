import { zodResolver } from '@hookform/resolvers/zod'
import { CopyIcon } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { errorMessageKey } from '@/lib/api/problem'
import { useCreateInvite } from '../api/invites'
import { copyText } from '../copy-text'
import { emailSchema } from '../sign-up-schema'

const inviteSchema = z.object({ email: emailSchema })

type InviteValues = z.input<typeof inviteSchema>

const ACCOUNT_EXISTS = 'errors.invite.account-exists'

function codeOf(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error
    ? error.code
    : undefined
}

/** Invites a client by email, then shows the link to copy. `children` is the button that opens it. */
export function InviteDialog({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  // a fresh form every time the dialog opens
  const [session, setSession] = useState(0)

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        if (next) setSession((count) => count + 1)
      }}
    >
      <DialogTrigger asChild>{children}</DialogTrigger>
      {open && <InviteDialogContent key={session} />}
    </Dialog>
  )
}

function InviteDialogContent() {
  const { t } = useTranslation()
  const createInvite = useCreateInvite()
  const [sent, setSent] = useState<{ email: string; link: string }>()
  const [failure, setFailure] = useState<string>()
  const form = useForm<InviteValues, unknown, z.output<typeof inviteSchema>>({
    resolver: zodResolver(inviteSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    shouldFocusError: true,
    defaultValues: { email: '' },
  })
  const { errors, isDirty } = form.formState
  const busy = createInvite.isPending

  const onSubmit = form.handleSubmit((values) => {
    if (busy) return
    setFailure(undefined)
    createInvite.mutate(values.email, {
      onSuccess: (created) =>
        setSent({ email: created.invite.email, link: created.link }),
      onError: (error) => {
        const code = codeOf(error)
        if (code === 'invite.account-exists' || code === 'validation.failed') {
          form.setError(
            'email',
            {
              type: 'server',
              message:
                code === 'validation.failed'
                  ? 'account.errors.email'
                  : ACCOUNT_EXISTS,
            },
            { shouldFocus: true },
          )
          return
        }
        setFailure(errorMessageKey(error))
      },
    })
  })

  return (
    <DialogContent
      // a half-typed email is not lost to a stray click; Escape and Cancel still close
      onInteractOutside={(event) => {
        if (isDirty && !sent) event.preventDefault()
      }}
    >
      {sent ? (
        <LinkReady
          link={sent.link}
          title={t('invites.dialog.sentTitle', { email: sent.email })}
          description={t('invites.dialog.sentDescription')}
        />
      ) : (
        <form noValidate onSubmit={onSubmit} className="flex flex-col gap-6">
          <DialogHeader>
            <DialogTitle>{t('invites.dialog.title')}</DialogTitle>
            <DialogDescription>
              {t('invites.dialog.description')}
            </DialogDescription>
          </DialogHeader>
          <FieldGroup>
            <Field data-invalid={!!errors.email}>
              <FieldLabel htmlFor="invite-email">
                {t('invites.dialog.email')}
              </FieldLabel>
              <Input
                id="invite-email"
                type="email"
                autoComplete="off"
                placeholder={t('invites.dialog.emailPlaceholder')}
                readOnly={busy}
                aria-invalid={!!errors.email}
                aria-describedby={
                  errors.email ? 'invite-email-error' : undefined
                }
                {...form.register('email', {
                  // the server's verdict no longer applies once the email changes
                  onChange: () => {
                    if (errors.email?.type === 'server') {
                      form.clearErrors('email')
                    }
                  },
                })}
              />
              {errors.email?.message && (
                <FieldError id="invite-email-error">
                  {t(errors.email.message)}
                </FieldError>
              )}
            </Field>
          </FieldGroup>
          {failure && (
            <Alert variant="destructive">
              <AlertDescription>{t(failure)}</AlertDescription>
            </Alert>
          )}
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline">
                {t('invites.dialog.cancel')}
              </Button>
            </DialogClose>
            <Button type="submit" disabled={busy} aria-busy={busy}>
              {busy && <Spinner data-icon="inline-start" aria-hidden="true" />}
              {t('invites.dialog.send')}
            </Button>
          </DialogFooter>
        </form>
      )}
    </DialogContent>
  )
}

/** Shows a new link that could not be copied, so it is not lost: the one copied before already stopped working. */
export function NewLinkDialog({
  email,
  link,
  onClose,
}: {
  email: string
  link: string
  onClose: () => void
}) {
  const { t } = useTranslation()
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <LinkReady
          link={link}
          title={t('invites.newLinkDialog.title', { email })}
          description={t('invites.newLinkDialog.description')}
        />
      </DialogContent>
    </Dialog>
  )
}

function LinkReady({
  link,
  title,
  description,
}: {
  link: string
  title: string
  description: string
}) {
  const { t } = useTranslation()
  const heading = useRef<HTMLHeadingElement>(null)
  const linkInput = useRef<HTMLInputElement>(null)
  const [copy, setCopy] = useState<'copied' | 'failed'>()

  // the form that had focus is gone; the new state starts at its heading
  useEffect(() => heading.current?.focus(), [])

  function copyLink() {
    copyText(Promise.resolve(link)).then(
      () => setCopy('copied'),
      () => {
        setCopy('failed')
        // selected, so copying by hand takes one shortcut
        linkInput.current?.focus()
      },
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <DialogHeader>
        <DialogTitle ref={heading} tabIndex={-1} className="outline-none">
          {title}
        </DialogTitle>
        <DialogDescription>{description}</DialogDescription>
      </DialogHeader>
      <Field>
        <FieldLabel htmlFor="invite-link">
          {t('invites.dialog.link')}
        </FieldLabel>
        <div className="flex gap-2">
          <Input
            ref={linkInput}
            id="invite-link"
            readOnly
            value={link}
            className="num flex-1 text-xs"
            aria-describedby="invite-link-warning"
            onFocus={(event) => event.currentTarget.select()}
          />
          <Button type="button" variant="outline" onClick={copyLink}>
            <CopyIcon data-icon="inline-start" aria-hidden="true" />
            {copy === 'copied'
              ? t('invites.copied')
              : t('invites.dialog.copyLink')}
          </Button>
        </div>
        <FieldDescription id="invite-link-warning">
          {t('invites.dialog.linkWarning')}
        </FieldDescription>
      </Field>
      <p role="status" className="sr-only">
        {copy === 'copied' && t('invites.dialog.linkCopied')}
      </p>
      {copy === 'failed' && (
        <Alert variant="destructive">
          <AlertDescription>{t('invites.copyFailed')}</AlertDescription>
        </Alert>
      )}
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button">{t('invites.dialog.done')}</Button>
        </DialogClose>
      </DialogFooter>
    </div>
  )
}
