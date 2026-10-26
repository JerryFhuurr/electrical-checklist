package com.jerry.electrical_checklist;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Explicit integration check: requires internet and an existing public APK release. */
@RunWith(AndroidJUnit4.class)
public class GitHubReleaseLiveTest {
    @Test public void publicReleaseCanBeReadOnAndroid() throws Exception {
        Assume.assumeTrue("Pass -e liveGithub true to enable network integration",
                "true".equals(InstrumentationRegistry.getArguments().getString("liveGithub")));
        GitHubReleaseClient.Result result = new GitHubReleaseClient().call();
        assertEquals(GitHubReleaseClient.Status.RELEASE, result.status);
        assertNotNull(AppVersion.parse(result.release.version));
        assertTrue(GitHubReleaseClient.validDownload(result.release.downloadUrl));
        System.out.println("Live GitHub release version: " + result.release.version);
    }
}
