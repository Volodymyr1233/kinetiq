import { afterEach, describe, expect, it, vi } from 'vitest'
import { redirectToSignIn } from './sign-in-redirect'

function openAt(path: string) {
  const assign = vi.fn()
  const url = new URL(path, 'http://app.test')
  vi.stubGlobal('location', {
    pathname: url.pathname,
    search: url.search,
    assign,
  })
  return assign
}

describe('redirectToSignIn', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps the area page and its query to return to', () => {
    const assign = openAt('/trainer/exercises?from=mail')

    redirectToSignIn()

    expect(assign).toHaveBeenCalledWith(
      '/sign-in?next=%2Ftrainer%2Fexercises%3Ffrom%3Dmail',
    )
  })

  it.each(['/sign-in', '/sign-up', '/', '/application'])(
    'stays on the public page %s',
    (path) => {
      const assign = openAt(path)

      redirectToSignIn()

      expect(assign).not.toHaveBeenCalled()
    },
  )
})
