# Viva Preparation Notes

## Purpose

This note highlights project areas that are useful to explain during viva or presentation questions.

## Topics that can be explained clearly

Useful discussion topics include:

- why the project uses JSON persistence
- why generated IDs are helpful
- how TA, MO, and Admin responsibilities differ
- how demo data supports realistic workflow presentation
- how validation protects local stored records

## Common questions and short answers

### What kind of system is this?

It is a stand-alone Java Swing application for managing Teaching Assistant recruitment with local JSON persistence.

### Why does the project use Swing?

Swing fits the coursework requirement to build a local Java application without relying on browser deployment or a separate web server.

### Why is there no database?

The coursework direction restricts persistence to local text files, so the project uses JSON under `data/` instead of a database.

### What is the benefit of the layered structure?

The layered structure separates UI, business logic, and persistence responsibilities, which makes the code easier to explain and easier for a team to divide during development.

### What can each role do?

- TA users maintain profiles, browse jobs, apply, withdraw eligible applications, read notes, view alerts, and exchange messages
- MO users manage jobs, review applicants, inspect documents, record notes, and update statuses
- Admin users monitor workloads, inspect all applications, manage demo data, create MO accounts, and export reports

### Where is the AI in the project?

The project uses explainable rule-based logic rather than an external black-box AI service. Matching score and missing skills are generated in a deterministic way and shown clearly in the UI.

### Why is explainability important here?

Recruitment support logic should be understandable. Showing a score together with missing skills makes weaker and stronger matches easier to justify.

### Does the system make final decisions automatically?

No. The system supports decision-making, but MO users still review applicants and choose the final outcome.

### How is workload calculated?

Accepted applications are connected to the workload information stored in jobs, then summarized per applicant. Weekly course-support workload and one-off event workload are tracked separately.

### Why are weekly and one-off workloads separated?

They represent different types of effort. Weekly course support affects recurring load, while invigilation and demo support are one-off commitments.

### What workload example is easiest to show?

Frank Zhao is a useful example because he is intentionally above the weekly threshold in the demo data.

### How are passwords handled?

Passwords are stored as hashes rather than plain text. This is better than storing raw passwords, although a production system would require stronger adaptive hashing and broader security controls.

### Is the storage model production-ready?

No. Local JSON storage is appropriate for coursework scope and for transparent viva explanation, but it is not intended as a high-concurrency production deployment model.

### How does the team explain project limitations honestly?

The strongest explanation is that the team prioritized the coursework core flows first:

- stand-alone Java delivery
- local file persistence
- transparent matching support
- workload monitoring
- role-based recruitment workflow

Advanced scheduling, richer reporting, and stronger production hardening are natural future extensions.

## Why this note is useful

Preparing short explanations in advance makes it easier to answer questions consistently and confidently.

## Practical value

This supports clearer communication about both technical design choices and user-facing workflow decisions.
