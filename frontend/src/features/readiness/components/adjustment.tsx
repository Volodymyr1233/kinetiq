import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useCauseList } from '@/lib/readiness'
import {
  useChooseTargets,
  type Adjustment,
  type TargetChoice,
} from '../api/check-in'

// the open Undo message, closed before another opens; outside the component, so it outlives a tab change. A fixed
// id would not do: sonner revives a dismissed message of the same id already closing
let restored: string | number | undefined
// without an id sonner closes every message
const closeRestored = () => restored !== undefined && toast.dismiss(restored)

/**
 * The row above today's session on a recovery day: a mark naming the targets in use and, while they can still be
 * switched, one button to switch to the other targets. Using the original targets offers Undo in a message; the button
 * stays, so nothing depends on catching the message in time.
 */
export function AdjustmentMark({
  adjustment,
  open,
}: {
  adjustment: Adjustment
  /** Whether the targets can still be switched: no set of today's session is logged. */
  open: boolean
}) {
  const { t } = useTranslation()
  const choose = useChooseTargets()
  const button = useRef<HTMLButtonElement>(null)
  // announced only after a switch the client made, not when Today opens
  const [switched, setSwitched] = useState(false)

  const send = (choice: TargetChoice) => {
    setSwitched(true)
    choose.mutate(choice, {
      onError: (error) => {
        // a set was logged meanwhile: the readiness card then says the targets are fixed
        if (problemCode(error) !== 'check-in.training-started') {
          toast.error(t(errorMessageKey(error)), { position: 'bottom-center' })
        }
      },
    })
  }

  const restoreOriginal = () => {
    send('ORIGINAL')
    closeRestored()
    restored = toast(t('readiness.adjustment.restored'), {
      position: 'bottom-center',
      // a second longer than the default, to reach Undo with one hand
      duration: 5000,
      action: {
        label: t('readiness.adjustment.undo'),
        onClick: () => {
          send('LIGHTER')
          // the message closes with the tap, so focus returns to the switch it undid
          button.current?.focus()
        },
      },
    })
  }

  return (
    <>
      {adjustment.inUse ? (
        <Badge variant="adjusted">{t('readiness.adjustment.badge')}</Badge>
      ) : (
        <Badge variant="secondary">{t('readiness.adjustment.original')}</Badge>
      )}
      {open && (
        // one button whose action follows the targets in use, so focus stays on it after a switch
        <Button
          ref={button}
          variant="link"
          size="touch"
          className="ml-auto px-0"
          onClick={() => {
            if (adjustment.inUse) return restoreOriginal()
            // the lighter targets are back, so the message has nothing left to undo
            closeRestored()
            send('LIGHTER')
          }}
        >
          {adjustment.inUse
            ? t('readiness.adjustment.useOriginal')
            : t('readiness.adjustment.useLighter')}
        </Button>
      )}
      <p role="status" className="sr-only">
        {switched &&
          (adjustment.inUse
            ? t('readiness.adjustment.announceLighter')
            : t('readiness.adjustment.announceOriginal'))}
      </p>
    </>
  )
}

/** One sentence naming the answers that made today's targets lighter, in the order the answers are asked. */
export function AdjustmentReason({ causes }: { causes: Adjustment['causes'] }) {
  const { t } = useTranslation()
  const causeList = useCauseList()

  return (
    <p>
      {t('readiness.adjustment.reason', {
        causes: causeList(causes, { opening: true }),
      })}
    </p>
  )
}

/** Says that only today's targets change, under the session's action. */
export function AdjustmentNote() {
  const { t } = useTranslation()

  return (
    <p className="text-muted-foreground text-center text-sm">
      {t('readiness.adjustment.planUnchanged')}
    </p>
  )
}
