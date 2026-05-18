package model;

import java.time.LocalDateTime;

/**
 * Audit entry for an application status transition.
 */
public class StatusHistoryEntry {
    private ApplicationStatus status;
    private LocalDateTime changedAt;
    private String actorUserId;
    private String note;

    public StatusHistoryEntry() {
    }

    public StatusHistoryEntry(ApplicationStatus status, LocalDateTime changedAt, String actorUserId, String note) {
        this.status = status;
        this.changedAt = changedAt;
        this.actorUserId = actorUserId;
        this.note = note;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
