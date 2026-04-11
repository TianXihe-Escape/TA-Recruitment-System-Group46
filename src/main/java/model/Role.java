package model;

/**
 * Enumeration of all supported user roles in the TA Recruitment System.
 * Each role defines a set of permissions and access levels within the application.
 * The system implements role-based access control (RBAC) to ensure users can only
 * perform actions appropriate to their responsibilities.
 *
 * The roles are hierarchical in terms of access privileges:
 * - TA (Teaching Assistant): Basic user role for students applying for TA positions
 * - MO (Module Organizer): Faculty role for managing specific course modules
 * - ADMIN (Administrator): System-wide administrative access
 *
 * This enum is used throughout the application for:
 * - User authentication and authorization
 * - UI customization based on user permissions
 * - Dashboard selection after login
 * - Access control for various system features
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public enum Role {

    /**
     * Teaching Assistant role.
     * Represents students who can:
     * - Apply for TA positions
     * - View their application status
     * - Update their applicant profile
     * - Access the TA dashboard with personal information
     *
     * This is the most basic user role with limited system access.
     */
    TA,

    /**
     * Module Organizer role.
     * Represents faculty members responsible for specific course modules.
     * Module Organizers can:
     * - Post job openings for their managed modules
     * - Review applications for positions in their modules
     * - Manage workload distribution for their courses
     * - Access MO dashboard with module-specific information
     *
     * This role has elevated permissions compared to TA but is limited to specific modules.
     */
    MO,

    /**
     * Administrator role.
     * Represents system administrators with full access to all features.
     * Administrators can:
     * - Manage all users and their roles
     * - Create and modify job postings across all modules
     * - Review all applications system-wide
     * - Access comprehensive system reports and analytics
     * - Configure system settings and preferences
     * - Load and reset sample data
     *
     * This is the highest privilege role with unrestricted system access.
     */
    ADMIN
}
