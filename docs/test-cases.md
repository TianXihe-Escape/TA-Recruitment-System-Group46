# Functional And Manual Test Cases

| Req ID | Feature | Test Case ID | Input / Action | Expected Result | Actual Result |
|---|---|---|---|---|---|
| US01 | Register TA | TC-REG-01 | Enter valid email/name/password | Account and profile created | Pass |
| US01 | Register validation | TC-REG-02 | Invalid email/name or duplicate email | Error shown, no record saved | Pass |
| US02 | Login | TC-LOG-01 | Valid role credentials | Correct dashboard opens | Pass |
| US02 | Login validation | TC-LOG-02 | Wrong password or role | Error shown | Pass |
| US03 | Reset password | TC-RST-01 | Existing email and matching new password | Password updates and login succeeds | Pass |
| US04 | Edit profile | TC-PRO-01 | Save name/email/phone/programme/year/skills | Profile saved to JSON | Pass |
| US05 | Upload documents | TC-DOC-01 | Choose CV/supporting document and save | Files copied to managed folders and paths saved | Pass |
| US06 | Browse jobs | TC-JOB-01 | Open TA dashboard | Open, non-expired jobs listed | Pass |
| US07 | Job details | TC-JOB-02 | Select job and open details | Category, semester, duties, hours, deadline shown | Pass |
| US08 | Search/filter jobs | TC-JOB-03 | Search keyword, module, or category | Matching jobs only are shown | Pass |
| US09 | Apply job | TC-APP-01 | Apply with complete profile | Application saved as `SUBMITTED` | Pass |
| US09 | Duplicate apply | TC-APP-02 | Apply twice to active application | Duplicate rejected | Pass |
| US10 | Withdraw | TC-WDR-01 | Withdraw before deadline | Status becomes `WITHDRAWN` | Pass |
| US10 | Withdraw after deadline | TC-WDR-02 | Withdraw after deadline | Error shown | Pass |
| US11 | Track status | TC-STA-01 | TA opens My Applications | Status, notes, missing skills visible | Pass |
| US12 | Favourite jobs | TC-FAV-01 | Toggle favourite | Favourite Jobs tab updates and persists | Pass |
| US13 | Post vacancy | TC-MO-01 | MO saves valid vacancy | Job appears in tables | Pass |
| US14 | Edit/close vacancy | TC-MO-02 | Update fields or close/open job | Saved with guarded workflow | Pass |
| US15 | Selection criteria | TC-MO-03 | Enter required skills | Criteria saved and shown in review | Pass |
| US16 | Review applicants | TC-MO-04 | Load applicants for job | Linked applicants listed | Pass |
| US17 | Review profile/CV | TC-MO-05 | Click CV/supporting document line | Correct file opens or clear error shown | Pass |
| US18 | Shortlist/remove shortlist | TC-MO-06 | Shortlist then remove shortlist | Status moves `SUBMITTED` / `SHORTLISTED` correctly | Pass |
| US19 | Hiring decision | TC-MO-07 | Accept/reject applicant | Decision timestamp/history updated | Pass |
| US19 | Interview invitation | TC-MO-09 | Invite shortlisted applicant to interview | Status becomes `INTERVIEW_INVITED` and notification is sent | Pass |
| US20 | Allocation | TC-ADM-01 | Accept applicant | Active allocation record created | Pass |
| US21 | Workload dashboard | TC-ADM-02 | Open admin dashboard | Total accepted hours shown | Pass |
| US22 | Workload warning | TC-MO-08 | Accept overloaded TA | Confirmation warning shown before accepting | Pass |
| US23 | Notifications | TC-NOT-01 | Submit/status change/export | Notifications created and visible | Pass |
| US24 | Export CSV | TC-EXP-01 | Admin clicks Export CSV | CSV report written to `exports/` | Pass |
| US25 | Skill matching | TC-AI-01 | Applicant matches some required skills | Score and matched/missing skills shown | Pass |
| US26 | Missing-skill suggestion | TC-AI-02 | TA views application details | Missing skills converted to improvement suggestion | Pass |
| US27 | Role permissions | TC-SEC-01 | TA/MO/Admin login | Only role dashboard/actions are accessible | Pass |
| US28 | JSON storage | TC-DAT-01 | Save/read entities | JSON files persist and reload data | Pass |
| US29 | TA/MO messages | TC-MSG-01 | TA sends message and MO replies | `messages.json` stores both directions with read tracking | Pass |

## Automated Unit Tests
- `ApplicationServiceTest`
- `AuthServiceTest`
- `JsonDataStoreTest`
- `JobServiceTest`
- `MatchingServiceTest`
- `NotificationServiceTest`
- `MessageServiceTest`
- `ValidationServiceTest`
- `WorkloadServiceTest`

Latest local JUnit run: 46 tests found, 46 successful, 0 failed.
