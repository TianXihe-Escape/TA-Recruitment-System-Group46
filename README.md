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
- `Admin`: monitor workloads, inspect all jobs, review all applications and MO reviewer notes, view summary cards for open jobs, closed jobs, applications, and accepted TAs, load or reset sample data, create MO accounts, open hiring management, export CSV recruitment reports, and view rebalance suggestions and notifications.

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
3. If Maven is not available, compile and launch directly with:

```text
build-and-run.bat
```

This script finds all `src/main/java` source files, compiles them with `javac -encoding UTF-8` into `target/classes`, then starts `app.Main`.

4. If the project has already been compiled, run:

```bash
java -cp target/classes app.Main
```

5. Or double-click the legacy launcher after compiling:

```text
run.bat
```

6. If you want Maven to compile and launch the app:

```bash
mvn test
mvn exec:java
```

7. In the login screen, click `Load Demo Data` for a ready-to-demo dataset if needed.

## Default Demo Accounts
Click `Load Demo Data` from the login screen, or sign in as Admin and use `Load Demo Data`, to restore the full sample dataset. `Reset Demo Data` clears local JSON data; use `Load Demo Data` afterwards to repopulate it.

Final demo data contains 7 course support jobs, 2 one-off event jobs, 7 MO accounts, 10 fictional TA accounts, sample applications with MO evaluations, notifications/messages, and workload examples. These sample accounts and module data are used only for local coursework demonstration. The system does not send real emails. TA accounts use `@demo.local` because they are fictional users.

Each fictional TA profile points to a local demo CV in the `cv/` folder, such as `cv/alice-chen-cv.docx`, so TA and MO users can click the CV path in the UI and open a real document.

The system separates schedule and workload. Schedule explains when and where the work happens, while workload measures effort. Course support jobs use weekly workload; one-off jobs such as invigilation and demo support use total workload.

In the demo dataset, the workload threshold is set to 10 hours/week. Frank Zhao is accepted for both `EBU6475` (6 hours/week) and `EBU6366` (5 hours/week), so his accepted weekly workload is 11 hours/week. Alice Chen is accepted for EBU6304 final assessment invigilation at 3 hours total, and Jason Wu is accepted for EBU6304 final demo support at 4 hours total. Admin workload monitoring shows weekly workload separately from one-off event workload, and one-off workload does not trigger the weekly overload warning.

Admin users can open `Application Reviews` to view all applications in one place, including TA name, course, job type, period/schedule, location, workload, MO, status, match score, missing skills, and MO reviewer notes. This avoids switching between multiple MO and TA accounts during the demo.

Admin:
- `Admin`: `admin@bupt.edu.cn` / `admin123`

MO:
- `Dr Ling Ma`: `ling.ma@qmul.ac.uk` / `Password123` for `EBU6304`
- `Dr Chao Shu`: `chao.shu@qmul.ac.uk` / `Password123` for `EBU6475`
- `Dr Jin Zhang`: `jin.zhang@qmul.ac.uk` / `Password123` for `EBU6366`
- `Dr Nickos Paltalidis`: `n.paltalidis@qmul.ac.uk` / `Password123` for `EBU5606`
- `Dr Paula Fonseca`: `paula.fonseca@qmul.ac.uk` / `Password123` for `EBU5042`
- `Dr Chao Liu`: `chao.liu@qmul.ac.uk` / `Password123` for `CBU5201`
- `Dr Athen Ma`: `a.ma@qmul.ac.uk` / `Password123` for `EBU6335`

TA:
- `Alice Chen`: `alice.chen@demo.local` / `Password123`
- `Ben Wang`: `ben.wang@demo.local` / `Password123`
- `Chloe Li`: `chloe.li@demo.local` / `Password123`
- `Daniel Zhang`: `daniel.zhang@demo.local` / `Password123`
- `Emma Liu`: `emma.liu@demo.local` / `Password123`
- `Frank Zhao`: `frank.zhao@demo.local` / `Password123`
- `Grace Xu`: `grace.xu@demo.local` / `Password123`
- `Henry Sun`: `henry.sun@demo.local` / `Password123`
- `Ivy Huang`: `ivy.huang@demo.local` / `Password123`
- `Jason Wu`: `jason.wu@demo.local` / `Password123`

Recommended viva flow:
1. Log in as Admin and check all 9 jobs plus the workload monitor.
2. Open `Application Reviews` to inspect all applications, job types, schedules, locations, matching results, missing skills, and MO reviewer notes.
3. Use the workload monitor to show Frank Zhao exceeding the 10 hours/week threshold because he has multiple accepted TA roles.
4. In `Application Reviews`, identify Emma Liu and Jason Wu as missing-skill examples, and Alice Chen / Ben Wang / Grace Xu / Henry Sun / Ivy Huang as strong matches.
5. Log in as Dr Ling Ma and review `EBU6304` applicants, comparing Alice Chen's high match with Emma Liu's missing skills.
6. Log in as Dr Chao Shu and review `EBU6475` applicants, including Ben Wang and Frank Zhao.
7. Log in as Dr Jin Zhang and review `EBU6366` applicants, including Chloe Li and Jason Wu.
8. Log in as Dr Nickos Paltalidis and review `EBU5606` applicants, including Daniel Zhang and Jason Wu.
9. Log in as Dr Paula Fonseca, Dr Chao Liu, and Dr Athen Ma to review Grace Xu, Henry Sun, and Ivy Huang.
10. Log in as Alice Chen to show shortlisted status, reviewer notes, notifications, and messages.
11. Log in as Emma Liu or Jason Wu to demonstrate missing skills.
12. Return to Admin and show workload balancing / warning for Frank Zhao, plus Alice/Jason one-off event workload.

Future work could expand the lightweight schedule conflict check into calendar-based workload planning.

Sample course set:
- `EBU6304`: Software Engineering - 2025/26, Module Organiser Dr Ling Ma; teaching team includes Dr Gokop Goteng, Dr Riasat Islam, Dr Salman Haleem, and Dr Alan Wong.
- `EBU6475`: Microprocessor Systems Design - 2025/26, Module Organiser Dr Chao Shu.
- `EBU6366`: Microwave, Millimeterwave and Optical Transmission - 2025/26, Module Organiser Dr Jin Zhang; related staff Dr Fatma Benkhelifa.
- `EBU5606`: Product Development and Marketing - 2025/26, Module Organiser Dr Nickos Paltalidis.
- `EBU5042`: Advanced Network Programming - 2025/26, Module Organiser Dr Paula Fonseca; related staff Prof Gareth Tyson.
- `CBU5201`: Machine Learning - 2025/26, Module Organiser Dr Chao Liu.
- `EBU6335`: Digital Systems Design - 2025/26, Module Organiser Dr Athen Ma.

Manual demo checklist:
- Admin: log in, check all jobs, workload monitor, Application Reviews, notifications, CSV export, and sample-data reset/load.
- Dr Ling Ma: log in, load `EBU6304` applicants, review Alice Chen and Emma Liu, and edit reviewer notes.
- Dr Chao Shu: log in, load `EBU6475` applicants, and review Ben Wang / Frank Zhao.
- Dr Jin Zhang: log in, load `EBU6366` applicants, and review Chloe Li / Frank Zhao.
- Dr Nickos Paltalidis: log in, load `EBU5606` applicants, and review Daniel Zhang / Jason Wu.
- Dr Paula Fonseca: log in, load `EBU5042` applicants, and review Grace Xu.
- Dr Chao Liu: log in, load `CBU5201` applicants, and review Henry Sun / Emma Liu.
- Dr Athen Ma: log in, load `EBU6335` applicants, and review Ivy Huang.
- Alice Chen: log in, check shortlisted status, reviewer notes, notifications, and messages.
- Emma Liu: log in, check rejected `EBU6304` application and missing skills.
- Frank Zhao: log in, check accepted applications and workload-related notification.
- Jason Wu: log in, compare EBU5606 support notes with EBU6366 missing RF skills.

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
- Build without Maven and launch: `build-and-run.bat`
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
- Admin users can use `Application Reviews` to audit all applications and MO reviewer notes from one read-only table.

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
