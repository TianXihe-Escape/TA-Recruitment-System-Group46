# Intermediate Viva Demo Script

## Speaker 1: Project Overview
- Open the login screen.
- Explain that this is a stand-alone Java Swing application with JSON persistence and no database.
- Mention the layered architecture: model, repository, service, UI, util.

## Speaker 2: TA Flow
- Click `Load Demo Data`.
- Log in as `alice.chen@demo.local`.
- Show profile editing, CV path storage, multiple PDF supporting documents, and available jobs.
- View a job detail dialog and point out job type, period, schedule, location, and weekly/total workload.
- Open `My Applications` to show accepted one-off invigilation workload, match score, missing skills, and MO reviewer notes.
- Log in as `emma.liu@demo.local` if time allows to show the EBU6304 missing-skills case.
- Log in as `jason.wu@demo.local` if time allows to show mixed results across EBU5606 and EBU6366.

## Speaker 3: MO Flow
- Log in as `ling.ma@qmul.ac.uk`.
- Show job creation/editing, including job type, start/end date, schedule, location, and workload type.
- Select a job, load applicants, and show the match score and missing skills.
- Click the supporting documents line to show that reviewers can choose from the applicant's award certificate, competition proof, and additional evidence PDFs.
- Add reviewer notes and accept or reject an applicant.
- Repeat briefly with:
- `chao.shu@qmul.ac.uk` for EBU6475 applicants Ben Wang and Frank Zhao.
- `jin.zhang@qmul.ac.uk` for EBU6366 applicants Chloe Li, Frank Zhao, and Jason Wu.
- `n.paltalidis@qmul.ac.uk` for EBU5606 applicants Daniel Zhang and Jason Wu.
- `paula.fonseca@qmul.ac.uk` for EBU5042 applicant Grace Xu.
- `chao.liu@qmul.ac.uk` for CBU5201 applicants Henry Sun and Emma Liu.
- `a.ma@qmul.ac.uk` for EBU6335 applicant Ivy Huang.

## Speaker 4: Admin Flow
- Log in as `admin@bupt.edu.cn`.
- Show that the final demo dataset has 7 course support jobs, 2 one-off event jobs, 7 MO accounts, and 10 fictional TA accounts.
- Show weekly workload, one-off workload, threshold, and warning columns in the workload monitor.
- Explain that the demo workload threshold is 10 hours/week.
- Highlight Frank Zhao's accepted workload warning: he is accepted for EBU6475 at 6 hours/week and EBU6366 at 5 hours/week, for a total of 11 hours/week.
- Explain that Alice Chen's 3 hour invigilation and Jason Wu's 4 hour demo support are displayed as total event workload and are not mixed into the weekly threshold.
- Open `Application Reviews` to show all applications, TA names, courses, job types, schedules, locations, workload, MOs, statuses, match scores, missing skills, and MO reviewer notes.
- Use Emma Liu and Jason Wu as missing-skill examples, and Alice Chen, Ben Wang, Grace Xu, Henry Sun, and Ivy Huang as strong-match examples.
- Click `Rebalance Suggestion` to show rule-based recommendations for open jobs.

## Speaker 5: Engineering Explanation
- Show the JSON files in `data/`.
- Explain that all persistence is transparent and inspectable.
- Explain that schedule tells users when and where work happens, while workload is split into `WEEKLY` course support and `TOTAL` one-off event effort.
- Mention automated tests for matching, workload, validation, duplicate prevention, JSON storage, and sample-data integrity.
- Mention that automatic calendar-based schedule conflict detection is future work beyond the current lightweight string-based check.

## Speaker 6: Teamwork Explanation
- Open `docs/team-task-split.md`.
- Explain how separate UI frames, services, repositories, and tests support parallel branch work.
