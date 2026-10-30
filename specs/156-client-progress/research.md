# Research: Client sees their progress

Findings for task #159, checked on 2026-10-30 against the shadcn CLI, `frontend/src/index.css` and by calculation. Tasks #157, #158, #160 and #161 are researched when they start.

## The chart component

- **Decision**: Add shadcn's `chart` through the CLI (`npx shadcn@latest add chart`), unedited.
- **Rationale**: `frontend/CLAUDE.md` requires shadcn components added through the CLI and never overwritten. A dry run shows it adds one file, `src/components/ui/chart.tsx`, and one dependency, `recharts@3.8.0` (React 19 compatible). The component provides a container that sets `--color-<key>` variables from a config, a tooltip and a legend.
- **Alternatives considered**: Chart.js (rejected: the design system chose shadcn Chart on Recharts); a hand-drawn SVG (rejected: a hand-rolled replacement for an existing component, constitution VII).

## The five meanings and their tokens

- **Decision**: A small module `src/lib/chart-series.ts` owns the mapping. A meaning is one of `completion`, `rpe`, `load`, `wearables`, `previous`; each maps to its token (`--chart-1` to `--chart-5`) and `previous` is the only dashed one. A helper builds the library's chart config from at most three meanings, using the translated series names.
- **Rationale**: Spec FR-002 to FR-005: the colour of a meaning must be the same in every chart and a chart author must not write a colour. A chart is built from meanings, never from colours, so a wrong colour cannot be typed. The tokens already exist for light and dark and are already mapped to `--color-chart-1…5` in `index.css`.
- **Alternatives considered**: Letting each chart pass `var(--chart-2)` itself (rejected: five different spellings of the same rule, and nothing stops a hex value); renaming the tokens by meaning (rejected: the design system and the shadcn chart default both use `--chart-N`).

## A thin wrapper that enforces the reading rules

- **Decision**: One component `src/components/finding-chart.tsx` wraps the library's container. It requires a `title` (rendered as a heading and as the figure's name), takes `kind` of `line` or `bar` only, takes at most three series (a tuple type, and a runtime error in development), names every series in a legend, shows a tooltip, adds a visually hidden table of the data as the text alternative, and turns animation off under `prefers-reduced-motion`.
- **Rationale**: Spec FR-005, FR-009 to FR-014. Each of these must hold for every chart of the app (#160, #161, #142, #165), so they live in one place and are not repeated per screen. It stays thin: no data fetching, no layout of its own beyond what a chart needs, nothing for a chart kind the design system forbids.
- **Alternatives considered**: Only adding `ui/chart.tsx` and the mapping and leaving title, legend and text alternative to each screen (rejected: the first real chart (#160) would carry them and the others would copy or forget them; spec FR-010 says the block must not allow a chart without a title); a full chart library of our own (rejected: constitution VI).

## Contrast and the colour of RPE: measured

Calculation (OKLCH to sRGB, WCAG relative luminance), against `--card`:

| Series | Light | Contrast | Dark | Contrast |
|---|---|---|---|---|
| completion (`--chart-1`) | `#4f950f` | 3.73:1 | `#a4e550` | 12.13:1 |
| RPE (`--chart-2`, as it is now) | `#d23f18` | 4.68:1 | `#fb784f` | 6.90:1 |
| load (`--chart-3`) | `#1762b6` | 6.07:1 | `#53a3f2` | 6.88:1 |
| wearables (`--chart-4`) | `#8e47cd` | 5.36:1 | `#bc88f4` | 6.94:1 |
| previous (`--chart-5`) | `#8f9299` | 3.11:1 | `#6e7279` | 3.77:1 |

- **Finding 1**: every series reaches 3:1 against the card in both themes. The lowest is the previous period in light (3.11:1), only just above the bar; it is a dashed neutral line, so it is kept.
- **Finding 2**: the RPE colour as it is now is `#d23f18` in light and `#fb784f` in dark. Next to the app's destructive red (`#dc2626` in light, `#f75d59` in dark) they are the same family: oklch hue 35 against 27 in light and 38 against 25 in dark. That breaks the rule that RPE is never red (spec FR-007, design rule "high RPE is not an error"): a line that looks red reads as an error.
- **Decision**: Move `--chart-2` to the orange the design system already uses for "hard" RPE: `oklch(0.6 0.17 45)` in light (`#cf5604`, 4.23:1 on the card) and `oklch(0.76 0.15 50)` in dark (`#fc9252`, 8.13:1), the values of `--rpe-hard`. A test keeps it from drifting back: the RPE hue must be at least 40 and at least 15 degrees away from the destructive hue in both themes.
- **Rationale**: The rule and the task's "light and dark checked" both point here, and the new values are the design system's own orange, so no new colour is invented. They stay clearly different from completion (green), load (blue), wearables (purple) and the previous period (grey).
- **Alternatives considered**: Leave the token and only report it (rejected: this task exists to check light and dark, and the check fails; the change is two values); pick a new yellow (rejected: low contrast on white). The change is a design-system edit: the pull request says so, so the design owner can object.

## Testing contrast without a browser

- **Decision**: A Vitest test reads `src/index.css`, takes the chart tokens, `--card` and `--destructive` from `:root` and `.dark`, converts OKLCH to sRGB and checks: each chart colour has at least 3:1 against the card, the five colours are pairwise different (colour distance above a floor), and the RPE rule above.
- **Rationale**: The values live in one file and the check needs no browser, so it runs in `npm run check` and stops a later edit of `index.css` from breaking a rule silently.
- **Alternatives considered**: A visual-regression tool (rejected: a dependency and baseline images for one component, constitution VI).

## Checking light and dark by eye

- **Decision**: A temporary preview page (not committed) rendered by the Vite dev server shows a sample chart for every meaning, and headless Microsoft Edge takes a screenshot in light and in dark; the screenshots are looked at and kept outside the repository. The manual check in a real browser with the person's own eyes is a step in `quickstart.md`.
- **Rationale**: Constitution VII asks for a check in a real browser in light and dark; the screenshots come from headless Edge, which the development machines have. It does not replace a person looking at it, and the task says so.
- **Alternatives considered**: Declaring the visual check done from the contrast numbers alone (rejected: it would claim something nobody saw).

## Testing the component in jsdom

- **Decision**: Test what a person can notice without layout: the heading with the finding, the hidden data table (rows, names), that a chart without a title does not compile (type test) and that more than three series is refused, and the reduced-motion flag. A jsdom layout has no width, so drawing details (axis, legend position) are checked in the preview screenshots, not in unit tests.
- **Rationale**: `frontend/CLAUDE.md`: test behaviour a user would notice, not library behaviour or styling.
- **Alternatives considered**: Mocking Recharts (rejected: tests that only test the mock).

## What stays open for later tasks

- **#157, #158**: the statistics queries and API; the 4, 8 and 12 week periods and the comparison with the previous week.
- **#160**: the Progress screen with tiles and the sessions chart, the first real chart; it chooses line or bar.
- **#161**: progress by exercise.
- The chart's number format follows the person's language and units; #160 passes formatted values in.
