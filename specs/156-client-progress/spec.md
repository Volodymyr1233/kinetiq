# Feature Specification: Client sees their progress

**Feature Branch**: `feat/159-shadcn-chart`

**Created**: 2026-10-30

**Status**: Draft

**Input**: Story #156 "Client sees their progress" and its tasks #157 (statistics queries over logs), #158 (statistics API), #159 (add shadcn chart with Kinetiq series tokens), #160 (progress screen: tiles and sessions chart) and #161 (progress by exercise). Screen: Kinetiq Screens, board Client → "Progress". Design: the Kinetiq design system, section 3.7 (charts) and `frontend/CLAUDE.md` (Design system). Builds on spec `specs/18-sign-in-out` (the signed-in client). The logs the numbers come from are the stories "Client logs sets" (#92) and "Client finishes a session" (#98); the trainer's view of the same numbers is story #162. This spec is mostly about the building block of the charts, which is the first task (#159) to be built; the screen and the numbers follow in #157, #158, #160 and #161.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Every chart uses the same colours for the same things (Priority: P1)

Whoever builds a chart in Kinetiq picks what a line or a bar means (completion, RPE, load or volume, a wearable value, the previous period) and gets the colour that meaning always has, in light and in dark. A client who has seen RPE in orange once finds it in orange everywhere.

**Why this priority**: Acceptance criterion of the story ("fixed series colours") and the whole of task #159. Every chart of the progress screens (#160, #161) and the later trainer charts (#142, #165) stand on it.

**Independent Test**: Draw one sample chart with each of the five meanings in the light theme and in the dark theme: each meaning has its own colour, the same colour in every chart, and the previous period is dashed.

**Acceptance Scenarios**:

1. **Given** a chart that shows completion, **When** it is drawn, **Then** completion has the primary series colour, in light and in dark.
2. **Given** a chart that shows RPE, **When** it is drawn, **Then** RPE has the RPE colour, always, whatever else the chart shows and whatever its values.
3. **Given** a chart that shows load or volume, a wearable value (heart rate, kcal), or the previous period, **When** it is drawn, **Then** each has the colour of its own meaning, and no two meanings share a colour.
4. **Given** a chart that compares with the previous period, **When** it is drawn, **Then** the previous period is a neutral dashed line, never a solid one.
5. **Given** the same meaning in two different charts, **When** both are drawn, **Then** it has the same colour in both.
6. **Given** the theme changes between light and dark, **When** a chart is on screen, **Then** its colours follow the theme at once, without a reload.

---

### User Story 2 - A chart can be read by everyone (Priority: P1)

A chart is readable by someone who cannot tell its colours apart, by someone using a screen reader, and by someone on a phone in daylight.

**Why this priority**: Acceptance criterion of the story ("charts follow the design system rules"), accessibility target WCAG 2.2 AA, and the rule "no state is shown by colour alone".

**Independent Test**: Look at a sample chart in greyscale: every series can still be told apart by its label or its line style; read it with a screen reader: it announces a title that states the finding and a text version of the data.

**Acceptance Scenarios**:

1. **Given** a chart, **When** it is drawn, **Then** it has a title that states the finding ("You trained 4 of 5 planned sessions"), not only the subject ("Sessions").
2. **Given** a chart with more than one series, **When** it is drawn, **Then** each series is named in a legend or label in words, so no series is told only by its colour.
3. **Given** a chart, **When** the person hovers over or focuses a point, **Then** the value and the name of its series show, reachable by keyboard as well as by pointer.
4. **Given** a screen reader, **When** it reaches a chart, **Then** it announces the title and gives the data as text (for example as a table or a sentence), not as an unlabelled image.
5. **Given** any series line or bar, **When** it is drawn on the card in light, **Then** it has a contrast of at least 3:1 against the card; in dark the same.
6. **Given** RPE above its target or low readiness, **When** a chart shows it, **Then** it is never red (low readiness and high RPE are not errors).

---

### User Story 3 - Client sees their progress over 4, 8 or 12 weeks (Priority: P2)

On the Progress screen a client picks a period of 4, 8 or 12 weeks and sees, each compared with the previous week: sessions done of planned, mean RPE against the 7 to 8 target, and the best set per exercise.

**Why this priority**: The story itself. It is built by #157, #158, #160 and #161 after the building block exists; this spec records it so the building block is shaped for it.

**Independent Test**: Sign in as a client with logged sessions, open Progress and choose 8 weeks: the tiles and charts show the last 8 weeks, and each number says how it changed from the previous week.

**Acceptance Scenarios**:

1. **Given** a client with logs, **When** they open Progress, **Then** they can choose 4, 8 or 12 weeks, and the screen shows that period.
2. **Given** the period, **When** the screen shows sessions, **Then** it shows sessions done of planned and how that compares with the previous week.
3. **Given** the period, **When** the screen shows RPE, **Then** it shows the mean RPE against the 7 to 8 target and how it compares with the previous week, never in red.
4. **Given** the period, **When** the screen shows exercises, **Then** it shows the best set per exercise and how it compares with the previous week.
5. **Given** a client with no logs yet, **When** they open Progress, **Then** they see an empty state that says what to do, not an empty chart.

---

### Edge Cases

- A chart with no data in the period shows an empty state, not an empty axis.
- A chart never has more than three series, so the colours stay distinguishable.
- A series with one point is shown as a point, not as a missing line.
- Very long series names and Polish names (about 30% longer than English) do not overlap on a 375 px screen.
- A number is shown in the person's language and units (spec 42: `82,5 kg` against `82.5 kg`).
- The chart's colours come only from the design tokens; a colour written in a chart's own code, a palette class or a hex value is not allowed.
- No pie and no radar chart is offered.
- Reduced motion: a chart does not animate when the person asks for less motion.

## Requirements *(mandatory)*

### Functional Requirements

**Chart building block** (task #159)

- **FR-001**: Kinetiq MUST have one reusable chart building block, built from the design system's chart component, that every chart of the app uses.
- **FR-002**: The building block MUST give each of five meanings its own colour: completion (the primary series), RPE, load or volume, wearables (heart rate, kcal) and the previous period. A meaning has that colour in every chart, in light and in dark.
- **FR-003**: The previous period MUST be drawn dashed and neutral; no other series is dashed.
- **FR-004**: Every colour of a chart MUST come from the design tokens (`--chart-1` to `--chart-5`); a chart MUST NOT contain a colour value, a palette class or a hex value of its own.
- **FR-005**: A chart MUST have at most three series.
- **FR-006**: Every series line or bar MUST have a contrast of at least 3:1 against the card, in light and in dark.
- **FR-007**: The colour of RPE MUST NOT be red, and no chart uses red for a high RPE or a low readiness.
- **FR-008**: A chart MUST follow the theme (light, dark, system) at once.
- **FR-009**: The building block MUST NOT offer pie or radar charts.

**Reading a chart** (task #159 and each chart's own task)

- **FR-010**: Every chart MUST have a title that states the finding; the building block MUST NOT allow a chart without a title.
- **FR-011**: Every series MUST be named in words in a legend or label, so it is not told by colour alone.
- **FR-012**: A chart MUST show the value and series name of a point on hover and on keyboard focus.
- **FR-013**: A chart MUST give a text version of its data to a screen reader.
- **FR-014**: A chart MUST NOT animate when the person prefers reduced motion.
- **FR-015**: Every text of a chart (title, series names, empty state) MUST exist in English and Polish.

**Progress screen** (tasks #157, #158, #160, #161)

- **FR-016**: The client MUST be able to choose a period of 4, 8 or 12 weeks.
- **FR-017**: Progress MUST show sessions done of planned, mean RPE against the 7 to 8 target, and the best set per exercise, each compared with the previous week.
- **FR-018**: A client MUST see only their own numbers; another person's numbers answer "not found".
- **FR-019**: A client with no logs MUST see an empty state that says what to do.

### Key Entities

- **Series meaning**: One of five fixed meanings a chart line or bar can have (completion, RPE, load or volume, wearables, previous period). Each has one colour token and, for the previous period, a dashed style.
- **Chart**: A titled figure of at most three series over a period, with a text version of its data.
- **Progress period**: 4, 8 or 12 weeks ending with the current week.

### Public endpoints

None. The numbers of #158 need a signed-in client (spec 18).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All five series meanings are visibly different from each other in light and in dark, and each reaches a contrast of at least 3:1 against the card.
- **SC-002**: A meaning has the same colour in every chart of the app; a chart cannot be built with a colour of its own.
- **SC-003**: A chart can be understood in greyscale: every series is named in words, and the previous period is told by its dashes.
- **SC-004**: A screen reader announces a title that states the finding and the data as text for every chart.
- **SC-005**: A client finds the sessions, the mean RPE and the best set per exercise of the last 4, 8 or 12 weeks, each with its change from the previous week, within one screen and two taps.

## Assumptions

- The five tokens already exist in `frontend/src/index.css` for light and dark, with the meanings of FR-002; task #159 adds the chart component and fixes how a chart author picks a meaning, and does not change the token values unless a contrast check requires it.
- Task #159 builds only the building block. It draws no real data and adds no route; the first real chart is #160.
- The design system measured chart lines at 3.1:1 or better; this task re-checks it for the five colours in both themes rather than trusting the figure.
- The colours are checked for contrast by calculation and in a real browser in light and dark (constitution VII); until someone has looked at a chart in a browser, the task is not verified visually.
- Charts are line and bar charts only; other kinds are not offered. Which of the two a chart uses is decided by the screen that needs it.
- The text version of a chart's data can be a visually hidden table; the form is chosen in the plan.
