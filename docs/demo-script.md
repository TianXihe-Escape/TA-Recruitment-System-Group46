# Intermediate Viva Demo Script

## Speaker 1: Project Overview
- Open the login screen.
- Explain that this is a stand-alone Java Swing application with JSON persistence and no database.
- Mention the layered architecture: model, repository, service, UI, util.

## Speaker 2: TA Flow
- Click `Load Demo Data`.
- Log in as `alice.chen@demo.local`.
- Show profile editing, CV path storage, and available jobs.
- View a job detail dialog.
- Open `My Applications` to show shortlisted status, match score, missing skills, and MO reviewer notes.
- Log in as `emma.liu@demo.local` if time allows to show the EBU6304 missing-skills case.
- Log in as `jason.wu@demo.local` if time allows to show mixed results across EBU5606 and EBU6366.

## Speaker 3: MO Flow
- Log in as `ling.ma@qmul.ac.uk`.
- Show job creation/editing.
- Select a job, load applicants, and show the match score and missing skills.
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
- Show that the final demo dataset has 7 courses, 7 MO accounts, and 10 fictional TA accounts.
- Show workload totals and overload highlighting.
- Explain that the demo workload threshold is 10 hours/week.
- Highlight Frank Zhao's accepted workload warning: he is accepted for EBU6475 at 6 hours/week and EBU6366 at 5 hours/week, for a total of 11 hours/week.
- Open `Application Reviews` to show all applications, TA names, courses, MOs, statuses, match scores, missing skills, and MO reviewer notes.
- Use Emma Liu and Jason Wu as missing-skill examples, and Alice Chen, Ben Wang, Grace Xu, Henry Sun, and Ivy Huang as strong-match examples.
- Click `Rebalance Suggestion` to show rule-based recommendations for open jobs.

## Speaker 5: Engineering Explanation
- Show the JSON files in `data/`.
- Explain that all persistence is transparent and inspectable.
- Mention automated tests for matching, workload, validation, duplicate prevention, JSON storage, and sample-data integrity.

## Speaker 6: Teamwork Explanation
- Open `docs/team-task-split.md`.
- Explain how separate UI frames, services, repositories, and tests support parallel branch work.
