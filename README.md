<<<<<<< HEAD
# ISTA-Recruitment-System

A lightweight Java-based system developed using Agile methods to streamline the recruitment and management of Teaching Assistants for the BUPT International School.

---

## 📋 Group Name-list

- TianXihe-Escape：231222877（Leader）
- wyx1216：231221434（Member）
- OLITE1：231222040（Member）
- RBKcrazywind：231220530（Member）
- ang666by：231221010（Member）
- wuyanze-zyy：231222291（Member）
=======
# BUPT International School TA Recruitment System

## Project Overview
This repository contains a stand-alone Java desktop application for managing Teaching Assistant recruitment at BUPT International School. The system replaces spreadsheet-based administration with a modular Swing application that supports TA applicants, Module Organisers, and Admin user.

The implementation is aligned with the coursework direction:
- Iteration 1: account creation, login, applicant profile, CV path storage, browse jobs
- Iteration 2: application workflow, MO review actions, admin workload monitor, explainable skill matching
- Iteration 3 ready: richer matching and reporting extensions can be added without changing the persistence approach

## Key Features By Role
- `TA Applicant`: register, log in, edit profile, store CV path, browse jobs, view job details, apply, track application status
- `Module Organiser`: create and edit jobs, view applicants per job, inspect match score and missing skills, shortlist, accept, reject
- `Admin`: monitor workloads, highlight overload, inspect all jobs, load/reset sample data, view simple rebalance suggestions

## Technology Stack
- Java 17
- Maven
- Java Swing
- Jackson for JSON persistence
- JUnit 5 for unit tests

## Coursework Constraints
- Stand-alone Java application
- Desktop UI only
- No database
- Text-file persistence only using JSON
- Explainable AI-like logic only

## How To Run
1. Install Java 17 and Maven.
2. Run:

```bash
mvn clean test
mvn exec:java
```

3. In the login screen, click `Load Sample Data` for a ready-to-demo dataset.

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
- Automated tests: `mvn test`
- Manual test cases: [docs/test-cases.md](/d:/CODE/Test/docs/test-cases.md)

## Screenshots
- Add login screen screenshot
- Add TA dashboard screenshot
- Add MO management screenshot
- Add Admin dashboard screenshot

## Contribution
- Use feature branches per module or role flow
- Keep pull requests focused on one iteration milestone
- See [docs/team-task-split.md](/d:/CODE/Test/docs/team-task-split.md)
>>>>>>> 13bb6c0 (11)
