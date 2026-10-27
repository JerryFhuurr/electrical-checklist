package com.jerry.electrical_checklist;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import com.jerry.electrical_checklist.GitHubReleaseClient.Status;

/** Preserve actionable failure reasons instead of treating every failure as disconnected Wi-Fi. */
final class UpdateFailure {
    private UpdateFailure() { }
    static Status classify(Throwable error) {
        Throwable current = error;
        for (int i = 0; current != null && i < 16; i++, current = current.getCause()) {
            if (current instanceof UnknownHostException) return Status.DNS_ERROR;
            if (current instanceof SocketTimeoutException) return Status.CONNECTION_TIMEOUT;
            if (current instanceof SSLException) return Status.SECURE_CONNECTION_ERROR;
        }
        return Status.ERROR;
    }
}
