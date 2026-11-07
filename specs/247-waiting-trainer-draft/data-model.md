# Data Model: Client sees their trainer and a draft summary while waiting for a plan

Nothing is stored and no table changes, so there is no migration and no new demo data. Everything below is derived.

## Draft summary

What a client may read of their own draft (spec 79). Built from the `Plan` aggregate on each read.

| Field | From | Notes |
|---|---|---|
| `trainingDays` | the day of each session | At least one, at most 6, in week order, Monday first; `Plan` keeps its sessions in that order |
| `weeks` | the plan's weeks | 4-16 |
| `goal` | the plan's goal | The draft's own goal, which a trainer may have changed from the quiz's |

It exists only for a plan with no first week and at least one session:

| The client's plan | Summary |
|---|---|
| Built automatically, with sessions | yes |
| Saved by a trainer, with sessions, including a failed draft that was filled | yes |
| Failed draft, empty | no |
| Draft without sessions | no |
| No plan, drafting included | no |
| Published | no |

## Trainer on the waiting state

| Value | From | Notes |
|---|---|---|
| Full name | `name` of the client's conversation (spec 58) | Gives the avatar's initials |
| First name | the first word of the full name | Shown in the text |

No conversation, or a failed read, means no name: the text reads "your trainer", without an avatar.
