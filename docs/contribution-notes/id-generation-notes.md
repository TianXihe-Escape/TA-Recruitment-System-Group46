# ID Generation Notes

## Purpose

This note explains why the system uses generated identifiers instead of asking users to type record IDs manually.

## Why generated IDs are useful

Generated identifiers make stored records easier to read, trace, and debug. They also reduce the risk of duplicate or inconsistent identifiers being entered by users.

## Typical formats

Examples used in the project include:

- `user-ta-01`
- `user-mo-01`
- `applicant-01`
- `job-ebu6335-01`

## Why this matters for JSON storage

Because the project stores data in JSON files instead of a database, readable IDs make local files easier to inspect during testing, debugging, and demonstration.

## Workflow impact

Users do not need to fill in these IDs manually. The system generates them when new records are created, which keeps the UI simpler and the stored data more consistent.
