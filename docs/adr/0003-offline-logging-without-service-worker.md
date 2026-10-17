# Offline logging without a service worker

Set logs made without a connection are queued as paused TanStack Query mutations, persisted to localStorage and replayed when the connection returns; each carries a client-generated id, so a replay never stores a set twice. There is no service worker or PWA, so the app must already be open when the connection drops; opening it offline is not supported. localStorage holds the whole queue easily (a session is a few kilobytes), so IndexedDB would only add an asynchronous persister. A service worker comes with a PWA, if one is ever wanted.
