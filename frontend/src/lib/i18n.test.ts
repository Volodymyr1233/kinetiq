import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n, { LANGUAGE_STORAGE_KEY, startLanguage } from './i18n'

function browserAsks(language: string) {
  vi.spyOn(navigator, 'language', 'get').mockReturnValue(language)
}

describe('startLanguage', () => {
  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('starts with the language last used on the device', () => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'pl')
    browserAsks('en-US')

    expect(startLanguage()).toBe('pl')
  })

  it('ignores a stored language the app does not have', () => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'de')
    browserAsks('en-US')

    expect(startLanguage()).toBe('en')
  })

  it.each([
    ['pl-PL', 'pl'],
    ['en-US', 'en'],
    ['de-DE', 'en'],
  ])('follows a %s browser when nothing was chosen', (browser, language) => {
    browserAsks(browser)

    expect(startLanguage()).toBe(language)
  })
})

describe('changing the language', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
    localStorage.clear()
  })

  it('remembers it on the device', async () => {
    await i18n.changeLanguage('pl')

    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('pl')
  })
})
