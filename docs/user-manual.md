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

If Maven is not available and `target/classes` does not exist yet, use:

```text
build-and-run.bat
```

If you want Maven to verify the project first:

```bash
mvn test
```

## Demo Accounts
- `Admin`: `admin@bupt.edu.cn` / `admin123`
- `MO EBU6304`: `ling.ma@qmul.ac.uk` / `Password123`
- `MO EBU6475`: `chao.shu@qmul.ac.uk` / `Password123`
- `MO EBU6366`: `jin.zhang@qmul.ac.uk` / `Password123`
- `MO EBU5606`: `n.paltalidis@qmul.ac.uk` / `Password123`
- `MO EBU5042`: `paula.fonseca@qmul.ac.uk` / `Password123`
- `MO CBU5201`: `chao.liu@qmul.ac.uk` / `Password123`
- `MO EBU6335`: `a.ma@qmul.ac.uk` / `Password123`
- `TA Alice`: `alice.chen@demo.local` / `Password123`
- `TA Ben`: `ben.wang@demo.local` / `Password123`
- `TA Chloe`: `chloe.li@demo.local` / `Password123`
- `TA Daniel`: `daniel.zhang@demo.local` / `Password123`
- `TA Emma`: `emma.liu@demo.local` / `Password123`
- `TA Frank`: `frank.zhao@demo.local` / `Password123`
- `TA Grace`: `grace.xu@demo.local` / `Password123`
- `TA Henry`: `henry.sun@demo.local` / `Password123`
- `TA Ivy`: `ivy.huang@demo.local` / `Password123`
- `TA Jason`: `jason.wu@demo.local` / `Password123`

All sample accounts are local coursework-demo accounts only. The application does not send real email.

The final demo dataset contains 7 course support jobs, 2 one-off event jobs, 7 MO accounts, 10 fictional TA accounts, sample applications with reviewer notes, notifications/messages, and workload examples. The demo workload threshold is 10 hours/week; Frank Zhao is intentionally accepted for EBU6475 and EBU6366 so his 11 hours/week accepted weekly workload demonstrates Admin workload balancing. Alice Chen's 3 h total invigilation and Jason Wu's 4 h total demo support are displayed separately as event workload.

## Demo Course Set
- `EBU6304`: Software Engineering - 2025/26, Module Organiser Dr Ling Ma.
- `EBU6475`: Microprocessor Systems Design - 2025/26, Module Organiser Dr Chao Shu.
- `EBU6366`: Microwave, Millimeterwave and Optical Transmission - 2025/26, Module Organiser Dr Jin Zhang.
- `EBU5606`: Product Development and Marketing - 2025/26, Module Organiser Dr Nickos Paltalidis.
- `EBU5042`: Advanced Network Programming - 2025/26, Module Organiser Dr Paula Fonseca.
- `CBU5201`: Machine Learning - 2025/26, Module Organiser Dr Chao Liu.
- `EBU6335`: Digital Systems Design - 2025/26, Module Organiser Dr Athen Ma.

## Main Screens

### Login Screen
Use this screen to sign in, load sample data, or create a TA account.

![Login screen](screenshots/login-frame.png)

### TA Dashboard
The TA dashboard is used to maintain the applicant profile, choose a CV and supporting document, search and filter open jobs, save favourite jobs, apply, withdraw eligible applications, exchange messages with MOs, view notifications, and track application results.

![TA dashboard](screenshots/ta-dashboard.png)

### MO Dashboard
The MO dashboard is used to manage jobs, review applicants, write reviewer notes, invite interviews, answer applicant messages, sort and filter applicants, and change job status through guarded selection workflows.

![MO dashboard](screenshots/mo-dashboard.png)

### Admin Dashboard
The Admin dashboard is used to monitor TA workloads, inspect jobs, view all application reviews, view summary cards, reset demo data, generate rebalance suggestions, open the full hiring-management console, and create MO accounts with managed modules.

![Admin dashboard](screenshots/admin-dashboard.png)

## TA Applicant Workflow
1. Log in as a TA user.
2. Complete the profile form on the left side.
3. Click `Choose File` to select a CV and optionally select a supporting document such as a transcript.
4. Click `Save Profile`.
5. In `Available Jobs`, use the search, module, and category filters to find relevant opportunities.
6. Use `Toggle Favourite` to save a job to the `Favourite Jobs` tab if needed.
7. Click `View Job Details` if needed.
8. Click `Apply` to submit the application.
9. Open `My Applications` to track:
- status
- match score
- missing skills
- reviewer notes
10. Select any application and click `View Application Details` to open a summary popup with:
- status
- reviewer notes
- match score
- missing skills
- missing-skill suggestions
- TA demand
- deadline
11. If an application is still under review and before the deadline, select it and click `Withdraw Application`.
12. Click `Message MO` from a selected job or application to ask the responsible organiser a question.
13. Open `Messages` to review replies and send follow-up responses.
14. Open `Notifications` to read system messages about submissions and review decisions.

### TA Notes
- Only `OPEN` jobs are shown in `Available Jobs`.
- `TA Demand` is shown as `accepted / required`.
- Withdrawn applications are marked as `WITHDRAWN` instead of being deleted.
- A withdrawn or rejected application can be submitted again later for the same job if the deadline has not passed.
- Empty tables show a helper message instead of a blank area.

## MO Workflow
1. Log in as an MO user.
2. Select an existing job or create a new one.
3. Fill in module code, title, category, semester, hours, TA needed, skills, deadline, status, and duties.
4. Click `Save Job`.
5. Select a job in the right table and click `Load Applicants`.
6. Select an applicant to review:
- match details
- missing skills
- applicant summary
- CV and supporting document paths, which can be clicked to open the files
- reviewer notes
7. Use the applicant status filter to narrow the list to `Submitted`, `Shortlisted`, `Interview Invited`, `Accepted`, `Rejected`, or `Withdrawn` records if needed.
8. Use the sort box to rank applicants by:
- match score high to low
- match score low to high
- applicant name
- status
9. Use `Shortlist`, `Remove Shortlist`, `Invite Interview`, `Accept`, `Reject`, or `Cancel Acceptance` as needed.
10. Confirmation dialogs are shown before high-impact review actions. Accepting a TA who would exceed the workload threshold shows an additional warning.
11. Use `View Messages` to read TA questions and reply.
12. Use `View Notifications` to read application and review updates.

### Job Status Rules
- Changing `OPEN -> CLOSED` requires the MO to choose the TA(s) who will be accepted before the change is saved.
- Changing `CLOSED -> OPEN` requires the MO to choose which accepted TA(s) will be removed before recruitment can continue.
- `TA Demand` is shown as `accepted / required`.
- Status cells use color badges to make `OPEN`, `CLOSED`, and application states easier to scan.
- Deadline cells are highlighted when a deadline is very close or already overdue.
- If there are no applicants yet, or if the current filter returns no rows, the applicant table shows a matching helper message.

## Admin Workflow
1. Log in as an Admin user.
2. Use the summary cards at the top of the workload view to monitor:
- open jobs
- closed jobs
- total applications
- accepted TAs
3. Use `Refresh` to reload current workload data.
4. Open `Application Reviews` to view all applications with TA name, course, job type, period/schedule, location, workload, MO, status, match score, missing skills, and MO reviewer notes.
5. Use `Open Hiring Management` to access all MO job-posting and applicant-review functions across all known modules.
6. Use `Create MO Account` to add a new MO login and assign one or more module codes.
7. Use `Load Demo Data` to repopulate the sample dataset.
8. Use `Reset Demo Data` to clear and reset the dataset.
9. Use `Rebalance Suggestion` to view simple recommendations for open jobs.
10. Use `Export CSV` to write a recruitment report to the `exports/` folder.
11. Use `View Notifications` to review admin notifications.
12. A confirmation dialog is shown before resetting demo data.

### Admin Application Reviews
- The table is read-only.
- It includes job type, period/schedule, location, and workload so Admin can distinguish weekly course support from one-off invigilation or demo support.
- Empty reviewer notes show as `Not yet reviewed`.
- Empty missing skills show as `None`.
- The detail panel below the table shows the selected application's full reviewer notes, so long MO comments remain readable.
- This overview helps Admin identify Emma Liu and Jason Wu as missing-skill examples and Alice Chen, Ben Wang, Grace Xu, Henry Sun, and Ivy Huang as strong matches without switching accounts.

### Job Schedule And Workload
- Job schedule explains when and where the work happens.
- Course support jobs use `WEEKLY` workload, shown as values such as `6 h/week`.
- One-off jobs such as invigilation and demo support use `TOTAL` workload, shown as values such as `3 h total`.
- The Admin workload monitor calculates accepted weekly jobs separately from accepted one-off jobs. Weekly workload is compared with the threshold; one-off workload is displayed as event workload and is not mixed with weekly threshold calculation.
- The demo threshold is 10 h/week. Frank Zhao exceeds it because `EBU6475` is 6 h/week and `EBU6366` is 5 h/week, totalling 11 h/week. Alice Chen has one-off EBU6304 invigilation at 3 h total, which is displayed separately and does not trigger weekly overload.
- Future work could expand the lightweight schedule conflict check into calendar-based workload planning.

## Data Files
The application stores all data in:
- `data/users.json`
- `data/profiles.json`
- `data/jobs.json`
- `data/applications.json`
- `data/notifications.json`
- `data/messages.json`
- `data/allocations.json`
- `data/config.json`

## Common Notes
- TA users must save a CV path before applying.
- Closed jobs cannot accept new applications.
- Reviewer notes entered by MO users are shown to TA users in `My Applications`.
- Automated tests can be run with `mvn test`.
