# The backend serves the SPA in production

Production runs as one Docker image on a managed platform (Railway): the frontend is built into the backend's static resources and Spring serves it, with unknown paths falling back to `index.html`. This keeps the SPA and `/api` on one origin, so the session cookie and CSRF work without CORS, and it needs no reverse proxy service. The cost is that frontend and backend deploy together. A separate static host or a proxy in front becomes worth it only if they must be released independently.
