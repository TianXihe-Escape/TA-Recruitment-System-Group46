# File Storage Notes

## Purpose

This note explains the role of the main JSON files used for local persistence in the TA Recruitment System.

## Why file storage is used

The coursework requires persistent data to be stored in simple text-file formats rather than in a database. For this reason, the project uses JSON files to store application data locally.

## Main data files

### `users.json`

Stores account-level data such as:

- user ID
- username or email
- password value
- role
- managed modules for MO accounts

This file supports role-based login and dashboard redirection.

### `profiles.json`

Stores TA applicant profile data such as:

- applicant ID
- linked user ID
- name and email
- phone number
- programme and year of study
- skills
- CV path
- supporting information

In practice this file provides the profile content used by TA profile editing, MO review, and Admin directory views.

### `jobs.json`

Stores job-posting data such as:

- job ID
- module code
- module title
- duties
- required skills
- hours
- application deadline
- job status

This file is the source of vacancy information shown across TA, MO, and Admin screens.

### `applications.json`

Stores application records such as:

- application ID
- applicant ID
- job ID
- application status
- reviewer notes
- match-related information

This file is central to the recruitment workflow because it links applicants with specific job postings and records review outcomes.

### `notifications.json`

Stores in-system notification records such as:

- notification ID
- target user ID
- message text
- created time
- read or unread state

This file supports user-facing alerts after important workflow events.

### `messages.json`

Stores TA and MO message records such as:

- message ID
- sender and recipient user IDs
- related job ID where relevant
- sent time
- read or unread state
- message body

This file keeps workflow-related communication inside the system.

### `allocations.json`

Stores accepted-assignment records such as:

- allocation ID
- application ID
- applicant ID
- job ID
- allocation time
- allocating user ID
- active flag

This file is important because Admin workload monitoring depends on accepted active allocations rather than only on raw applications.

### `config.json`

Stores shared configuration values such as:

- workload threshold
- other small system settings used across the application

This file helps shared business rules stay configurable without hard-coding every value directly into UI logic.

## Related managed folders

### `cv/`

Stores managed copies or demo copies of applicant CV files.

### `supporting-documents/`

Stores supporting evidence files associated with applicant profiles.

### `exports/`

Stores CSV reports exported from the Admin dashboard.

## Record relationships

The local storage structure becomes easier to explain if the team highlights the main relationships:

- user accounts connect to TA profiles
- TA profiles are used when submitting applications
- applications connect applicants and job postings
- accepted applications can create active allocation records
- notifications and messages support the surrounding workflow rather than replacing it

## Why this structure helps

Separating the data by record type makes the project easier to maintain and helps keep each file focused on one main responsibility.
