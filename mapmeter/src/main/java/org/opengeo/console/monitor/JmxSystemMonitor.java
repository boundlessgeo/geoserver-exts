package org.opengeo.console.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;

public class JmxSystemMonitor implements SystemMonitor {

    private OperatingSystemMXBean operatingSystemMXBean;

    private MemoryMXBean memoryMXBean;

    public JmxSystemMonitor() {
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public SystemStatSnapshot pollSystemStatSnapshot() {
        double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();

        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long heapUsed = heapMemoryUsage.getUsed();
        long nonHeapUsed = nonHeapMemoryUsage.getUsed();
        long totalUsed = heapUsed + nonHeapUsed;

        long heapMax = heapMemoryUsage.getMax();
        long nonHeapMax = nonHeapMemoryUsage.getMax();
        long totalMax = heapMax + nonHeapMax;

        SystemStatSnapshot systemStatSnapshot = new SystemStatSnapshot(totalUsed, totalMax,
                systemLoadAverage);
        return systemStatSnapshot;
    }

}
