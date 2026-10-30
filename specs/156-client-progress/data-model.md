# Data Model: Client sees their progress

No table and no API change in #159. The building block has these parts.

## Series meaning (task #159)

One of five values; each has a colour token, a style and a translated name.

| Meaning | Token | Style | Name (English / Polish) |
|---|---|---|---|
| `completion` | `--chart-1` | solid, the primary series | Sessions done / Wykonane sesje |
| `rpe` | `--chart-2` | solid | RPE / RPE |
| `load` | `--chart-3` | solid | Load / Obciążenie |
| `wearables` | `--chart-4` | solid | Heart rate and kcal / Tętno i kcal |
| `previous` | `--chart-5` | dashed, neutral | Previous period / Poprzedni okres |

- A meaning has the same token in every chart, in light and dark.
- Only `previous` is dashed.
- The token is used as `var(--color-<meaning>)` inside a chart, which the chart container fills from the token.
- Constraint: the `rpe` colour is not red: its hue is at least 40 and at least 15 degrees from the destructive hue in both themes (a test).

## Chart (task #159)

| Part | Meaning | Rule |
|---|---|---|
| title | The finding, as a sentence | Required; shown as a heading and used as the figure's name |
| kind | `line` or `bar` | Nothing else is accepted |
| data | Rows of numbers, one per point on the x axis | May be empty: an empty state shows instead of an empty axis |
| x key and its label | Which field is the x axis and its heading in the text version | Both required |
| series | One to three entries of a meaning and the data field they read | At most three; a fourth is refused |
| text version | A visually hidden table with the title as caption, the x column and one column per series named in words | Always present |

## Progress period (planned, #157 to #160)

4, 8 or 12 weeks ending with the current week; each number is compared with the previous week. Built later.
