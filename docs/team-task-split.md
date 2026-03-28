# Recommended Team Task Split For 6 Members

## Branch Ownership
- Member A: authentication and registration
  - Branch: `feature/auth-registration`
- Member B: TA dashboard and applicant profile
  - Branch: `feature/ta-dashboard-profile`
- Member C: MO job posting and job repository work
  - Branch: `feature/mo-job-management`
- Member D: application workflow
  - Branch: `feature/application-workflow`
- Member E: admin workload and matching logic
  - Branch: `feature/admin-matching-workload`
- Member F: testing, integration, and documentation
  - Branch: `feature/testing-docs-integration`

## Independent Modules
- `repository` can stabilize first to reduce conflicts
- Each `service` class is feature-scoped
- Each main UI frame is role-specific
- Tests and docs can proceed in parallel once interfaces are stable

## Suggested Commit Granularity
- `chore: scaffold Maven project and package structure`
- `feat: add JSON repositories and data initialization`
- `feat: implement TA registration and login`
- `feat: implement applicant profile management`
- `feat: implement job posting management for MO`
- `feat: implement application workflow and review states`
- `feat: add workload monitor and explainable matching`
- `test: add service unit tests`
- `docs: add design, traceability, test cases, and demo script`

## Example Issue Titles
- `Set up Maven project and JSON persistence`
- `Implement TA login and registration flow`
- `Build applicant profile editor and CV path storage`
- `Build MO job management screen`
- `Implement application review and status workflow`
- `Add admin workload monitor and overload warning`
- `Write unit tests for matching and workload services`
- `Prepare README and intermediate viva demo docs`

## Example Pull Request Titles
- `PR1: Scaffold standalone Swing architecture and JSON data store`
- `PR2: Add TA registration, login, and profile management`
- `PR3: Add MO job posting and applicant review workflow`
- `PR4: Add admin workload dashboard and explainable matching`
- `PR5: Add automated tests and assessment documentation`
