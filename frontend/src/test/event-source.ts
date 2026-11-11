/** Stands in for the browser's `EventSource`, which jsdom lacks; a test opens, fires and fails streams by hand. */
export class FakeEventSource extends EventTarget {
  static readonly CONNECTING = 0
  static readonly OPEN = 1
  static readonly CLOSED = 2
  static instances: FakeEventSource[] = []

  readonly url: string
  readyState = FakeEventSource.CONNECTING

  constructor(url: string) {
    super()
    this.url = url
    FakeEventSource.instances.push(this)
  }

  /** The streams not closed by the app. */
  static open() {
    return FakeEventSource.instances.filter(
      (source) => source.readyState !== FakeEventSource.CLOSED,
    )
  }

  close() {
    this.readyState = FakeEventSource.CLOSED
  }

  connect() {
    this.readyState = FakeEventSource.OPEN
    this.dispatchEvent(new Event('open'))
  }

  emit(name: string, data: string) {
    this.dispatchEvent(new MessageEvent(name, { data }))
  }

  /** The browser gave up, as on a non-200 answer. */
  giveUp() {
    this.readyState = FakeEventSource.CLOSED
    this.dispatchEvent(new Event('error'))
  }
}
