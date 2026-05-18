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

### `applications.json`

Stores application records such as:

- application ID
- applicant ID
- job ID
- application status
- reviewer notes
- match-related information

## Why this structure helps

Separating the data by record type makes the project easier to maintain and helps keep each file focused on one main responsibility.
