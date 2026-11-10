# Kinetiq

A training system for two roles: trainers design plans and watch progress across many clients; clients train, log what they did and talk to their trainer.

## Language

### People

**Trainer**:
A professional who designs plans for clients and decides on changes to them.
_Avoid_: Coach, instructor

**Client**:
A person who trains under one trainer's plan.
_Avoid_: Athlete, user, trainee

**Account**:
What a trainer or client signs in with: name, email, password and role. A trainer creates one by signing up; a client gets one through an invite.
_Avoid_: User, profile, login

**Sign-in**:
One browser's signed-in state for one account. It ends when the person signs out or after 14 days without use; an account can have one per device.
_Avoid_: Session, login

**Confirmation link**:
A single-use link sent to an account's email that proves the person controls that address.
_Avoid_: Verification code, activation link

**Invite**:
A single-use link with which a trainer lets a person join as their client.
_Avoid_: Invitation code, referral

**Client status**:
The trainer's at-a-glance verdict on a client: On track, Watch, At risk, Plan to review or No plan.

### Planning

**Plan**:
The training a trainer has prescribed for one client: a set of sessions repeated week by week for a fixed number of weeks.
_Avoid_: Program, routine

**Draft**:
A plan only the trainer can read until it is published; the first one is built automatically from the quiz. The client sees at most its draft summary.

**Draft summary**:
What a client may read of their draft while they wait: its training days, length and focus.

**Failed draft**:
A draft the system could not build automatically: empty, and marked so for the trainer.

**Week**:
Monday to Sunday in the client's time zone. Plan weeks are numbered from the week the plan was published.

**Session**:
One named training occasion within a plan, such as "Lower body A".
_Avoid_: Workout, training

**Exercise**:
A movement that can be prescribed, such as a back squat.

**Exercise entry**:
One exercise in its place within a session, with its targets. The same exercise can appear twice in a session.
_Avoid_: Row, item

**Exercise library**:
The catalogue of exercises a trainer picks from when building a plan: the starter exercises plus the trainer's own.

**Starter exercise**:
An exercise every trainer sees and nobody can edit.
_Avoid_: Default exercise, system exercise

**Category**:
The group an exercise belongs to: Chest, Back, Legs, Shoulders, Arms, Core, Full body, Cardio or Mobility.
_Avoid_: Muscle group

**Measure**:
What an exercise counts: repetitions or seconds.

**Cues**:
Short coaching instructions for doing an exercise well.
_Avoid_: Description, notes

**Set**:
One continuous run of repetitions of an exercise.
_Avoid_: Series

**Target**:
What the plan asks for in a set: load, repetitions or seconds, RPE and rest.
_Avoid_: Prescription, goal

**Publish**:
The trainer's act of making a drafted plan visible to the client.

**Quiz**:
The questions a new client answers, from which their first plan is drafted.
_Avoid_: Survey, questionnaire, onboarding form

### Training

**Log**:
What a client actually did in a set: load, repetitions or seconds, and RPE.
_Avoid_: Track, record, entry

**Pending log**:
A log made on the phone that the server has not confirmed yet. It is kept on the phone, per account, and sent by itself when the app is open and online.
_Avoid_: Offline log, queued log

**Log state**:
Where one logged set stands for the client: **Saved on this phone** (a pending log not yet sent), **Synced** (stored by the server) or **Not synced** (the server refused it, or it could not be sent after 3 tries; it offers Retry and Discard). Derived, never stored.
_Avoid_: Failed, error

**Session state**:
Where one planned session stands: Done, In progress, Missed, Today or Planned. Derived from the calendar and the session log, never stored.

**Session summary**:
The sets logged, total volume and average RPE of a finished session, as its sets are now.

**Missed session**:
A planned session that was never started, or was started and not finished before it locked, whose day has passed and is on or after the day the plan was published.
_Avoid_: Skipped session

**Locked**:
A started session whose first 24 hours have passed: its sets can be read but no longer logged or changed, whether or not it was finished.
_Avoid_: Closed, frozen

**RPE**:
Rate of perceived exertion: how hard a set felt on a 1–10 scale. 1–6 is easy, 7–8 is the usual target, 9–10 is hard.

**PR**:
A client's best logged result for an exercise.
_Avoid_: Record, achievement

**Top set**:
The best set of an exercise in a session or a week: a set with a load before one without, then the highest load, then the most repetitions or seconds.
_Avoid_: Best set, max

**Week streak**:
The weeks in a row in which a client did at least 2 sessions, or every session of a week that had fewer.
_Avoid_: Chain, combo

### Readiness

**Check-in**:
A client's daily answers about sleep, soreness and stress, given before the first session of the day.

**Readiness**:
A 0–100 score derived from a check-in, saying how prepared the client is to train today.
_Avoid_: Recovery score

**Readiness band**:
One of three ranges of readiness: Ready, Take it easier, Recovery day.

**Adjustment**:
A lighter set of targets for today only, applied automatically on a recovery day. The plan itself does not change.
_Avoid_: Scale-down, plan change

### Changing a plan

**Rule**:
A condition over a client's logs that, when met, calls for a plan change.

**Plan change**:
A suggested lasting modification to a plan, with the rule that caused it. It takes effect only when both trainer and client accept it.
_Avoid_: Proposal, suggestion

**Plan change content**:
The proposed changes of a plan change: targets, removed session and moves.
_Avoid_: Draft, which is an unpublished plan

**Effective week**:
The week from which an accepted plan change applies: the one after the plan change was created.

**Expired**:
The end of a plan change that trainer and client did not both accept before its effective week began.

**Approval**:
A decision waiting for the trainer: a plan change to accept or decline, or a drafted plan to publish.

### Conversation

**Message**:
A piece of text sent between a client and their trainer.

**Conversation**:
All messages between one client and their trainer.
_Avoid_: Chat, thread

**Unread**:
A message from the other person received after the reader last opened the conversation.
_Avoid_: New, unseen

**Delivery state**:
What the sender sees on their own message: Sending, Sent or Not sent. There are no read receipts.
_Avoid_: Status, read ticks
