---

description: "Task list for the public home page"
---

# Tasks: Visitor sees what Kinetiq does and signs up

**Input**: Design documents from `specs/313-home-page/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/home-route.md

**Tests**: Requested by task #314 ("Tests cover the redirect for each role and the visitor's links").

**Commit**: All tasks belong to GitHub task #314 and land as one commit, `feat(frontend): add the public home page (#314)`.

## Phase 1: Setup

- [X] T001 Add display text tokens `--text-display-sm` (2.5rem, line height 2.75rem) and `--text-display` (3.25rem, line height 3.5rem) to the type scale in `frontend/src/index.css` (research R5)
- [X] T002 [P] Add variant `brand` (`bg-brand text-brand-foreground hover:bg-brand/80`) to `frontend/src/components/ui/button.tsx` (research R3)
- [X] T003 Add a `reveal` utility to `frontend/src/index.css`: inside `@supports (animation-timeline: view())`, a fade-up keyframe (opacity 0 → 1, translateY 12px → 0) on `animation-timeline: view()` over `entry 0% entry 40%`, fill both; with `prefers-reduced-motion: reduce` a fade only (research R4)

## Phase 2: Foundational

- [X] T004 Replace `placeholder.*` with `home.*` strings in `frontend/src/locales/en.json` and `frontend/src/locales/pl.json`: header, hero, mock-up labels and example texts, how it works, two sides, plan changes, closing, facts, footer (FR-010, FR-011)

## Phase 3: User Story 1 — Visitor learns what Kinetiq does and signs up (P1) 🎯 MVP

**Goal**: A visitor at `/` sees the whole page and reaches sign-up, sign-in and the privacy notice.

**Independent Test**: Signed out, open `/`: one `h1`, "Create trainer account" twice to `/sign-up`, "Sign in" to `/sign-in`, privacy links to `/privacy`.

- [X] T005 [US1] Rewrite `frontend/src/app/home-page.tsx` as the Home boards: header (brand mark, ghost `Button asChild` + `Link` "Sign in"), hero (label, `h1`, lead, `Button variant="brand" size="touch" asChild` to `/sign-up`, note) beside the desk and phone mock-up, how it works (`ol` of 3 `Card`s), At your desk / On their phone (lists with mini mock-ups), Plan changes (split with a plan change `Card`), closing (split with invite mock-up, then the 3 facts and a privacy link), footer (brand mark, privacy link, language and theme `SettingToggleGroup`s). Every mock-up a `figure role="img"` with an `aria-label`, no focusable content, no lime (FR-001–FR-005, FR-012, FR-013)
- [X] T006 [US1] Add the hero entry motion (`tw-animate-css`, 80 ms steps), `reveal` on each later section, and the readiness effect (answers at 500/750/1000 ms, count 0 → 83 over 600 ms, final state at once with reduced motion, timers cleared on unmount) in `frontend/src/app/home-page.tsx` (FR-007, FR-008)
- [X] T007 [P] [US1] Test in `frontend/src/app/home-page.test.tsx` that a visitor at `/` sees the main heading and that the links point to `/sign-up` (two), `/sign-in` and `/privacy`, and that choosing Polski switches the page (SC-003)

## Phase 4: User Story 2 — A signed-in person goes straight to their own area (P1)

**Goal**: `/` redirects a signed-in trainer and client.

**Independent Test**: Signed in as trainer → `/trainer/clients`; as client → `/app/today`; visitor stays on `/`.

- [X] T008 [US2] In `frontend/src/app/router.ts`, give `/` `loader: redirectIfSignedIn`, `HydrateFallback: BlankWhileLoading` and a lazy `HomePage` (FR-006, research R1, R2)
- [X] T009 [P] [US2] Test in `frontend/src/app/home-page.test.tsx` that a trainer lands on `/trainer/clients`, a client on `/app/today`, and a failed check shows the page (SC-002)

## Phase 5: User Story 3 — Reads well on a phone and a desktop (P2)

**Goal**: Layout per board at 375 px and 1280 px, both themes, both languages.

- [X] T010 [US3] Responsive classes in `frontend/src/app/home-page.tsx`: one column below `lg`; hero and closing split `5fr/7fr` from `lg`; desk mock-up shown from `lg`; steps and facts 3 columns from `md`; call-to-action buttons full width below `sm`; nothing wider than the viewport at 320 px

## Phase 6: Polish

- [X] T011 [P] Add **Visitor** under People in `CONTEXT.md` (FR-009)
- [X] T012 Verify in the built-in browser per `quickstart.md`: 375 px and 1280 px, light and dark, EN and PL, keyboard focus, console; review against ui-ux-pro-max for WCAG 2.2 AA
- [X] T013 Run `npm run check` in `frontend/`

## Dependencies

- T001–T003 before T005/T006; T004 before T005.
- US1 (T005–T007) before US3 (T010, same file). US2 (T008–T009) is independent of the page's content.
- T012–T013 last.

## Parallel examples

- T002 with T001/T003 (different files).
- T007 and T009 share a file; write them together after T005 and T008.
- T011 any time.

## Implementation strategy

One task (#314), one commit. Build Setup → Foundational → US1 → US2 → US3, then verify and run the gate.
