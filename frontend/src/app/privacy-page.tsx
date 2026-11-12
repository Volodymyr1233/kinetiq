import { Trans, useTranslation } from 'react-i18next'
import { Link } from 'react-router'

// change it whenever the notice's text changes
const UPDATED_ON = new Date(2026, 10, 12)
const CONTACT = 'privacy@kinetiq.page'

type Section = {
  heading: string
  paragraphs?: string[]
  items?: string[]
  after?: string[]
}

/** The privacy notice; public, because it is read before an account exists. */
export function PrivacyPage() {
  const { t, i18n } = useTranslation()
  const sections = t('privacy.sections', { returnObjects: true }) as Section[]
  const updated = new Intl.DateTimeFormat(i18n.language, {
    dateStyle: 'long',
  }).format(UPDATED_ON)

  // the keys, not the text, so every paragraph goes through Trans with its mail link
  const paragraphs = (index: number, key: 'paragraphs' | 'after') =>
    sections[index][key]?.map((_, line) => (
      <p key={`${key}-${line}`}>
        <Trans
          i18nKey={`privacy.sections.${index}.${key}.${line}`}
          components={{
            mail: (
              <a
                href={`mailto:${CONTACT}`}
                className="underline underline-offset-3"
              />
            ),
          }}
        />
      </p>
    ))

  return (
    <div className="bg-background min-h-svh px-4 pt-8 pb-12">
      <div className="mx-auto flex max-w-prose flex-col gap-8">
        <Link
          to="/"
          className="flex items-center gap-2.5 self-start rounded-md text-base font-semibold tracking-tight"
        >
          <span
            aria-hidden="true"
            className="bg-brand text-brand-foreground grid size-7 place-items-center rounded-lg font-mono text-sm"
          >
            K
          </span>
          Kinetiq
          <span className="sr-only">{t('privacy.home')}</span>
        </Link>
        <main className="flex flex-col gap-8 wrap-anywhere">
          <header className="flex flex-col gap-1">
            <h1 className="text-2xl leading-8 font-semibold tracking-tight">
              {t('privacy.title')}
            </h1>
            <p className="text-muted-foreground text-sm">
              {t('privacy.updated', { date: updated })}
            </p>
          </header>
          {sections.map((section, index) => (
            <section
              key={section.heading}
              aria-labelledby={`privacy-${index}`}
              className="flex flex-col gap-3 text-sm leading-6"
            >
              <h2 id={`privacy-${index}`} className="text-base font-semibold">
                {section.heading}
              </h2>
              {paragraphs(index, 'paragraphs')}
              {section.items && (
                <ul className="flex list-disc flex-col gap-1.5 pl-5">
                  {section.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              )}
              {paragraphs(index, 'after')}
            </section>
          ))}
        </main>
      </div>
    </div>
  )
}
