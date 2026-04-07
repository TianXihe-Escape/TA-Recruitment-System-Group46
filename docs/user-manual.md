# User Manual

## Overview
This application supports three roles:
- `TA Applicant`
- `Module Organiser (MO)`
- `Admin`

All data is stored locally in JSON files under `data/`. No database is required.

## Setup
1. Install Java 17 or later.
2. Open the project root folder.
3. Start the application with one of the following methods:

```bash
java -cp target/classes app.Main
```

or

```text
run.bat
```

If you want Maven to verify the project first:

```bash
mvn test
```

## Demo Accounts
- `TA`: `ta1@bupt.edu.cn` / `ta123`
- `MO`: `mo1@bupt.edu.cn` / `mo123`
- `Admin`: `admin@bupt.edu.cn` / `admin123`

## Main Screens

### Login Screen
Use this screen to sign in, load sample data, or create a TA account.

![Login screen](screenshots/login-frame.png)

### TA Dashboard
The TA dashboard is used to maintain the applicant profile, choose a CV file, browse open jobs, apply, and track application results.

![TA dashboard](screenshots/ta-dashboard.png)

### MO Dashboard
The MO dashboard is used to manage jobs, review applicants, write reviewer notes, and change job status through guarded selection workflows.

![MO dashboard](screenshots/mo-dashboard.png)

### Admin Dashboard
The Admin dashboard is used to monitor TA workloads, inspect jobs, reset demo data, and generate rebalance suggestions.

![Admin dashboard](screenshots/admin-dashboard.png)

## TA Applicant Workflow
1. Log in as a TA user.
2. Complete the profile form on the left side.
3. Click `Choose File` to select a CV.
4. Click `Save Profile`.
5. In `Available Jobs`, select an open job.
6. Click `View Job Details` if needed.
7. Click `Apply` to submit the application.
8. Open `My Applications` to track:
- status
- match score
- missing skills
- reviewer notes

## MO Workflow
1. Log in as an MO user.
2. Select an existing job or create a new one.
3. Fill in module code, title, hours, TA needed, skills, deadline, status, and duties.
4. Click `Save Job`.
5. Select a job in the right table and click `Load Applicants`.
6. Select an applicant to review:
- match details
- missing skills
- applicant summary
- reviewer notes
7. Use `Shortlist`, `Accept`, `Reject`, or `Cancel Acceptance` as needed.

### Job Status Rules
- Changing `OPEN -> CLOSED` requires the MO to choose the TA(s) who will be accepted before the change is saved.
- Changing `CLOSED -> OPEN` requires the MO to choose which accepted TA(s) will be removed before recruitment can continue.
- `TA Demand` is shown as `accepted / required`.

## Admin Workflow
1. Log in as an Admin user.
2. Use `Refresh` to reload current workload data.
3. Use `Load Demo Data` to repopulate the sample dataset.
4. Use `Reset Demo Data` to clear and reset the dataset.
5. Use `Rebalance Suggestion` to view simple recommendations for open jobs.

## Data Files
The application stores all data in:
- `data/users.json`
- `data/profiles.json`
- `data/jobs.json`
- `data/applications.json`
- `data/config.json`

## Common Notes
- TA users must save a CV path before applying.
- Closed jobs cannot accept new applications.
- Reviewer notes entered by MO users are shown to TA users in `My Applications`.
- Automated tests can be run with `mvn test`.
