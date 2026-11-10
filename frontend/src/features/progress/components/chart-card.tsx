import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

/**
 * A chart as the design system's section 3.7 asks: its finding as the title, what it shows in the subtitle, and the
 * same numbers as a table on request. `note` stays under the chart and under the table; `level` is the title's
 * heading level.
 */
export function ChartCard({
  title,
  subtitle,
  chart,
  table,
  note,
  level = 2,
}: {
  title: string
  subtitle: string
  chart: ReactNode
  table: ReactNode
  note?: ReactNode
  level?: 2 | 3
}) {
  const { t } = useTranslation()
  const [asTable, setAsTable] = useState(false)
  const Heading = level === 2 ? 'h2' : 'h3'

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <Heading>{title}</Heading>
        </CardTitle>
        <CardDescription>{subtitle}</CardDescription>
      </CardHeader>
      <CardContent>
        {asTable ? table : chart}
        {note}
        {/* the text says what a press does next, so the button has no pressed state that could contradict it */}
        <Button
          variant="outline"
          size="touch"
          className="self-start"
          onClick={() => setAsTable(!asTable)}
        >
          {asTable
            ? t('progress.chart.showChart')
            : t('progress.chart.showTable')}
        </Button>
      </CardContent>
    </Card>
  )
}
