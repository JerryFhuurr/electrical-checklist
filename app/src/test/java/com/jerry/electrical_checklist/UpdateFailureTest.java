package com.jerry.electrical_checklist;

import org.junit.Test;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLHandshakeException;
import static org.junit.Assert.*;
import static com.jerry.electrical_checklist.GitHubReleaseClient.Status.*;

public class UpdateFailureTest {
    @Test public void dnsFailureIsNotReportedAsGenericConnectivityFailure() {
        assertEquals(DNS_ERROR, UpdateFailure.classify(new UnknownHostException("api.github.com")));
    }
    @Test public void wrappedDnsFailureRemainsActionable() {
        assertEquals(DNS_ERROR, UpdateFailure.classify(new IOException("request failed", new UnknownHostException())));
    }
    @Test public void timeoutHasItsOwnResult() {
        assertEquals(CONNECTION_TIMEOUT, UpdateFailure.classify(new SocketTimeoutException()));
    }
    @Test public void certificateFailureIsNotReportedAsDisconnectedWifi() {
        assertEquals(SECURE_CONNECTION_ERROR, UpdateFailure.classify(new SSLHandshakeException("certificate")));
    }
    @Test public void unrelatedFailuresUseGenericResult() {
        assertEquals(ERROR, UpdateFailure.classify(new IOException("HTTP 500")));
    }
}
