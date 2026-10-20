import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from '@/locales/en.json'
import pl from '@/locales/pl.json'

// screen readers pick their voice from <html lang>
i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language
})

void i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, pl: { translation: pl } },
  // no language switcher yet: follow the browser, English unless it asks for Polish
  lng: navigator.language.toLowerCase().startsWith('pl') ? 'pl' : 'en',
  fallbackLng: 'en',
  interpolation: { escapeValue: false }, // React already escapes
})

export default i18n
