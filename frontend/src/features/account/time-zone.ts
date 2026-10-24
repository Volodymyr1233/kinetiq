/** The browser's region time zone, or nothing when it reports none or a placeholder such as `Etc/Unknown`. */
export function browserTimeZone() {
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  return !zone || zone === 'Etc/Unknown' ? undefined : zone
}
