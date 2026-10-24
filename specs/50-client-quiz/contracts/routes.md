# Contract: quiz routes and stored draft

The story adds no API endpoint. What other tasks rely on is listed here: the quiz's addresses (task #53 sends clients to them, task #56 adds the review step after them) and the stored draft (tasks #56 and #57 read and remove it).

## Addresses

| Address | Shows |
|---|---|
| `/app/quiz` | The step the client may open (data-model.md, "Which step may open"). Replaces the address. |
| `/app/quiz/1` … `/app/quiz/7` | That step, when it may open. Otherwise the step that may, replacing the address. |
| `/app/quiz/<anything else>` | Same as `/app/quiz`, until task #56 adds `/app/quiz/review`. |

- No tab bar on any quiz address.
- Continue pushes the next address, so the browser's back returns to the previous step.
- Entry routing (task #53) sends a client without a submitted quiz to `/app/quiz`.

## Stored draft

- Key: `kinetiq.quiz` in `sessionStorage`.
- Shape and rules: data-model.md, "Quiz draft".
- Only the quiz feature reads or writes it. Task #57 removes it after a successful submit.

## Needed from story #54 for task #53

Task #53 needs one authenticated read that tells, for the signed-in client, whether the quiz was submitted and whether a plan is published. Until plans exist, "published" is always false. The exact endpoint and fields, and whether task #55 or #57 builds it, are defined in the spec of story #54; the types come into `schema.d.ts` with that task.
