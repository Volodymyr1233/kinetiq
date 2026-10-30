# Implementation Plan: Client sees their progress

**Branch**: `feat/159-shadcn-chart` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/156-client-progress/spec.md`

## Summary

A client sees their progress over 4, 8 or 12 weeks in charts that always use the same colours for the same things and can be read by everyone. The story has five tasks. This branch builds **#159 only**: the building block of the charts (shadcn's `chart` component, the mapping of the five series meanings to the design tokens, and a thin wrapper that enforces the reading rules). #157, #158, #160 and #161 (the numbers, the API, the screen) are planned only at overview level.

Checking light and dark showed that the RPE colour is too close to the app's red; the plan moves it to the design system's orange (see [research.md](research.md)).

## Technical Context

**Language/Version**: TypeScript 6, React 19 (frontend)

**Primary Dependencies**: shadcn `chart` component (adds `recharts@3.8.0`), Tailwind CSS 4 tokens in `src/index.css`, react-i18next

**Storage**: none

**Testing**: Vitest with Testing Library; a contrast test over `src/index.css`; screenshots of a temporary preview page in light and dark (not committed)

**Target Platform**: the client area (375 px) and the trainer area (1280 px), light and dark

**Project Type**: web application, frontend only for #159

**Performance Goals**: none specific; no animation under reduced motion

**Constraints**: only design tokens for colour (no palette class, no hex); at most three series; no pie or radar; WCAG 2.2 AA; every text in English and Polish

**Scale/Scope**: five series meanings, one wrapper component, two kinds of chart (line, bar)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Result | Note |
|---|---|---|
| I. Hexagonal Boundaries | Not applicable | Frontend only |
| II. One Contract | Pass | No API change, no `schema.d.ts` change |
| III. Secure by Default | Pass | No data, no endpoint |
| IV. Explainable, Approved Plan Changes | Not applicable | No plan change here; the charts' later RPE and readiness are shown without alarm colours |
| V. Tests Verify Behaviour | Pass | Tests check what a person notices (title, text version, three-series limit, reduced motion) and the colour rules by calculation; no test of library internals or classes |
| VI. Simplicity | Pass | One wrapper, one mapping module, no chart kind the design forbids; the dependency is added by the task that needs it |
| VII. One Design System | Pass, with one design-system edit | Colours only from tokens, shadcn component added by the CLI, both languages; `--chart-2` changes to the system's own orange (research.md) and is flagged in the pull request; checked in light and dark by screenshots, and by eye in a real browser as a manual step |

No violations; Complexity Tracking is empty.

## Phase 0: Research

See [research.md](research.md). All unknowns are resolved: the component, the mapping, the wrapper, the contrast numbers and the RPE colour, how to test without a browser, how to look at light and dark.

## Phase 1: Design

- The meanings, the wrapper's props and the data shape: [data-model.md](data-model.md)
- The building block's interface: [contracts/chart-building-block.md](contracts/chart-building-block.md)
- How to prove the task works, including the manual browser check: [quickstart.md](quickstart.md)

**Re-check after design**: unchanged. The only edit outside the new files is the two values of `--chart-2` in `src/index.css`.

## Project Structure

### Documentation (this feature)

```text
specs/156-client-progress/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── chart-building-block.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
frontend/package.json                       # #159: recharts (added by the shadcn CLI)
frontend/src/components/ui/chart.tsx        # #159: shadcn chart, added by the CLI, unedited
frontend/src/lib/chart-series.ts            # #159: the five meanings, tokens, dashed flag, config builder
frontend/src/lib/chart-series.test.ts       # #159
frontend/src/lib/chart-colors.test.ts       # #159: contrast, distinct colours, RPE not red, from index.css
frontend/src/components/finding-chart.tsx   # #159: title, kinds, legend, text alternative, reduced motion
frontend/src/components/finding-chart.test.tsx # #159
frontend/src/index.css                      # #159: two values of --chart-2 (light, dark)
frontend/src/locales/en.json, pl.json       # #159: charts.series.* names, the hidden table's text

# not committed, for looking at the result:
#   frontend/chart-preview.html and frontend/src/chart-preview.tsx   (deleted after the screenshots)

# planned, built by later tasks:
#   #157, #158  backend statistics queries and API (backend, new feature package or readiness/statistics)
#   #160        frontend/src/features/progress/... the Progress screen using FindingChart
#   #161        progress by exercise on the same screen
```

**Structure Decision**: The building block is shared by client and trainer charts, so it lives in `components/` and `lib/`, not inside a feature. Features import it.

## Complexity Tracking

No violations to justify.
