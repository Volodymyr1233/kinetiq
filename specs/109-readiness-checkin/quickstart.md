# Quickstart: validating task #111 (readiness score and band)

Run in `backend/` with Docker running.

## 1. The calculation and its bands

```bash
./gradlew test --tests '*ReadinessTest'
```

Expected: all tests pass. They cover:

- the issue's examples (sleep 2, soreness 4, stress 3 gives 33; sleep 4, soreness 2, stress 1 gives 83) and both extremes (0 and 100);
- every one of the 125 combinations of answers: a score from 0 to 100, a better answer never lowers it;
- the band edges on scores (44, 45, 69, 70) and on real answers near them (42, 50, 58, 67, 75);
- answers outside 1 to 5 and scores outside 0 to 100 refused.

## 2. The architecture rules

```bash
./gradlew test --tests '*HexagonalArchitectureTest'
```

Expected: passes; the `readiness` domain depends on nothing but the JDK and `common`.

## 3. The whole quality gate

```bash
./gradlew check
```

Expected: `BUILD SUCCESSFUL`.

## 4. A test that bites

Break the formula on purpose (for example use `soreness − 1` for the soreness term, or change `>=` to `>` at 70) and run step 1: the tests MUST fail. Restore the code and they pass again.

Tasks #110, #112 and #113 get their own validation steps in this file when they start.
