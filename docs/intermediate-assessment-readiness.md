# Intermediate Assessment Readiness Report

## Implemented Now
- Stand-alone Java Swing application with role-based login
- TA registration with file-based account persistence
- Applicant profile editing and CV path storage
- Job browsing and job detail viewing
- TA application submission with duplicate-prevention and status tracking
- MO job creation and editing
- MO applicant review with shortlist, accept, and reject actions
- Admin workload overview with overload highlighting
- Explainable skill matching with score, matched skills, and missing skills
- Rule-based rebalance suggestions for open jobs
- Unit tests for validation, matching, workload, application workflow, and JSON storage
- Project documentation for design, traceability, tests, demo, and team split

## Remaining For Later Iterations
- Better UI polish and form layout refinement
- Richer search and filtering for jobs and applicants
- Export/report helpers
- Stronger security such as hashed passwords
- More advanced matching rules and richer missing-skill explanations

## Known Limitations
- Passwords are stored in plain text for coursework simplicity
- CV upload stores a file path only, not the file itself
- There is no concurrency control for simultaneous edits because persistence is local JSON
- Automated verification could not be executed with Maven in this environment because `mvn` is not installed locally

## Suggested Demo Flow
1. Start application and show the login screen
2. Log in as TA and demonstrate profile, jobs, apply flow
3. Log in as MO and demonstrate applicant review and status update
4. Log in as Admin and demonstrate workload overview and suggestions
5. Open JSON files and explain persistence transparency

## Likely Viva Questions And Short Answers
- Why no database?
  - The coursework explicitly forbids database use, so we used JSON files under `data/`.
- Why Swing instead of web?
  - The project must be a stand-alone Java application, and Swing is standard and easy to run locally.
- Where is the AI?
  - The AI-like functions are deterministic structured logic: skill matching and workload balancing rules, both visible in code and easy to explain.
- How do you prevent duplicate applications?
  - `ApplicationService` checks existing applications for the same applicant and job before saving a new one.
- How is workload calculated?
  - Accepted applications are joined to job hours and summed per applicant, then compared with a threshold from `config.json`.
