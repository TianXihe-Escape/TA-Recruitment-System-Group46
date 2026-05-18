# Scope Limitation Notes

## Purpose

This note records the main boundaries of the current implementation and gives a consistent way to explain them during discussion or acceptance.

## Current strengths

The current project already provides:

- role-based TA, MO, and Admin workflows
- stand-alone Java desktop delivery
- JSON-based local persistence
- explainable matching support
- workload monitoring with warning examples
- notifications, messages, and supporting document references

## Main limitations

### Local JSON persistence

The project uses local JSON files rather than a database. This is correct for coursework scope, but it also means there is no advanced transaction support and limited support for simultaneous editing.

### Coursework-level security

The authentication approach is suitable for a local coursework demonstration, but it is not intended as a production-grade security architecture with full audit, recovery, and hardened deployment controls.

### Lightweight schedule reasoning

The system distinguishes workload and schedule information, but it does not perform full calendar conflict resolution. Reviewers still need to use judgment when comparing time-related details.

### Desktop-only delivery

Because the project is built as a Swing application, it is designed for local desktop use rather than browser-based remote access.

### Lightweight reporting

The CSV export is useful for summary reporting, but it is not intended as a full reporting or analytics platform.

## Why these trade-offs are reasonable

These boundaries are easier to defend when explained as deliberate scope choices:

- the coursework requires a stand-alone Java application
- the coursework requires text-file persistence rather than a database
- transparent local logic is easier to explain in viva
- the team prioritized core recruitment flows first

## Future work directions

Natural future improvements could include:

- richer scheduling and conflict detection
- broader reporting and export options
- stronger search and filtering
- more production-grade authentication and audit controls
- further UI refinement and onboarding guidance

## Practical discussion value

Explaining limitations clearly can strengthen a presentation because it shows scope awareness. The system should be described as a solid coursework implementation with clear boundaries, not as a production deployment.
