/** The first word of a person's name: "Marek Nowak" talks as "Marek", as on the boards. */
export function firstName(name: string) {
  return name.trim().split(/\s+/)[0] ?? name
}
