# Implementation Plan: Visitor sees what Kinetiq does and signs up

**Branch**: `feat/313-home-page` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/313-home-page/spec.md`

## Summary

`/` becomes the public home page of the Home boards (#314). The route gets the `redirectIfSignedIn` loader the sign-in page already uses, so a signed-in trainer lands on `/trainer/clients` and a client on `/app/today` through their area's index; a visitor, or a failed check, sees the page. The page is a lazy chunk in `src/app/home-page.tsx`, built from the installed shadcn components (`Button` with a new design system variant `brand`, `Card`, `Badge`, `Alert`, `Avatar`, `Separator`, and `SettingToggleGroup` for language and theme). Its mock-ups are static markup, hidden from assistive technology and described by one `aria-label` each. Motion uses `tw-animate-css` (already installed) for the hero, a scroll-timeline utility in `src/index.css` for the sections, and a small effect for the readiness count-up. Two display text tokens join `src/index.css`. CONTEXT.md gains **Visitor**; the `placeholder.*` strings go. No backend change.

## Technical Context

**Language/Version**: TypeScript, React 19

**Primary Dependencies**: Existing only: React Router 8 (route `loader`, `lazy`), react-i18next, shadcn components already in `components/ui/`, Lucide, `tw-animate-css`. No new dependency.

**Storage**: None. Language and theme keep their existing browser storage (`kinetiq-language`, `kinetiq-theme`).

**Testing**: Vitest + Testing Library through `renderAppAt` (the whole router with the app's query client): the redirect per role and for a visitor, and the visitor's links.

**Target Platform**: Browser, public page; checked at 375 px and 1280 px

**Project Type**: Web application, frontend only

**Performance Goals**: The page is its own chunk, so the areas' first load does not grow; the only request is the sign-in check the loader makes.

**Constraints**: Semantic tokens only; one lime element per view; the mock-ups use no lime; opacity and transform only, 200 ms enter, nothing loops; reduced motion leaves only fades and the final readiness; every string in English and Polish; `app/` may import from features, a feature never from `app/`.

**Scale/Scope**: One page, about 60 strings per language.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | Backend untouched. | n/a |
| II. One contract | No API change; no regenerated types. | n/a |
| III. Secure by default | No new endpoint; the loader only reads the current account, as on `/sign-in`. The mock-ups hold made-up examples, no data. Nothing reaches an LLM. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Tests for the three landings and the visitor's links; no tests of styling or animation (research R6). | Pass |
| VI. Simplicity | Reuses `redirectIfSignedIn`, `landingFor`, `SettingToggleGroup` and `tw-animate-css`; one CSS utility for the scroll reveal; the mock-ups are inline markup, not components of their own, since each is used once. | Pass |
| VII. One design system | Tokens and shadcn components; the lime action is `Button` variant `brand`, a variant the design system defines (kq.css `btn-brand`); checked in the browser in light and dark at 375 px and 1280 px, EN and PL. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/313-home-page/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── home-route.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
CONTEXT.md                                   # Visitor
frontend/src/
├── index.css                                # display text tokens, reveal utility
├── components/ui/button.tsx                 # variant brand
├── app/
│   ├── router.ts                            # `/`: loader redirectIfSignedIn, lazy HomePage
│   ├── home-page.tsx                        # the page, replacing the placeholder
│   └── home-page.test.tsx                   # landings and links
└── locales/en.json, pl.json                 # home.*, placeholder.* removed
```

**Structure Decision**: The page stays in `src/app/` beside the privacy notice and the not-found page: it belongs to no backend feature. It imports `SettingToggleGroup` from `features/account`, as `app/` already imports from features.

## Complexity Tracking

None.
