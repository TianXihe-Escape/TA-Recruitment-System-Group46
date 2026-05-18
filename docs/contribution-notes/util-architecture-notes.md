# Utility Layer Architecture Notes

## Purpose

This note explains the role of the utility layer in the TA Recruitment System.

## Why the utility layer matters

Although utility classes are not directly tied to one visible UI screen, they support all three main workflows in the system:

- TA actions
- MO actions
- Admin actions

They help keep file handling, record generation, and JSON processing consistent across the application.

## Key utility classes

### `Constants`

Provides shared application constants such as:

- application title
- file and folder paths
- configuration-related values

### `FileUtil`

Provides small helper methods for common file-related checks, such as blank-value checks and path existence checks.

### `IdGenerator`

Generates readable identifiers for:

- users
- applicants
- jobs

### `JsonUtil`

Handles converting between JSON text and in-memory Java structures.

### `SampleDataLoader`

Loads local sample data for testing, demonstration, and viva preparation.

## Architectural value

The utility layer keeps low-level shared responsibilities centralized. This reduces repetition and helps the rest of the system focus on UI, validation, and recruitment workflows.
