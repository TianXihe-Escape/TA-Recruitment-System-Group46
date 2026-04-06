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
- `TA Applicant`: register, log in, edit profile, choose a local CV file, browse jobs, view job details, apply, track application status, and read reviewer notes in `My Applications`
- `Module Organiser`: create and edit jobs, reopen closed jobs, view applicants per job, inspect match score and missing skills, shortlist, accept, reject, add reviewer notes, and safely adjust accepted TAs when reopening a post
- `Admin`: monitor workloads, highlight overload, inspect all jobs, load/reset sample data, view simple rebalance suggestions

## Reliability Improvements
- Stronger validation for TA registration and profile editing, including normalized email input, phone number checks, Chinese-name-friendly validation, and cleaner text handling
- More tolerant skill parsing that supports English and Chinese separators such as `,` `，` `;` `；` and `、`
- Safer MO review workflow so refreshing tables after shortlist, accept, reject, or cancel acceptance does not trigger false errors
- MO status changes now use guarded selection flows:
- changing `OPEN -> CLOSED` requires selecting the TA(s) to accept first
- changing `CLOSED -> OPEN` requires choosing which accepted TA(s) to remove before recruitment can continue
- Reviewer notes are visible to TAs in `My Applications`, and notes are refreshed per selected application to avoid accidental carry-over
- TA demand is shown consistently across MO and TA views as `accepted / required`
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
3. If the project has already been compiled, you can run it directly:

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

6. In the login screen, click `Load Sample Data` for a ready-to-demo dataset.

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

data/
  users.json, profiles.json, jobs.json, applications.json, config.json

docs/
  design, traceability, demo, tests, teamwork split
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
- A job marked `OPEN` can continue recruiting TAs, while a job marked `CLOSED` cannot accept new applications.
- When reopening a closed job, the MO must choose which accepted TA records move back to a reviewable state.
- When closing an open job, the MO must choose which applicants become accepted before the status change is saved.

## Screenshots
- Add login screen screenshot
- Add TA dashboard screenshot
- Add MO management screenshot
- Add Admin dashboard screenshot

## Contribution
- Use feature branches per module or role flow
- Keep pull requests focused on one iteration milestone
