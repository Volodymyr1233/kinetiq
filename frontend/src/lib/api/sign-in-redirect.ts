const AREAS = ['/trainer', '/app']

/** Sends a person whose sign-in ended to sign in, and back to this page afterwards. Public pages stay. */
export function redirectToSignIn() {
  const { pathname, search } = window.location
  if (
    !AREAS.some((area) => pathname === area || pathname.startsWith(`${area}/`))
  )
    return
  // a full load also drops every cache of the person who was signed in
  window.location.assign(
    `/sign-in?next=${encodeURIComponent(pathname + search)}`,
  )
}
