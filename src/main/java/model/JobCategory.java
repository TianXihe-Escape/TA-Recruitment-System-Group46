package model;

/**
 * Broad activity type for a recruitment posting.
 */
public enum JobCategory {
    MODULE_TA("Module TA"),
    INVIGILATION("Invigilation"),
    OTHER_ACTIVITY("Other Activity");

    private final String displayName;

    JobCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
