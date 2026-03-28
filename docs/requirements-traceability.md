# Requirements Traceability Matrix

| Requirement / User Story | Module / Class | UI Screen | Test Coverage |
|---|---|---|---|
| TA can register | `service.AuthService` | `ui.RegisterFrame` | `ValidationServiceTest` |
| User can log in by role | `service.AuthService` | `ui.LoginFrame` | manual |
| TA can edit applicant profile | `service.ApplicantService` | `ui.TADashboardFrame` | `ValidationServiceTest`, manual |
| TA can store CV path | `service.ApplicantService` | `ui.TADashboardFrame` | manual |
| TA can browse jobs | `service.JobService` | `ui.TADashboardFrame` | manual |
| TA can apply for jobs | `service.ApplicationService` | `ui.TADashboardFrame` | `ApplicationServiceTest` |
| Duplicate application is blocked | `service.ApplicationService` | `ui.TADashboardFrame` | `ApplicationServiceTest` |
| MO can create/edit jobs | `service.JobService` | `ui.MOManagementFrame` | manual |
| MO can review applicants | `service.ApplicationService`, `service.MatchingService` | `ui.MOManagementFrame` | `MatchingServiceTest`, manual |
| MO can shortlist/accept/reject | `service.ApplicationService` | `ui.MOManagementFrame` | `ApplicationServiceTest`, manual |
| Admin can view workloads | `service.WorkloadService` | `ui.AdminDashboardFrame` | `WorkloadServiceTest` |
| Overload warning appears | `service.WorkloadService` | `ui.AdminDashboardFrame` | `WorkloadServiceTest` |
| Skill match score is explainable | `service.MatchingService` | `ui.MOManagementFrame` | `MatchingServiceTest` |
| File persistence works from fresh clone | `repository.JsonDataStore` | all screens | `JsonDataStoreTest` |
