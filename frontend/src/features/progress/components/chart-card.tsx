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
 * same numbers as a table on request. `note` stays under the chart and under the table.
 */
export function ChartCard({
  title,
  subtitle,
  chart,
  table,
  note,
}: {
  title: string
  subtitle: string
  chart: ReactNode
  table: ReactNode
  note?: ReactNode
}) {
  const { t } = useTranslation()
  const [asTable, setAsTable] = useState(false)

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2>{title}</h2>
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
