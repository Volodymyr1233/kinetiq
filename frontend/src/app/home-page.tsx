import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { THEMES, useTheme } from '@/lib/theme'

// Placeholder until the first feature lands. Exercises tokens, fonts, theme and i18n.
export function HomePage() {
  const { t, i18n } = useTranslation()
  const { theme, setTheme } = useTheme()

  return (
    <main className="mx-auto flex min-h-svh max-w-[640px] flex-col gap-8 px-4 py-12">
      <header className="flex items-center gap-2 text-lg font-semibold">
        <span className="bg-brand text-brand-foreground flex size-8 items-center justify-center rounded-md">
          K
        </span>
        Kinetiq
      </header>

      <section className="bg-card text-card-foreground flex flex-col gap-4 rounded-xl border p-6">
        <div>
          <p className="text-muted-foreground text-sm">
            {t('placeholder.metricLabel')}
          </p>
          <p className="num text-metric-lg font-medium">83</p>
        </div>
        <div>
          <h1 className="text-lg font-semibold">{t('placeholder.title')}</h1>
          <p className="text-muted-foreground text-sm">
            {t('placeholder.body')}
          </p>
        </div>
        <Button className="bg-brand text-brand-foreground hover:bg-brand/80 self-start">
          {t('placeholder.action')}
        </Button>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">{t('theme.label')}</h2>
        <div className="flex gap-2">
          {THEMES.map((option) => (
            <Button
              key={option}
              size="sm"
              variant={theme === option ? 'default' : 'outline'}
              aria-pressed={theme === option}
              onClick={() => setTheme(option)}
            >
              {t(`theme.${option}`)}
            </Button>
          ))}
        </div>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">{t('language.label')}</h2>
        <div className="flex gap-2">
          {(['en', 'pl'] as const).map((language) => (
            <Button
              key={language}
              size="sm"
              variant={i18n.language === language ? 'default' : 'outline'}
              aria-pressed={i18n.language === language}
              onClick={() => void i18n.changeLanguage(language)}
            >
              {t(`language.${language}`)}
            </Button>
          ))}
        </div>
      </section>
    </main>
  )
}
