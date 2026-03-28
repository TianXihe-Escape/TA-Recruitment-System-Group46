# Functional And Manual Test Cases

| Req ID | Feature | Test Case ID | Input / Action | Expected Result | Actual Result |
|---|---|---|---|---|---|
| R1 | Register TA | TC-REG-01 | Enter valid email and matching password | Account created and TA can log in | TBD |
| R1 | Register Validation | TC-REG-02 | Empty email | Error shown | TBD |
| R1 | Register Validation | TC-REG-03 | Invalid email | Error shown | TBD |
| R2 | Login TA | TC-LOG-01 | Valid TA account and role | TA dashboard opens | TBD |
| R2 | Login Validation | TC-LOG-02 | Wrong password | Error shown | TBD |
| R3 | Edit Profile | TC-PRO-01 | Update profile and save | Profile saved to JSON | TBD |
| R3 | Profile Validation | TC-PRO-02 | Empty name | Error shown | TBD |
| R4 | CV Path | TC-CV-01 | Save CV path | Path stored in JSON | TBD |
| R4 | CV Path Validation | TC-CV-02 | Apply without CV path | Error shown | TBD |
| R5 | Browse Jobs | TC-JOB-01 | Open job tab | Jobs listed | TBD |
| R6 | Apply Job | TC-APP-01 | Apply to open job | Application saved with `SUBMITTED` | TBD |
| R6 | Duplicate Apply | TC-APP-02 | Apply twice to same job | Duplicate rejected | TBD |
| R6 | Closed Job | TC-APP-03 | Apply to closed job | Error shown | TBD |
| R7 | MO Post Job | TC-MO-01 | Fill form and save | Job appears in table | TBD |
| R8 | MO Review Applicants | TC-MO-02 | Select job and load applicants | Applicants displayed | TBD |
| R8 | MO Accept Applicant | TC-MO-03 | Accept applicant | Status becomes `ACCEPTED`, job closes | TBD |
| R8 | MO Reject Applicant | TC-MO-04 | Reject applicant | Status becomes `REJECTED` | TBD |
| R9 | TA Status Tracking | TC-STA-01 | TA logs in after review | Updated status visible | TBD |
| R10 | Admin Workload | TC-ADM-01 | Open admin dashboard | Workload totals shown | TBD |
| R10 | Overload Flag | TC-ADM-02 | Accepted hours exceed threshold | Flag shows `YES` and row highlights | TBD |
| R11 | Skill Match Score | TC-AI-01 | Match 2 of 3 skills | Score `67%`, missing skill shown | TBD |

## Automated Unit Tests
- `MatchingServiceTest`
- `WorkloadServiceTest`
- `ValidationServiceTest`
- `ApplicationServiceTest`
- `JsonDataStoreTest`
