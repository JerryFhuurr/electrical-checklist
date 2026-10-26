package com.jerry.electrical_checklist;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.jerry.electrical_checklist.GitHubReleaseClient.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GitHubReleaseClientTest {
    static final String APK = "https://github.com/JerryFhuurr/electrical-checklist/releases/download/Main/E-Checklist-v9.0-release.apk";
    private JSONObject asset(String name) throws Exception {
        return new JSONObject().put("name", name).put("browser_download_url", APK).put("state", "uploaded").put("size", 100);
    }
    private JSONObject release(String tag, JSONObject... assets) throws Exception {
        JSONArray array = new JSONArray(); for (JSONObject asset : assets) array.put(asset);
        return new JSONObject().put("tag_name", tag).put("assets", array);
    }
    @Test public void mainTagUsesVersionFromReleaseApkFilename() throws Exception {
        Result result = GitHubReleaseClient.parse(release("Main", asset("E-Checklist-v1.17-release.apk")));
        assertEquals(Status.RELEASE, result.status); assertEquals("1.17", result.release.version);
    }
    @Test public void numericTagSupportsGenericApkAndChoosesHighestAsset() throws Exception {
        assertEquals("2.0", GitHubReleaseClient.parse(release("v2.0", asset("app-release.apk"))).release.version);
        assertEquals("1.10", GitHubReleaseClient.parse(release("Main", asset("E-Checklist-v1.9-release.apk"),
                asset("E-Checklist-v1.10-release.apk"))).release.version);
    }
    @Test public void draftAndPrereleaseAreIgnored() throws Exception {
        for (String flag : new String[]{"draft", "prerelease"})
            assertEquals(Status.NO_RELEASE, GitHubReleaseClient.parse(release("v2.0", asset("app-release.apk")).put(flag, true)).status);
    }
    @Test public void missingDebugEmptyAndIncompleteAssetsAreIgnored() throws Exception {
        assertEquals(Status.NO_APK, GitHubReleaseClient.parse(release("Main")).status);
        assertEquals(Status.NO_APK, GitHubReleaseClient.parse(release("v2.0", asset("app-debug.apk"),
                asset("app-release.apk").put("state", "new"), asset("empty.apk").put("size", 0))).status);
        assertEquals(Status.NO_APK, GitHubReleaseClient.parse(release("Main", asset("app-release.apk"))).status);
    }
    @Test public void acceptsOnlyHttpsApksFromThisRepository() throws Exception {
        assertTrue(GitHubReleaseClient.validDownload(APK));
        for (String url : new String[]{"http://github.com/JerryFhuurr/electrical-checklist/releases/download/v2/app.apk",
                APK.replace("github.com", "github.com.evil.test"), APK.replace("electrical-checklist", "other-repo"),
                APK + "?redirect=evil", "javascript:alert(1)", APK.replace("/Main/", "/../../other/"),
                "https://github.com@evil.test/file.apk", null}) {
            assertFalse(String.valueOf(url), GitHubReleaseClient.validDownload(url));
        }
        assertEquals(Status.NO_APK, GitHubReleaseClient.parse(release("v2.0", asset("app.apk")
                .put("browser_download_url", "https://evil.test/app.apk"))).status);
    }
    private static class Connection extends HttpURLConnection {
        final int status; final byte[] bytes; boolean disconnected;
        Connection(int status, String body) throws Exception {
            super(new URL(GitHubReleaseClient.API_URL)); this.status = status; bytes = body.getBytes(StandardCharsets.UTF_8);
        }
        @Override public int getResponseCode() { return status; }
        @Override public String getHeaderField(String name) { return "X-RateLimit-Remaining".equals(name) ? "0" : null; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void connect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void disconnect() { disconnected = true; }
    }
    @Test public void httpSuccessSetsTimeoutsHeadersAndClosesConnection() throws Exception {
        Connection connection = new Connection(200, release("v2.0", asset("app-release.apk")).toString());
        assertEquals(Status.RELEASE, new GitHubReleaseClient(url -> connection).call().status);
        assertEquals(5000, connection.getConnectTimeout()); assertEquals(5000, connection.getReadTimeout());
        assertEquals("application/vnd.github+json", connection.getRequestProperty("Accept"));
        assertEquals("2026-03-10", connection.getRequestProperty("X-GitHub-Api-Version"));
        assertTrue(connection.disconnected);
    }
    @Test public void missingReleaseAndRateLimitsHaveSpecificResults() throws Exception {
        for (int status : new int[]{404, 403, 429}) {
            Connection connection = new Connection(status, "");
            assertEquals(status == 404 ? Status.NO_RELEASE : Status.RATE_LIMITED,
                    new GitHubReleaseClient(url -> connection).call().status);
            assertTrue(connection.disconnected);
        }
    }
    @Test public void serverFailureMalformedAndOversizedResponsesFailSafely() throws Exception {
        for (Connection connection : new Connection[]{new Connection(500, ""), new Connection(200, "not json"),
                new Connection(200, new String(new char[1024 * 1024 + 1]))}) {
            try { new GitHubReleaseClient(url -> connection).call(); fail("Expected an IO failure"); }
            catch (IOException expected) { assertTrue(connection.disconnected); }
        }
    }
    @Test public void preferencesPersistSkipAndReminderAcrossInstancesAndProgressReset() {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "update_test_" + System.nanoTime();
        try {
            long now = 1800000000000L;
            Release release = new Release("9.0", APK);
            UpdatePreferences prefs = new UpdatePreferences(context, name);
            assertTrue(prefs.checkDue(now)); prefs.attempted(now); prefs.record(Result.available(release), now);
            assertFalse(prefs.checkDue(now)); assertNotNull(prefs.cached(now)); assertNull(prefs.cached(now + UpdateRules.DAY));
            prefs.skip(release);
            new ProgressStore(context).save(new ChecklistProgress(java.util.Collections.emptySet(), java.util.Collections.emptySet()));
            prefs = new UpdatePreferences(context, name);
            assertTrue(prefs.suppressed(new Release("9.0.0", APK), now));
            assertFalse(prefs.suppressed(new Release("9.1", APK), now));
            prefs.remindLater(release, now);
            prefs = new UpdatePreferences(context, name);
            assertTrue(prefs.suppressed(release, now + UpdateRules.DAY - 1));
            assertFalse(prefs.suppressed(release, now + UpdateRules.DAY));
            prefs.record(Result.of(Status.NO_RELEASE), now); assertNull(prefs.cached(now));
        } finally { context.deleteSharedPreferences(name); }
    }
}
