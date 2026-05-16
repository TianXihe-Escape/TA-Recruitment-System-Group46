# BUPT International School TA Recruitment System

## Project Overview
This repository contains a stand-alone Java desktop application for managing Teaching Assistant recruitment at BUPT International School. The system replaces spreadsheet-based administration with a modular Swing application that supports TA applicants, Module Organisers, and Admin users.

The implementation is aligned with the coursework direction:
- Iteration 1: account creation, login, applicant profile, CV path storage, browse jobs
- Iteration 2: application workflow, MO review actions, admin workload monitor, explainable skill matching
- Iteration 3 ready: richer matching and reporting extensions can be added without changing the persistence approach

## Group Members
- TianXihe-Escape: 31222877 (Leader)
- wyx1216: 31221434 (Member)
- OLITE1: 31222040 (Member)
- RBKcrazywind: 31220530 (Member)
- ang666by: 31221010 (Member)
- wuyanze-zyy: 31222291 (Member)

## Key Features By Role
- `TA Applicant`: register, reset password, log in, edit profile with programme/year, upload a CV and supporting document, search/filter open jobs, save favourites, view job details, apply, withdraw before the deadline, track application status, open application detail popups, read reviewer notes, view missing-skill suggestions, exchange messages with MOs, and read notifications.
- `Module Organiser`: create and edit module, invigilation, or other activity jobs; reopen closed jobs; view applicants per job; inspect match score, missing skills, profiles, CVs, and supporting documents; filter and sort applicants; shortlist, remove shortlist, invite interviews, accept, reject, add reviewer notes, reply to applicant messages, receive notifications, and see workload warnings before accepting overloaded TAs.
- `Admin`: monitor workloads, inspect all jobs, view summary cards for open jobs, closed jobs, applications, and accepted TAs, load or reset sample data, create MO accounts, open hiring management, export CSV recruitment reports, and view rebalance suggestions and notifications.

## Reliability Improvements
- Stronger validation for TA registration and profile editing, including normalized email input, phone number checks, Chinese-name-friendly validation, and cleaner text handling
- More tolerant skill parsing that supports English and Chinese separators such as `,`, `，`, `;`, `；`, and `、`
- Safer MO review workflow so refreshing tables after shortlist, accept, reject, or cancel acceptance does not trigger false errors
- Guarded MO status changes:
- changing `OPEN -> CLOSED` requires selecting the TA(s) to accept first
- changing `CLOSED -> OPEN` requires choosing which accepted TA(s) to remove before recruitment can continue
- Reviewer notes are visible to TAs in `My Applications`, and notes are refreshed per selected application to avoid accidental carry-over
- TA demand is shown consistently across MO and TA views as `accepted / required`
- TA applicants can open an application detail popup to review status, reviewer notes, match score, missing skills, TA demand, and deadline in one place
- TA applicants can withdraw non-finalized applications, and withdrawn records stay in the audit trail while still allowing re-application later
- MO applicants can be filtered by status and sorted by match score, applicant name, or status during review
- Empty-state hints explain whether a table has no data yet or whether the current filter produced no matching rows
- Status badges highlight `SUBMITTED`, `SHORTLISTED`, `INTERVIEW_INVITED`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`, `OPEN`, and `CLOSED` states more clearly in TA and MO tables
- Deadline warnings now highlight near-due and overdue jobs in TA and MO job tables
- Accepted applications create allocation records, and application decisions store update timestamps plus status history.
- In-system notifications are generated for application submission, withdrawal, review decisions, MO account creation, and CSV exports.
- Two-way TA/MO messages are stored in `messages.json`, with unread/read tracking and notification prompts for replies.
- Admin CSV export writes recruitment summary data to the `exports/` directory.
- Confirmation dialogs protect high-impact actions such as accept, reject, cancel acceptance, and reset demo data
- Admin analytics cards give a fast snapshot of open jobs, closed jobs, total applications, and accepted TAs
- UTF-8 data persistence and Chinese-friendly UI fonts to reduce encoding and display issues on Windows
- Faster global scroll behavior, including form-area mouse-wheel scrolling on page containers

## Technology Stack
- Java 17+
- Java Swing
- JSON file persistence
- JUnit 5 for unit tests
- Maven project structure for dependency and build management

## Coursework Constraints
- Stand-alone Java application
- Desktop UI only
- No database
- Text-file persistence only using JSON
- Explainable AI-like logic only

## How To Run
1. Install Java 17 or later.
2. Open a terminal in the project root.
3. If the project has already been compiled, run:

```bash
java -cp target/classes app.Main
```

4. Or double-click:

```text
run.bat
```

5. If you want Maven to compile and launch the app:

```bash
mvn test
mvn exec:java
```

6. In the login screen, click `Load Sample Data` for a ready-to-demo dataset if needed.

## Default Demo Accounts
- `TA`: `ta1@bupt.edu.cn` / `ta123`
- `MO`: `mo1@bupt.edu.cn` / `mo123`
- `Admin`: `admin@bupt.edu.cn` / `admin123`

## Project Structure
```text
src/main/java
  app/           application entry point
  model/         entities and enums
  repository/    JSON-backed persistence
  service/       business logic and validation
  ui/            Swing frames and dialogs
  util/          constants, IDs, JSON helpers, sample data loader

src/test/java/service
  unit and smoke tests

docs/
  design, traceability, demo, tests, team split, user manual, screenshots

data/
  users.json, profiles.json, jobs.json, applications.json, notifications.json, messages.json, allocations.json, config.json
```

## Testing
- Quick launch: `run.bat`
- Manual launch from compiled classes: `java -cp target/classes app.Main`
- Compile and run tests with Maven: `mvn test`
- Launch through Maven: `mvn exec:java`

## Notes
- Applicant and job data are stored in the `data/` folder as JSON files.
- TA applicants must save a profile and choose a CV file before applying.
- `Reviewer Notes` are stored in `applications.json` and shown to TA users in `My Applications`.
- A withdrawn application is marked as `WITHDRAWN` instead of being deleted, so the system keeps a readable history.
- A job marked `OPEN` can continue recruiting TAs, while a job marked `CLOSED` cannot accept new applications.
- When reopening a closed job, the MO must choose which accepted TA records move back to a reviewable state.
- When closing an open job, the MO must choose which applicants become accepted before the status change is saved.
- TA applicants can save favourite jobs and filter jobs by keyword, module, or activity category.
- TA applicants can message the responsible MO from a job or application, and MOs can reply from the review queue.
- Admin users can export CSV reports to the `exports/` folder.

## User Manual
- See [docs/user-manual.md](docs/user-manual.md) for setup, role-based workflows, and screenshots.

## Screenshots
- Login: [docs/screenshots/login-frame.png](docs/screenshots/login-frame.png)
- TA dashboard: [docs/screenshots/ta-dashboard.png](docs/screenshots/ta-dashboard.png)
- MO dashboard: [docs/screenshots/mo-dashboard.png](docs/screenshots/mo-dashboard.png)
- Admin dashboard: [docs/screenshots/admin-dashboard.png](docs/screenshots/admin-dashboard.png)

## Contribution
- Use feature branches per module or role flow
- Keep pull requests focused on one iteration milestone
