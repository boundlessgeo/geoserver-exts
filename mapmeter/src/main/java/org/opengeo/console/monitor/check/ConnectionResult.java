package org.opengeo.console.monitor.check;

import com.google.common.base.Optional;

// Encapsulate the connection result of a status endpoint check
// This doesn't contain anything of value yet, but we can hang useful things off it.
public class ConnectionResult {

    private final Optional<Integer> statusCode;

    private final Optional<String> error;

    public ConnectionResult(int statusCode, String error) {
        this.statusCode = Optional.of(statusCode);
        this.error = Optional.of(error);
    }

    public ConnectionResult(int statusCode) {
        this.statusCode = Optional.of(statusCode);
        this.error = Optional.absent();
    }

    public ConnectionResult(String error) {
        this.error = Optional.of(error);
        this.statusCode = Optional.absent();
    }

    public boolean isError() {
        return error.isPresent();
    }

    public String getError() {
        return error.get();
    }

    public Optional<Integer> getStatusCode() {
        return statusCode;
    }

}
