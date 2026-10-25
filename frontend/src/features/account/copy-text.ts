/**
 * Copies text that may still be on its way. Safari allows writing only inside the click, so a `ClipboardItem`
 * holding the pending text is written at once; other browsers may get it when it arrives. Rejects when copying fails.
 */
export async function copyText(text: Promise<string>) {
  if (typeof ClipboardItem !== 'undefined' && navigator.clipboard.write) {
    const blob = text.then((value) => new Blob([value], { type: 'text/plain' }))
    await navigator.clipboard.write([new ClipboardItem({ 'text/plain': blob })])
    return
  }
  await navigator.clipboard.writeText(await text)
}
