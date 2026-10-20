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
A plan the client cannot see yet; the first one is built automatically from the quiz.

**Week**:
Monday to Sunday in the client's time zone. Plan weeks are numbered from the week the plan was published.

**Session**:
One named training occasion within a plan, such as "Lower body A".
_Avoid_: Workout, training

**Exercise**:
A movement that can be prescribed, such as a back squat.

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

**Missed session**:
A session whose day has passed without the client finishing it.
_Avoid_: Skipped session

**RPE**:
Rate of perceived exertion: how hard a set felt on a 1–10 scale. 1–6 is easy, 7–8 is the usual target, 9–10 is hard.

**PR**:
A client's best logged result for an exercise.
_Avoid_: Record, achievement

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
