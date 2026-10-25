import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from '@/locales/en.json'
import pl from '@/locales/pl.json'

export const LANGUAGE_STORAGE_KEY = 'kinetiq-language'
const LANGUAGES = ['en', 'pl']

// storage can be unavailable (private mode); then the browser's language is used every time
function rememberedLanguage() {
  try {
    const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY)
    return stored !== null && LANGUAGES.includes(stored) ? stored : undefined
  } catch {
    return undefined
  }
}

/** The language the app starts in: the one last used on this device, else the browser's, English unless it asks for Polish. */
export function startLanguage() {
  return (
    rememberedLanguage() ??
    (navigator.language.toLowerCase().startsWith('pl') ? 'pl' : 'en')
  )
}

i18n.on('languageChanged', (language) => {
  // screen readers pick their voice from <html lang>
  document.documentElement.lang = language
  // the start-up language is only the device's guess; what the person chose is kept for the pages before sign-in
  if (!i18n.isInitialized) return
  try {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language)
  } catch {
    // nothing to keep it in; the account's language applies again after sign-in
  }
})

void i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, pl: { translation: pl } },
  lng: startLanguage(),
  fallbackLng: 'en',
  interpolation: { escapeValue: false }, // React already escapes
})

export default i18n
