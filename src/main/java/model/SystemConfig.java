package model;

/**
 * File-based configuration for demo thresholds.
 */
public class SystemConfig {
    private int workloadThreshold = 12;

    public int getWorkloadThreshold() {
        return workloadThreshold;
    }

    public void setWorkloadThreshold(int workloadThreshold) {
        this.workloadThreshold = workloadThreshold;
    }
}
