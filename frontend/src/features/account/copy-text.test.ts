import { afterEach, describe, expect, it, vi } from 'vitest'
import { copyText } from './copy-text'

function stubClipboard(clipboard: Partial<Clipboard>) {
  vi.stubGlobal('navigator', { clipboard })
}

describe('copyText', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('writes the text once it arrives through a clipboard item', async () => {
    let written: Blob | undefined
    vi.stubGlobal(
      'ClipboardItem',
      class {
        items: Record<string, Promise<Blob>>
        constructor(items: Record<string, Promise<Blob>>) {
          this.items = items
        }
      },
    )
    stubClipboard({
      write: async ([item]) => {
        written = await (
          item as unknown as { items: Record<string, Promise<Blob>> }
        ).items['text/plain']
      },
    })

    await copyText(Promise.resolve('https://kinetiq.app/join#token=a'))

    expect(await written?.text()).toBe('https://kinetiq.app/join#token=a')
  })

  it('writes plain text where clipboard items are missing', async () => {
    vi.stubGlobal('ClipboardItem', undefined)
    const writeText = vi.fn(async () => {})
    stubClipboard({ writeText })

    await copyText(Promise.resolve('link'))

    expect(writeText).toHaveBeenCalledWith('link')
  })

  it('fails when the clipboard refuses', async () => {
    vi.stubGlobal('ClipboardItem', undefined)
    stubClipboard({
      writeText: async () => {
        throw new DOMException('denied', 'NotAllowedError')
      },
    })

    await expect(copyText(Promise.resolve('link'))).rejects.toThrow('denied')
  })
})
