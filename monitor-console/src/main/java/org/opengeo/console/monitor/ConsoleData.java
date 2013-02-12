package org.opengeo.console.monitor;

import com.google.common.base.Optional;

public class ConsoleData {

    private final boolean isCacheHit;

    private final Optional<String> cacheMissReason;

    public ConsoleData(boolean isCacheHit, Optional<String> cacheMissReason) {
        this.isCacheHit = isCacheHit;
        this.cacheMissReason = cacheMissReason;
    }

    public boolean isCacheHit() {
        return isCacheHit;
    }

    public Optional<String> getCacheMissReason() {
        return cacheMissReason;
    }

}
