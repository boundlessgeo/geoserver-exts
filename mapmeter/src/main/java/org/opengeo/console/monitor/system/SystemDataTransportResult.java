package org.opengeo.console.monitor.system;

import com.google.common.base.Optional;

public class SystemDataTransportResult {

    private final Optional<Integer> statusCode;

    private final Optional<String> responseText;

    private final boolean isSuccessful;

    private final Optional<Exception> error;

    public SystemDataTransportResult(boolean isSuccessful, Optional<Integer> statusCode,
            Optional<String> responseText, Optional<Exception> error) {
        this.isSuccessful = isSuccessful;
        this.statusCode = statusCode;
        this.responseText = responseText;
        this.error = error;
    }

    public Optional<Integer> getStatusCode() {
        return statusCode;
    }

    public Optional<String> getResponseText() {
        return responseText;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public boolean isTransferError() {
        return error.isPresent();
    }

    public Optional<Exception> getError() {
        return error;
    }

    public static SystemDataTransportResult transferError(Exception e) {
        return new SystemDataTransportResult(false, Optional.<Integer> absent(),
                Optional.<String> absent(), Optional.of(e));
    }

    public static SystemDataTransportResult success() {
        return new SystemDataTransportResult(true, Optional.<Integer> absent(),
                Optional.<String> absent(), Optional.<Exception> absent());
    }

    public static SystemDataTransportResult errorResponse(int statusCode, String responseText) {
        return new SystemDataTransportResult(false, Optional.of(statusCode),
                Optional.of(responseText), Optional.<Exception> absent());
    }
}
