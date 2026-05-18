# Acceptance Testing Notes

## Purpose

This note summarizes how the system can be presented during final acceptance-style checking.
It is intended as a practical preparation note rather than a formal test specification.

## Useful areas to demonstrate

Acceptance-style checking can focus on:

- account login for different roles
- TA profile and application workflow
- MO job posting and applicant review workflow
- Admin monitoring and oversight workflow
- local JSON persistence behavior

## Before the demonstration

Before final acceptance-style checking, it is useful to confirm that:

- Java 17 or later is available on the local machine
- the project opens from the expected root folder
- the `data/`, `cv/`, `supporting-documents/`, and `exports/` folders are present
- the login screen starts correctly
- the demo dataset can be restored with `Load Demo Data` if previous practice changed local records

## TA workflow checks

Useful TA-side acceptance points include:

- logging in as a TA account
- opening and explaining the applicant profile
- showing stored CV path and supporting document references
- browsing open jobs with search and filter controls
- opening job details to explain workload, deadline, location, and required skills
- showing `TA Demand` as `accepted / required`
- opening `My Applications` to show status, score, missing skills, and reviewer notes
- showing that withdrawn records remain visible as part of the audit trail
- opening notifications and messages to show in-system communication

## MO workflow checks

Useful MO-side acceptance points include:

- opening the job editor
- explaining module code, workload, schedule, location, duties, and deadline fields
- selecting a job and loading its applicants
- showing the applicant summary, match score, and missing skills
- opening CV and supporting document paths from the review area
- demonstrating applicant filtering and sorting
- demonstrating reviewer notes and one or more review actions such as shortlist, invite interview, accept, reject, or cancel acceptance
- explaining that higher-impact review actions use confirmation dialogs

## Admin workflow checks

Useful Admin-side acceptance points include:

- showing the summary cards for open jobs, closed jobs, applications, and accepted TAs
- opening the workload view and explaining the warning column
- using Frank Zhao as the planned weekly-overload example
- explaining why one-off event workload is displayed separately from weekly workload
- opening `Application Reviews` to inspect all applications in one place
- showing `Load Demo Data`, `Reset Demo Data`, and `Export CSV`
- explaining that exported files are written into `exports/`

## Architecture evidence that can be shown

If acceptance questions become more technical, the team can also show:

- the layered package structure under `src/main/java`
- the JSON files under `data/`
- the relationship between applicant profile data, job data, and application records
- the fact that matching and workload logic are explainable local rules rather than hidden external services

## Practical fallback plan

If something unexpected happens during demonstration, lower-risk fallback actions include:

- returning to the login screen and loading demo data again
- switching to a prepared example account instead of improvising a fresh edit
- focusing on one TA example, one MO example, and the Admin overview if time is short
- showing stored document paths even if the local machine does not open files directly from the UI

## Why this note is useful

A clear testing outline helps the team prepare a more structured and confident final demonstration.
It also reduces the chance that the presentation skips useful evidence already available in the system.

## Relationship to the project

Because the project is a stand-alone desktop application with local persistence, acceptance checking should show both user-facing workflow behavior and the stored-data model behind it.
