import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useDraftSummary } from '../api/client-plan'
import { dayName } from '../plan-draft'

/**
 * What a waiting client may see of their draft. It shows nothing until there is a summary, so loading, no summary and
 * a failed read all leave the waiting state as it is.
 */
export function DraftCard() {
  const { t, i18n } = useTranslation()
  const { data: summary } = useDraftSummary()

  if (summary === undefined) return null

  const rows = [
    {
      label: t('plan.client.draft.trainingDays'),
      value: new Intl.ListFormat(i18n.language, {
        style: 'short',
        type: 'unit',
      }).format(
        summary.trainingDays.map((day) => dayName(day, i18n.language, 'short')),
      ),
    },
    {
      label: t('plan.client.draft.length'),
      value: t('plan.client.ready.weeks', { count: summary.weeks }),
    },
    {
      label: t('plan.client.draft.focus'),
      value: t(`quiz.goal.options.${summary.goal}.title`),
    },
  ]

  return (
    <Card className="mt-4">
      <CardHeader className="flex items-center justify-between gap-2">
        <CardTitle className="num text-muted-foreground text-xs font-normal tracking-wider uppercase">
          {t('plan.client.draft.label')}
        </CardTitle>
        <Badge variant="info">{t('plan.client.draft.waiting')}</Badge>
      </CardHeader>
      <CardContent>
        <dl className="divide-border divide-y">
          {rows.map(({ label, value }) => (
            <div key={label} className="flex justify-between gap-4 py-3">
              <dt>{label}</dt>
              <dd className="text-right font-semibold">{value}</dd>
            </div>
          ))}
        </dl>
        <p className="text-muted-foreground">{t('plan.client.draft.note')}</p>
      </CardContent>
    </Card>
  )
}
