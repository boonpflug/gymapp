import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import en from './en.json'
import de from './de.json'
import fr from './fr.json'
import enPortal from './en_portal.json'
import dePortal from './de_portal.json'
import frPortal from './fr_portal.json'
import enExtra from './en_extra.json'
import deExtra from './de_extra.json'
import frExtra from './fr_extra.json'

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: { ...en, ...enPortal, ...enExtra } },
      de: { translation: { ...de, ...dePortal, ...deExtra } },
      fr: { translation: { ...fr, ...frPortal, ...frExtra } },
    },
    lng: 'de',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  })

export default i18n
