# Contract: the chart building block (task #159)

No endpoint. The interface is what a screen imports and how it uses it. Names are the plan's; the implementation follows them.

## `src/lib/chart-series.ts`

- `SeriesMeaning`: `'completion' | 'rpe' | 'load' | 'wearables' | 'previous'`.
- `SERIES`: for each meaning its token (`'--chart-1'` … `'--chart-5'`) and `dashed` (true only for `previous`).
- A function that takes up to three entries `{ meaning, dataKey }` and the translation function and returns the chart config the shadcn chart expects (`label` and `color: var(--chart-N)` per entry). More than three entries throw.

## `src/components/finding-chart.tsx`

```tsx
<FindingChart
  title="You trained 4 of 5 planned sessions"   // the finding, required
  kind="line"                                   // 'line' | 'bar'
  data={rows}                                   // rows of numbers
  xKey="week"
  xLabel={t('progress.week')}                   // heading of the x column in the text version
  series={[{ meaning: 'completion', dataKey: 'done' }, { meaning: 'previous', dataKey: 'before' }]}  // 1 to 3
  emptyText={t('progress.noLogs')}              // shown instead of an empty chart
/>
```

Behaviour:

- Renders a figure named by `title`, with the title shown as a heading.
- Draws each series in the colour of its meaning; `previous` is a dashed neutral line (or a lighter bar).
- A legend names every series in words; a tooltip shows value and series name on hover and on keyboard focus.
- A visually hidden table is the text version: caption = title, first column `xLabel`, one column per series.
- With no rows, shows `emptyText` and no axes.
- Under `prefers-reduced-motion: reduce`, no animation.
- Props that would allow what the design forbids do not exist: no colour prop, no pie or radar kind, no fourth series.

## Texts

`charts.series.completion|rpe|load|wearables|previous` in `en.json` and `pl.json`, and the text of the hidden table if it needs any.
