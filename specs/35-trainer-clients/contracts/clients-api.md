# Contract: clients API (#36)

`GET /api/v1/clients`: the signed-in trainer's clients.

| Status | When | Body |
|---|---|---|
| `200` | trainer signed in | `ClientListResponse` |
| `401` | not signed in | problem details, `request.unauthenticated` |
| `403` | signed in as a client | problem details, `request.not-allowed` |

`ClientListResponse`: `{ "clients": [ ClientResponse ] }`, ordered by name without regard to case, then by id; `clients` is never null and is empty when the trainer has no clients.

`ClientResponse`:

| Field | Type | Notes |
|---|---|---|
| `id` | uuid | required |
| `name` | string | required |
| `goal` | `BUILD_MUSCLE` \| `GET_STRONGER` \| `LOSE_FAT` \| `MOVE_BETTER` \| `GENERAL_FITNESS` | left out when the client has not submitted the quiz |
| `planState` | `NO_QUIZ` \| `PLAN_TO_REVIEW` | required; more values with the plan stories |

No query parameters. The frontend types come from the generated OpenAPI document (`npm run gen:api`).
