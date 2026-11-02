# Research: Client checks in and sees readiness

## Rounding of the score

- **Decision**: The score is `round(sum × 100 / 12)`, where `sum = (sleep − 1) + (5 − soreness) + (5 − stress)` and each term is 0 to 4. Rounding is to the nearest whole number.
- **Rationale**: The story defines the score as the mean of three values `(s − 1) / 4`, `(5 − x) / 4`, `(5 − x) / 4`, times 100. That equals `sum / 12 × 100`. The sum is an integer from 0 to 12, so only 13 scores are possible from sums, and none of them falls on a `.5` (it would need `sum × 25 / 3` to end in `.5`, which no integer sum gives). Nearest-whole rounding therefore has no tie to break and every rounding mode gives the same result.
- **Alternatives considered**: Floating-point means of three fractions (rejected: needless rounding noise and harder to read); storing the score (rejected: see below).

## Which scores can occur

- **Decision**: The band thresholds are applied to the integer score. Because only 13 scores occur from check-ins (0, 8, 17, 25, 33, 42, 50, 58, 67, 75, 83, 92, 100), the score 45 and 70 are never produced by answers; the nearest are 42 (Recovery day), 50 (Take it easier), 67 (Take it easier) and 75 (Ready).
- **Rationale**: The issue states the bands as ranges on the score, and `Readiness` can hold any score from 0 to 100, so the thresholds are written as stated and tested on the edges (44, 45, 69, 70) as well as on real answers near them.
- **Alternatives considered**: Defining the bands on the sum (rejected: the thresholds are a product setting stated in score points and may change).

## Value object, not stored

- **Decision**: `Readiness` is a record holding the score; the band is derived by a method. The score is not stored: #110 stores the three answers only.
- **Rationale**: A later change of a threshold or of the formula then applies to old check-ins without a migration, and the answers stay the single source of truth.
- **Alternatives considered**: A `Readiness` entity with its own table (rejected: nothing in the spec needs the score without the answers).

## Where the code lives

- **Decision**: A new feature package `com.kinetiq.readiness`, domain only for #111.
- **Rationale**: Readiness is a concept of its own in `CONTEXT.md` and is used by #110, by the later adjustment (#117, #118) and by the trainer overview (#122, #123). Putting it in `account` would tie it to the wrong feature.
- **Alternatives considered**: `common.domain` (rejected: it is not shared kernel yet, and `common` must not depend on features).

## Invalid answers

- **Decision**: The factory refuses an answer outside 1 to 5 with the existing `InvalidValueException` (`validation.failed`). #110's request validation reports the field code; the domain check is the last line of defence.
- **Rationale**: Matches how other value objects refuse bad input and how `GlobalExceptionHandler` turns it into a problem detail.
- **Alternatives considered**: Clamping the answer (rejected: it would hide a client bug).

## Day and lock of a check-in (planning for #110)

- **Decision**: The day of a check-in is the calendar day in the client's time zone (`Account.timeZone`), taken from an injected `Clock`. A check-in is changeable until the client's first set of that day is logged; until the logging model exists (#93), nothing locks it.
- **Rationale**: Spec edge cases and FR-006, FR-007. The lock needs a port to ask "has this client logged a set today?"; #110 can implement it against the logging model when #93 lands, with a fake in tests until then.
- **Alternatives considered**: Server-local day (rejected: a late-night check-in would land on the wrong day).
