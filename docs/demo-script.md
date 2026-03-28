# Intermediate Viva Demo Script

## Speaker 1: Project Overview
- Open the login screen.
- Explain that this is a stand-alone Java Swing application with JSON persistence and no database.
- Mention the layered architecture: model, repository, service, UI, util.

## Speaker 2: TA Flow
- Click `Load Sample Data`.
- Log in as `ta1@bupt.edu.cn`.
- Show profile editing, CV path storage, and available jobs.
- View a job detail dialog.
- Apply to an open job and show the application status table.
- Attempt a duplicate application to show validation.

## Speaker 3: MO Flow
- Log in as `mo1@bupt.edu.cn`.
- Show job creation/editing.
- Select a job, load applicants, and show the match score and missing skills.
- Add reviewer notes and accept or reject an applicant.

## Speaker 4: Admin Flow
- Log in as `admin@bupt.edu.cn`.
- Show workload totals and overload highlighting.
- Click `Rebalance Suggestion` to show rule-based recommendations for open jobs.

## Speaker 5: Engineering Explanation
- Show the JSON files in `data/`.
- Explain that all persistence is transparent and inspectable.
- Mention automated tests for matching, workload, validation, duplicate prevention, and JSON storage.

## Speaker 6: Teamwork Explanation
- Open `docs/team-task-split.md`.
- Explain how separate UI frames, services, repositories, and tests support parallel branch work.
