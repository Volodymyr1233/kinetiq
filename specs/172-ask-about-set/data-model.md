# Data Model: Client asks about a logged set

One column is added. The other tables are those of spec `58-client-trainer-messaging` and spec `92-session-logging`.

## messages (migration `V26__add_message_about.sql`)

| Column | Type | Notes |
|---|---|---|
| `about_set_log_id` | `uuid`, nullable | The set log the message is about. `fk_messages_about_set_log` → `set_logs (id)`. Written once with the message, never updated. |

No index: nothing looks messages up by set, and a set log is never deleted, so the foreign key is never checked from that side.

## About reference

| Rule | Where it is kept |
|---|---|
| At most one per message | one column |
| Only a set log of a session log whose client is the sender | `MessagingService.send`, through `AboutSets` (research R4) |
| Fixed once stored | the row is written by one insert and never updated (`JpaMessageRepository.insertIfAbsent`) |
| A repeat of the same message has the same reference | `Message.isSameSendAs` |

## AboutSet (read, never stored)

What a message shows of its set, read when the message is read:

| Value | From |
|---|---|
| set log id | `set_logs.id` |
| session log id, session name, session date | `session_logs.id`, `session_name`, `log_date` |
| exercise name, English and Polish | `exercises.name`, `name_pl`, through `session_log_entries.exercise_id` |
| entry position, set number | `set_logs.entry_position`, `set_number` |
| RPE | `set_logs.rpe`, as it is now |

## Demo data

`R__demo_session_logs.sql` gains a conversation between Demo Client Daily and their trainer and one message from the client with a reference to one of their logged sets (research R13).
