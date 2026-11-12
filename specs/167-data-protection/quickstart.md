# Quickstart: Personal and health data are protected

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/` (JDK 25). Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- Outgoing mail is logged by the backend, so invite and confirmation links are read from its log.

## 1. Inventory and retention (#168)

1. Read `docs/adr/0005-data-inventory-and-encryption.md`: every table of [data-model.md](data-model.md) appears in its inventory, with purpose, lawful basis, recipients and retention, and the encryption decision has its reason.
2. Run `./gradlew test --tests '*RetentionIT'`: an unconfirmed trainer whose link expired more than 6 days ago is erased and one at 5 days is kept; an unconfirmed client is kept; invites ended 61 minutes ago are erased and 59 minutes ago are kept.

## 2. Privacy notice (#169)

1. Signed out, open http://localhost:5173/sign-up: under "Create account" reads "How we handle your data: Privacy notice". Type a name, choose the link: the notice opens in a new tab; the name is still in the form.
2. Open an invite link from the backend log: the join form's "Privacy notice" opens the notice, not the not-found page (bug #242).
3. On the notice, check every section of spec FR-004, the `privacy@kinetiq.page` link and the last-updated date. Switch the device to Polish (or sign in as a Polish account): the whole notice is Polish.
4. Check 375 px and 1280 px, light and dark, headings in order, focus visible on the links.

## 3. Deleting an account, API (#170)

1. Run `./gradlew test --tests '*AccountErasureIT'`: a client with a row in every table is erased and no column of any table holds their id, email or name; the control client and trainer are unchanged.
2. Signed in as `demo.trainer@example.com`, `GET /api/v1/accounts/me/deletion` answers `clients` > 0; `POST` with the right password answers `409 account.has-clients` and nothing changes.
3. `POST` with a wrong password five times: `400 account.password-incorrect` each time; the sixth try, even with the right password, answers `429 account.sign-in-locked`, as the sign-in page does.

## 4. Deleting an account, UI (#171)

1. Sign in as `demo.client.other@example.com` on two browsers. In one, open Settings from the app bar: below "Sign out" is "Delete account" and a "Privacy notice" link.
2. Choose "Delete account": the dialog says what is erased, names the trainer, mentions sets saved on this phone, asks for the password; focus is on Cancel. Cancel: focus returns to "Delete account".
3. Open it again, enter a wrong password: "That password is incorrect." on the field, the dialog stays. Enter the right one: the sign-in page shows "Your account was deleted.". The other browser's next action leads to sign-in; signing in as that client fails as for an unknown email.
4. Sign in as their trainer `demo.trainer2@example.com`: the client is gone from Clients, Approvals and Messages. Settings shows "You have {n} clients. You can delete your account once none are left." and no delete button.
5. Sign up a new trainer, confirm it from the backend log, add an exercise and an invite, open Settings: "Delete account" erases it after the password; the invite's links show "This link no longer works".
6. Check the trainer section at 1280 px and the client sheet and dialog at 375 px, light and dark, English and Polish, keyboard only, and an empty console.
