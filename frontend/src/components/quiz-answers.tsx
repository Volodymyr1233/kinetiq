import { useTranslation } from 'react-i18next'
import { Separator } from '@/components/ui/separator'
import type { components } from '@/lib/api/schema'
import { fromKg, useWeightUnit } from '@/lib/weight'

type QuizAnswers = components['schemas']['QuizAnswersResponse']

/** What a client answered in the quiz, as the trainer reads it beside a draft and on the client's overview. */
export function QuizAnswersList({
  answers,
  name,
}: {
  answers: QuizAnswers
  name: string
}) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const rows = [
    ['goal', t(`quiz.goal.options.${answers.goal}.title`)],
    ['experience', t(`quiz.experience.options.${answers.experience}.title`)],
    ['level', t(`quiz.level.options.${answers.level}.title`)],
    ['age', t('plan.panel.ageValue', { count: answers.age })],
    ['height', t('plan.panel.heightValue', { height: answers.heightCm })],
    [
      'weight',
      t('plan.panel.weightValue', {
        weight: fromKg(answers.weightKg, unit),
        unit: t(`plan.units.${unit}`),
      }),
    ],
    [
      'likes',
      new Intl.ListFormat(i18n.language).format(
        answers.likes.map((like) => t(`quiz.likes.options.${like}.title`)),
      ),
    ],
    ['days', t('plan.panel.daysValue', { count: answers.daysPerWeek })],
  ] as const
  return (
    <>
      <dl className="flex flex-col gap-2 text-sm">
        {rows.map(([key, value]) => (
          <div key={key} className="flex justify-between gap-4">
            <dt className="text-muted-foreground shrink-0">
              {t(`plan.panel.${key}`)}
            </dt>
            <dd className="text-right font-medium">{value}</dd>
          </div>
        ))}
      </dl>
      <Separator />
      <div className="flex flex-col gap-1 text-sm">
        <p className="text-muted-foreground">{t('plan.panel.notes')}</p>
        {/* as the client typed it, never as markup */}
        <p className="wrap-anywhere whitespace-pre-wrap">
          {answers.notes ?? t('plan.panel.noNotes', { name })}
        </p>
      </div>
    </>
  )
}
