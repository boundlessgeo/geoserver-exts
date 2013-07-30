package org.geoserver.monitor.gwc;

import com.google.common.base.Optional;

public class GwcStatistics {

    private final boolean isTiled;

    private final boolean isCacheHit;

    private final Optional<String> missReason;

    public GwcStatistics(boolean isTiled, boolean isCacheHit, Optional<String> missReason) {
        this.isTiled = isTiled;
        this.isCacheHit = isCacheHit;
        this.missReason = missReason;
    }

    public boolean isTiled() {
        return isTiled;
    }

    public boolean isCacheHit() {
        return isCacheHit;
    }

    public Optional<String> getMissReason() {
        return missReason;
    }

}
