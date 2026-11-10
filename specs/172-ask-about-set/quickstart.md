# Quickstart: Client asks about a logged set

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.client.daily@example.com` has finished, locked sessions and a session on Monday to Saturday; their trainer is `demo.trainer@example.com`. The demo data holds one message from this client with a reference ([data-model.md](data-model.md)).

## 1. The reference (#173)

1. Sign in as `demo.client.daily` and open Messages: the demo message is there. In the developer tools, the answer of `GET /api/v1/conversations/{clientId}/messages` holds its `about` ([contracts/api.md](contracts/api.md)).
2. Send a message with the `aboutSetLogId` of a set log of another client, and one as the trainer with any set log id: both answer `404` with `"code": "message.about-not-found"` and store nothing.

## 2. The action (#174)

1. As `demo.client.daily`, open http://localhost:5173/app/session and log a set. When it is Synced, its row offers "Ask your trainer"; a row that is not logged does not.
2. Choose it during a rest: the conversation opens over the logger with the About chip in the composer and the focus in the field. Send is disabled while the field is empty.
3. Type a question and send it: it is Sent. Close the conversation: the same session is there and the rest timer still counts.
4. Open the action again, remove the chip, send: an ordinary message.
5. Cut the network, ask about a set and send: Not sent. Restore the network and choose Retry: Sent once, with its reference.
6. From the Plan tab, open a locked session with "View log": every logged row offers the action and it works; Tab reaches nothing else in the rows.

## 3. The chip (#175)

1. As the client, open Messages: the chip sits above the message, reading "About" and the session, its date, the exercise, the set and the RPE. Choose it: the logger opens on that session with the set's row marked and in view.
2. Within the session's 24 hours, change the set's RPE and open Messages again: the chip shows the new RPE.
3. Sign in as `demo.trainer`, open Messages and the client's conversation: the same chip. Choose it: the client's Logs tab opens with that session chosen and the set's row marked. Reload: the same. Choose another session: the mark is gone.
4. Switch the UI to Polish on both sides: "Dotyczy", "seria", the date and the exercise name follow within 5 seconds.
5. Check light and dark, 375 px and 320 px (client), 1280 px (trainer), keyboard focus and an empty console.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `MessagingServiceTest` proves the rule's cases, `JdbcAboutSetsIT` that only the owner gets a set, `MessageRepositoryImplIT` that a reference is stored and read with the set's current values, `ConversationControllerTest` the field and `404 message.about-not-found`.
