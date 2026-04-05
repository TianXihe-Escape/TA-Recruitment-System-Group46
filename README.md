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
- `TA Applicant`: register, log in, edit profile, store CV path, browse jobs, view job details, apply, track application status
- `Module Organiser`: create and edit jobs, view applicants per job, inspect match score and missing skills, shortlist, accept, reject
- `Admin`: monitor workloads, highlight overload, inspect all jobs, load/reset sample data, view simple rebalance suggestions

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
3. Run:

```bash
java -cp target/classes app.Main
```

4. In the login screen, click `Load Sample Data` for a ready-to-demo dataset.

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
- Manual launch: `java -cp target/classes app.Main`
- Automated tests require Maven: `mvn test`

## Screenshots
- Add login screen screenshot
- Add TA dashboard screenshot
- Add MO management screenshot
- Add Admin dashboard screenshot

## Contribution
- Use feature branches per module or role flow
- Keep pull requests focused on one iteration milestone
