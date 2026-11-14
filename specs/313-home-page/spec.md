# Feature Specification: Visitor sees what Kinetiq does and signs up

**Feature Branch**: `feat/313-home-page`

**Created**: 2026-11-14

**Status**: Draft

**Input**: Story #313 "Visitor sees what Kinetiq does and signs up" and its task #314 "Public home page". Screens: Kinetiq Screens, page Home, boards "Home · desktop" (1280 px) and "Home · phone" (375 px). Design: the Kinetiq design system, its brand rule (at most one lime element per view) and its motion section (enter 200 ms, opacity and transform only, nothing loops). Builds on spec `2-trainer-sign-up` (the sign-up page), `18-sign-in-out` (sign-in and the area each role lands in) and `167-data-protection` (the privacy notice and the health data consent). The story, the task and the boards are starting points; every place this spec departs from them is listed under "Departures from the story and the boards".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visitor learns what Kinetiq does and signs up as a trainer (Priority: P1)

A visitor opens the address of Kinetiq. Without signing in, they read what it does for a trainer and their clients, and reach trainer sign-up with one action.

**Why this priority**: The story's first acceptance criterion. Today `/` shows a placeholder, so a visitor has no way to learn what Kinetiq is or where to start.

**Independent Test**: Signed out, open `/`: the page names Kinetiq, says who it is for, shows how it works, and "Create trainer account" opens the sign-up page; "Sign in" opens the sign-in page; "Privacy notice" opens the privacy notice.

**Acceptance Scenarios**:

1. **Given** a visitor, **When** they open `/`, **Then** they see the header (brand mark, "Sign in"), the hero for trainers with "Create trainer account", How it works in three steps, At your desk and On their phone, Plan changes, the closing call to action with three data facts, and the footer.
2. **Given** a visitor on `/`, **When** they choose "Create trainer account" in the hero or in the closing call to action, **Then** the trainer sign-up page opens.
3. **Given** a visitor on `/`, **When** they choose "Sign in", **Then** the sign-in page opens.
4. **Given** a visitor on `/`, **When** they choose a privacy notice link, **Then** the privacy notice opens.
5. **Given** a visitor on `/`, **When** they pick a language or a theme in the footer, **Then** the page switches at once and keeps the choice on their next visit, as the account pages do.

---

### User Story 2 - A signed-in person goes straight to their own area (Priority: P1)

A trainer or client who is already signed in and opens `/` lands in their own area without seeing the public page.

**Why this priority**: The story's second acceptance criterion. A signed-in person has nothing to do on the public page, and a bookmark of the bare address must still take them to work.

**Independent Test**: Signed in as a trainer, open `/`: the overview of clients opens. Signed in as a client, open `/`: Today opens.

**Acceptance Scenarios**:

1. **Given** a signed-in trainer, **When** they open `/`, **Then** they land on `/trainer/clients`.
2. **Given** a signed-in client, **When** they open `/`, **Then** they land on `/app/today`.
3. **Given** a visitor, or a person whose sign-in cannot be checked (the server cannot be reached), **When** they open `/`, **Then** the public page shows.

---

### User Story 3 - The page reads well on a phone and a desktop, in both themes and languages (Priority: P2)

Whether a visitor opens the page on a phone or a laptop, in English or Polish, light or dark, it is laid out for that screen and reads as one product with the rest of Kinetiq.

**Why this priority**: The story's third acceptance criterion. The page is the first thing a visitor sees, so it must look finished everywhere it opens.

**Independent Test**: Open `/` at 375 px and at 1280 px, each in light and dark and in English and Polish: nothing scrolls sideways, nothing is cut off, and every mock-up stays readable.

**Acceptance Scenarios**:

1. **Given** a 375 px wide screen, **When** the page shows, **Then** the content stacks in one column, the hero shows the client's phone, the call-to-action buttons fill the width, and nothing scrolls sideways.
2. **Given** a 1280 px wide screen, **When** the page shows, **Then** the hero and the closing call to action place their text beside their mock-up, and the hero shows the trainer's overview behind the client's phone.
3. **Given** the dark theme, **When** the page shows, **Then** every part uses the dark theme's colours and keeps its contrast.
4. **Given** Polish, **When** the page shows, **Then** every text is in Polish and still fits its place.
5. **Given** a person who asked for reduced motion, **When** the page shows, **Then** sections only fade in, and the phone mock-up shows its final readiness and answers at once.

### Edge Cases

- The sign-in check is slow: the page waits for it rather than flashing the public page before a redirect.
- A browser without scroll-linked animations shows every section as is, without a reveal.
- Keyboard: every link and control is reachable in reading order with a visible focus ring; the mock-ups hold no focusable element.
- Screen readers: each mock-up is announced once by a short description, not element by element.
- 320 px (200 % zoom at 640 px): nothing scrolls sideways.
- Long Polish words in buttons and headings wrap within their box.
- The mock-ups show made-up example people and figures, never real data, and need no request to the server.

## Requirements *(mandatory)*

### Functional Requirements

**Public page** (task #314)

- **FR-001**: `/` MUST show, to a visitor, in this order: a header with the Kinetiq brand mark and "Sign in"; a hero for trainers with a headline, a lead, one "Create trainer account" action and the note that signing up is free and clients join by invite, beside a mock-up of the trainer's overview and the client's phone; How it works in three steps (invite a client, they answer the quiz, you review and publish); At your desk and On their phone, each with what that side does and a small mock-up; Plan changes with a mock-up of a plan change; a closing call to action with "Create trainer account" beside a mock-up of inviting a client, and three data facts; a footer with the brand mark, a privacy notice link, the language and the theme.
- **FR-002**: "Create trainer account" MUST open the trainer sign-up page, "Sign in" the sign-in page, and every privacy notice link the privacy notice.
- **FR-003**: The three data facts MUST say only what Kinetiq does (spec 167, the privacy notice): a client agrees to the processing of their health data before joining; names and email addresses never go to the service that drafts plans; deleting an account erases its data.
- **FR-004**: The language and theme controls MUST work as everywhere else in Kinetiq: the choice applies at once and is remembered in the browser.
- **FR-005**: Each view MUST hold at most one lime element: the hero's "Create trainer account" at the top, the closing one at the end; the mock-ups use no lime.

**Signed-in redirect** (task #314)

- **FR-006**: A signed-in trainer who opens `/` MUST land on `/trainer/clients`, and a signed-in client on `/app/today`, by the same rule as after sign-in. A visitor, or anyone whose sign-in check fails, MUST see the public page.

**Motion** (task #314)

- **FR-007**: On load, the hero's parts MUST fade up in sequence; every later section MUST reveal once as it scrolls into view; the phone mock-up MUST fill its check-in answers and count its readiness up to its final value once. Nothing loops.
- **FR-008**: With reduced motion asked for, only fades MUST remain, and the phone mock-up MUST show its answers and final readiness from the start.

**Glossary and clean-up** (task #314)

- **FR-009**: CONTEXT.md MUST define **Visitor**: a person who opens Kinetiq without being signed in.
- **FR-010**: The placeholder texts and the placeholder page MUST be removed.

**Quality**

- **FR-011**: Every text MUST exist in English and Polish.
- **FR-012**: The page MUST meet WCAG 2.2 AA: one main heading and ordered section headings; each mock-up hidden from assistive technology and described once; the language and theme controls grouped and labelled with their current choice exposed; every target at least 24 × 24 px, the call-to-action buttons 48 px high; nothing scrolls sideways at 375 px or 320 px; light and dark contrast as the design system's tokens give.
- **FR-013**: The page MUST be built from Kinetiq's tokens and existing components, without new dependencies, and without restyling them.

### Key Entities

- **Visitor**: A person who opens Kinetiq without being signed in. No data is kept about them.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A visitor reaches the trainer sign-up page from `/` with one action, from the top of the page and from its end.
- **SC-002**: In tests, a signed-in trainer, a signed-in client and a visitor who open `/` land on `/trainer/clients`, `/app/today` and `/` respectively, in 100 % of cases.
- **SC-003**: In tests, each of the visitor's links (Create trainer account, Sign in, privacy notice) opens its page.
- **SC-004**: At 375 px and 1280 px, in light and dark, in English and Polish, the page shows with no sideways scroll, no cut-off text and no console error.
- **SC-005**: The page passes WCAG 2.2 AA checks for contrast, keyboard access, focus visibility and text alternatives.
- **SC-006**: No English or Polish text is missing, and no placeholder text is left.

## Departures from the story and the boards

| Source | It says | This spec | Why |
|---|---|---|---|
| Boards "Home · desktop" and "Home · phone" | Privacy links point to the top of the page | They open the privacy notice | A canvas has no privacy page; the app has one at `/privacy`. |
| Boards | Mock-ups show the canvas's example names and figures | The same made-up examples, translated where they are words | They illustrate; they are not data. |

## Assumptions

- Signing up is free and open to trainers only; clients join by invite (spec 2, spec 30), so the page has no sign-up for clients.
- The sign-in page already sends a signed-in person to their area; `/` follows the same rule, and each area's index takes the trainer to `/trainer/clients` and the client to `/app/today`.
- The page is frontend only: no new endpoint, no data, no migration and no demo data.
- The design system's tokens hold no display size above `text-3xl`; the hero headline needs one, so it is added as a token.
- Polish copy uses informal "ty", to be confirmed by a copywriter as the design system says.
