package org.opengeo.console.monitor;

import java.util.Date;

public class SystemStatSnapshot {

    private final long totalMemoryUsage;

    private final long totalMemoryMax;

    private final double systemLoadAverage;

    private final long timeStamp;

    public SystemStatSnapshot(long totalMemoryUsage, long totalMemoryMax,
            double systemLoadAverage) {
        this(totalMemoryUsage, totalMemoryMax, systemLoadAverage, new Date().getTime() / 1000);
    }

    public SystemStatSnapshot(long totalMemoryUsage, long totalMemoryMax,
            double systemLoadAverage, long secondsSinceEpoch) {
        this.totalMemoryUsage = totalMemoryUsage;
        this.totalMemoryMax = totalMemoryMax;
        this.systemLoadAverage = systemLoadAverage;
        this.timeStamp = secondsSinceEpoch;
    }

    public long getTotalMemoryUsage() {
        return totalMemoryUsage;
    }

    public long getTotalMemoryMax() {
        return totalMemoryMax;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
