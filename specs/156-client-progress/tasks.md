---
description: "Tasks for story #156, Client sees their progress"
---

# Tasks: Client sees their progress

**Input**: `specs/156-client-progress/` (spec.md, plan.md, research.md, data-model.md, contracts/chart-building-block.md, quickstart.md), building on `specs/18-sign-in-out`

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #159 builds the chart building block (US1 and the chart half of US2), #157 and #158 the numbers and their API, #160 the Progress screen with its first chart (US3), #161 the progress by exercise (US3). **Only #159 is built on the branch `feat/159-shadcn-chart`**; the phases of #157, #158, #160 and #161 stay unchecked and are carried out in their own pull requests when their blockers are closed (#157 waits for #99, #158 for #157, #160 for #158 and #159, #161 for #160).

**Paths**: `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 same colours for the same things, US2 a chart can be read by everyone, US3 client sees progress over 4, 8 or 12 weeks)

---

## Phase 1: Setup

- [X] T001 Commit `specs/156-client-progress/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #159, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: the chart vocabulary is the design system's

---

## Phase 2: task #159, shadcn chart with Kinetiq series tokens (US1, US2)

**Goal**: One chart building block: shadcn's chart, the five meanings mapped to the tokens, and a thin wrapper that enforces the reading rules. No data, no route.

**Independent Test**: `npx vitest run src/lib/chart-colors.test.ts src/lib/chart-series.test.ts src/components/finding-chart.test.tsx` passes; screenshots in light and dark show five different colours, the previous period dashed, RPE orange.

- [X] T002 [US1] Add the component through the CLI from `frontend/`: `npx shadcn@latest add chart` (adds `F/components/ui/chart.tsx` and `recharts@3.8.0` to `frontend/package.json`); read the added file; do not edit it unless a rule of the spec cannot be met otherwise (research.md)
- [X] T003 [US1] Change `--chart-2` in `F/index.css`: light to `oklch(0.6 0.17 45)`, dark to `oklch(0.76 0.15 50)` (the values of `--rpe-hard`; research.md Finding 2); update the one-line comment beside it; no other token changes
- [X] T004 [US1] Create `F/lib/chart-series.ts`: type `SeriesMeaning` = `'completion' | 'rpe' | 'load' | 'wearables' | 'previous'`; `SERIES` mapping each meaning to its token (`--chart-1` completion, `--chart-2` rpe, `--chart-3` load, `--chart-4` wearables, `--chart-5` previous) and `dashed` (true only for `previous`); a function taking one to three `{ meaning, dataKey }` entries and the translation function and returning the shadcn chart config (`label` from `charts.series.<meaning>`, `color: var(--chart-N)`); more than three entries throw (FR-002 to FR-005, data-model.md)
- [X] T005 [P] [US2] Add the series names to `F/locales/en.json` and `F/locales/pl.json` under `charts.series`: completion "Sessions done" / "Wykonane sesje", rpe "RPE" / "RPE", load "Load" / "Obciążenie", wearables "Heart rate and kcal" / "Tętno i kcal", previous "Previous period" / "Poprzedni okres" (FR-015)
- [X] T006 [US2] Create `F/components/finding-chart.tsx` as in contracts/chart-building-block.md: required `title` (heading and figure name), `kind` `'line' | 'bar'` only, `data`, `xKey`, `xLabel`, one to three `series` (tuple type), `emptyText`; each series in the colour of its meaning, `previous` dashed and neutral; a legend naming every series; a tooltip; a visually hidden table (caption = title, first column `xLabel`, one column per series named in words) as the text alternative; no animation under `prefers-reduced-motion: reduce`; no colour prop, no pie or radar, no fourth series; colours only from tokens (FR-001, FR-003 to FR-005, FR-009 to FR-014)
- [X] T007 [P] [US1] Test `F/lib/chart-colors.test.ts`: read `F/index.css`, take the five chart tokens, `--card` and `--destructive` from `:root` and `.dark`, convert OKLCH to sRGB, and check: each chart colour has at least 3:1 contrast against `--card` in light and in dark (FR-006); the five are pairwise different by a colour-distance floor (SC-001); the RPE colour has hue of at least 40 and at least 15 degrees from the destructive hue in both themes (FR-007)
- [X] T008 [P] [US1] Test `F/lib/chart-series.test.ts`: each meaning maps to its own token; only `previous` is dashed; the config builder returns the translated label and `var(--chart-N)` colour per entry; a fourth entry throws (FR-002, FR-003, FR-005)
- [X] T009 [US2] Test `F/components/finding-chart.test.tsx` with Testing Library: the title is a heading and names the figure; the hidden table has the caption, the `xLabel` column, one column per series named in words and one row per data row; with no rows the `emptyText` shows and no table of values; with `prefers-reduced-motion` mocked as reduce, animation is off; a chart with four series is refused (type error, checked with `// @ts-expect-error`, and a runtime error); an English and a Polish render show the translated series names (FR-010 to FR-015)

**Checkpoint**: US1 and the chart half of US2 work when T002 to T009 pass.

---

## Phase 3: Verify task #159

- [X] T010 Look at it (quickstart step 5): create the temporary preview page `frontend/chart-preview.html` and `F/chart-preview.tsx` (do not commit them) showing, in a card, a sample `FindingChart` for each of the five meanings and one with three series including `previous`, with the theme set from `?theme=light|dark`; start `npm run dev`; take screenshots with headless Microsoft Edge in light and in dark, at 375 px and at 1280 px wide, into a temporary folder outside the repository; look at them and check: five different colours, `previous` dashed, RPE orange and not red, every series named in words, nothing overlapping; fix what is wrong; delete the preview files and stop the dev server
- [X] T011 Mutation checks (quickstart step 4): set `--chart-2` back to hue 35 and `chart-colors.test.ts` MUST fail; swap two tokens in `chart-series.ts` and `chart-series.test.ts` MUST fail; remove the title requirement or the hidden table from `finding-chart.tsx` and `finding-chart.test.tsx` MUST fail; restore the code afterwards
- [X] T012 Run `npm run check` in `frontend/`; ESLint, Prettier, tests, type check and build all pass
- [X] T013 Run `/speckit-converge` for this task; commit the code, tests and spec locally as `feat(frontend): add chart building block with Kinetiq series tokens`; before the Definition of Done box "checked in the browser, light and dark" is ticked, a person looks at the preview in a real browser (quickstart step 6), and the pull request says `--chart-2` was changed

---

## Phase 4: task #157, statistics queries over logs (US3) — later pull request

**Waits for**: #99 (finish API and session states).

- [ ] T014 [US3] Re-plan #157 in this folder when it starts; queries for sessions done of planned, mean RPE against the 7 to 8 target, and best set per exercise, for 4, 8 and 12 weeks, each with the previous week (FR-016, FR-017); tests with logs of a client and of another client (FR-018)

---

## Phase 5: task #158, statistics API (US3) — later pull request

**Waits for**: #157.

- [ ] T015 [US3] Endpoint for the signed-in client's numbers; another person's numbers answer `404` with a test; regenerate `frontend/src/lib/api/schema.d.ts` in the same pull request

---

## Phase 6: task #160, progress screen with tiles and sessions chart (US3) — later pull request

**Waits for**: #158 and #159.

- [ ] T016 [US3] Client Progress screen: period choice (4, 8, 12 weeks), tiles for sessions done and mean RPE with the change from the previous week, and the sessions chart built with `FindingChart` and a title that states the finding; empty state without logs (FR-016, FR-017, FR-019); texts in English and Polish; checked in the browser, light and dark, at 375 px

---

## Phase 7: task #161, progress by exercise (US3) — later pull request

**Waits for**: #160.

- [ ] T017 [US3] Best set per exercise with its change from the previous week on the Progress screen, with a chart built with `FindingChart`; texts in both languages; checked in the browser, light and dark, at 375 px

---

## Dependencies and order

- #159 has no blocker and is built first (this branch). #160 uses its building block; #165 and #142 (trainer charts) will too.
- Within #159: T002 → T003 and T004; T005 independent; T006 after T004 and T005; T007 and T008 independent after T003 and T004; T009 after T006; T010 to T013 last. T001 is committed together with T013.
- #157 waits for #99; #158 for #157; #160 for #158 and #159; #161 for #160.

## Parallel opportunities (#159)

T005, T007 and T008 touch different files and can be written together once T003 and T004 exist.

## Implementation strategy

MVP of this branch is US1 and the chart half of US2 (#159): the building block, tested by calculation and by looking at it. Stop after T013; what only a person can verify (the tooltip by keyboard, a screen reader, a real browser at 375 px) is listed in quickstart step 6.

---

## Phase 8: Convergence

- [X] T018 Record the props of Recharts' `Bar` in `frontend/src/components/finding-chart.test.tsx` the way `Line` is recorded, and test for `kind="bar"`: each series fills with `var(--color-<meaning>)`, only the previous period is lighter with a dashed outline, and animation is off under reduced motion and on otherwise per FR-002, FR-003, FR-014 (partial)
- [X] T019 Refuse an empty or blank `title` in `frontend/src/components/finding-chart.tsx` (throw, as `chartConfig` does for a fourth series) and test it in `frontend/src/components/finding-chart.test.tsx` per FR-010 (partial)
