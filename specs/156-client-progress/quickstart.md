# Quickstart: validating task #159 (chart building block)

Run in `frontend/`.

## 1. The colour rules, by calculation

```bash
npx vitest run src/lib/chart-colors.test.ts
```

Expected: passes. Each of the five chart colours has at least 3:1 against the card in light and in dark; the five are pairwise different; the RPE colour is orange, not red (hue at least 40 and at least 15 degrees from the destructive hue), in both themes.

## 2. The mapping and the component

```bash
npx vitest run src/lib/chart-series.test.ts src/components/finding-chart.test.tsx
```

Expected: passes. A meaning has its own token and only `previous` is dashed; more than three series is refused; the chart shows its title as a heading, a hidden table with the data and the series names in words, an empty state without data, and no animation under reduced motion.

## 3. The whole quality gate

```bash
npm run check
```

Expected: ESLint, Prettier, tests, type check and build all pass.

## 4. A test that bites

Change the RPE token back to its old hue (35) and run step 1: the test MUST fail. Swap two tokens in `chart-series.ts` and run step 2: the mapping test MUST fail. Restore the code.

## 5. Looking at it: screenshots in light and dark

1. Add the temporary preview page `chart-preview.html` and `src/chart-preview.tsx` (not committed) showing a sample chart for every meaning in a card.
2. `npm run dev`, then take screenshots with headless Edge of `http://localhost:5173/chart-preview.html` in light and in dark (the page sets the theme from `?theme=light|dark`).
3. Expected: five different colours; the previous period dashed; RPE orange, not red; every series named in words; readable on the card in both themes.
4. Delete the preview files.

## 6. By a person's eyes in a real browser (manual, required by constitution VII)

Until someone has done this, the task is not verified visually.

1. Put the preview files back (step 5.1), run `npm run dev`, open `http://localhost:5173/chart-preview.html?theme=light` and `?theme=dark` in a browser, and make the window 375 px wide (DevTools device mode) and then 1280 px.
2. Check: colours as in step 5.3; the tooltip works with the mouse and, by Tab, with the keyboard; nothing overlaps with Polish names (set the browser language to Polish and reload).
3. Delete the preview files.

Tasks #160 and #161 add their own validation steps to this file when they start.
