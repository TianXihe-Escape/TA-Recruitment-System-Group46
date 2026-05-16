# Requirements Traceability Matrix

| Story | Feature | Implementation | UI / Evidence | Test Coverage |
|---|---|---|---|---|
| US01 | Register account | `AuthService.registerTa` | `RegisterFrame` | `AuthServiceTest` |
| US02 | Log in | `AuthService.login` | `LoginFrame` | manual |
| US03 | Reset password | `AuthService.resetPassword` | `ResetPasswordFrame` | `AuthServiceTest` |
| US04 | Create applicant profile | `ApplicantProfile`, `ApplicantService` | `TADashboardFrame` | `ValidationServiceTest` |
| US05 | Upload CV/supporting documents | `CvStorageService`, `supportingDocumentPath` | TA profile, MO applicant details | manual |
| US06 | Browse TA vacancies | `JobService.getOpenJobs` | TA Available Jobs tab | manual |
| US07 | View vacancy details | `JobDetailsDialog` | TA View Job Details | manual |
| US08 | Search/filter vacancies | `JobService.searchOpenJobs` | TA search/module/category filters | `JobServiceTest` |
| US09 | Submit application | `ApplicationService.apply` | TA Apply button | `ApplicationServiceTest` |
| US10 | Withdraw application | `ApplicationService.withdrawApplication` with deadline rule | TA Withdraw Application | `ApplicationServiceTest` |
| US11 | View application status | `ApplicationRepository.findByApplicantId` | TA My Applications tab | manual |
| US12 | Save favourite jobs | `ApplicantProfile.favoriteJobIds` | TA Favourite Jobs tab / Toggle Favourite | `JsonDataStoreTest` |
| US13 | Post vacancy | `JobService.saveJob` | MO job editor | manual |
| US14 | Edit or close vacancy | `JobService.saveJob`, guarded close/reopen flows | MO dashboard | manual |
| US15 | Define selection criteria | `JobPosting.requiredSkills` | MO Required Skills field and match details | `ValidationServiceTest` |
| US16 | Review applicant list | `ApplicationService.getApplicationsForJob` | MO Load Applicants | manual |
| US17 | Review applicant profile/CV | `CvStorageService.resolveCvPath` | MO applicant summary / clickable document lines | manual |
| US18 | Shortlist candidates | `ApplicationService.updateStatus`, `removeShortlist` | MO Shortlist / Remove Shortlist | `ApplicationServiceTest` |
| US19 | Record hiring decision | `ApplicationRecord.decisionAt`, `lastUpdatedAt`, `statusHistory` | TA/MO details | `ApplicationServiceTest` |
| US20 | Allocate TA to vacancy | `AllocationRecord`, `AllocationService` | Admin workload and accepted assignments | `ApplicationServiceTest` |
| US21 | View TA workload dashboard | `WorkloadService.buildWorkloadRecords` | Admin TA Workload | `WorkloadServiceTest` |
| US22 | Conflict/workload warning | `WorkloadService.projectedHours` | MO accept workload warning | `WorkloadServiceTest` |
| US23 | Send notifications | `NotificationService`, `notifications.json` | TA tab, MO/Admin View Notifications | `NotificationServiceTest` |
| US24 | Export recruitment report | `ExportService` | Admin Export CSV | manual |
| US25 | Skill matching recommendation | `MatchingService.calculateMatch` | MO match details / ranking | `MatchingServiceTest` |
| US26 | Missing skill suggestion | `ApplicationRecord.missingSkills` | TA application detail suggestion | manual |
| US27 | Role-based permissions | role dashboards, MO managed modules | Login/MO/Admin frames | manual |
| US28 | File-based storage | `JsonDataStore`, JSON files | `data/*.json`, no database | `JsonDataStoreTest` |
