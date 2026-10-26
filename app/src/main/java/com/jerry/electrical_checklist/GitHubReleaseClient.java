package com.jerry.electrical_checklist;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Public release metadata only. No credentials, APK downloading, or installation. */
public final class GitHubReleaseClient implements Callable<GitHubReleaseClient.Result> {
    public static final String API_URL = "https://api.github.com/repos/JerryFhuurr/electrical-checklist/releases/latest";
    public static final String RELEASES_URL = "https://github.com/JerryFhuurr/electrical-checklist/releases";
    private static final String DOWNLOAD_PREFIX = "/jerryfhuurr/electrical-checklist/releases/download/";
    private static final int MAX_RESPONSE = 1024 * 1024;
    private static final Pattern ASSET_VERSION = Pattern.compile("(?:^|[-_ ])v?([0-9]+(?:\\.[0-9]+){1,3})(?:[-_]release)?\\.apk$", Pattern.CASE_INSENSITIVE);
    interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
    private final ConnectionFactory connections;
    public GitHubReleaseClient() { this(url -> (HttpURLConnection) url.openConnection()); }
    GitHubReleaseClient(ConnectionFactory connections) { this.connections = connections; }

    public enum Status { RELEASE, NO_RELEASE, NO_APK, ERROR, RATE_LIMITED }
    public static final class Release {
        public final String version, downloadUrl;
        Release(String version, String downloadUrl) { this.version = version; this.downloadUrl = downloadUrl; }
    }
    public static final class Result {
        public final Status status;
        public final Release release;
        private Result(Status status, Release release) { this.status = status; this.release = release; }
        public static Result of(Status status) { return new Result(status, null); }
        public static Result available(Release release) { return new Result(Status.RELEASE, release); }
        boolean successful() { return status == Status.RELEASE || status == Status.NO_RELEASE || status == Status.NO_APK; }
    }

    static boolean validDownload(String value) {
        try {
            URI uri = new URI(value).normalize();
            return "https".equalsIgnoreCase(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getRawQuery() == null && uri.getFragment() == null
                    && uri.getPath().toLowerCase(Locale.ROOT).startsWith(DOWNLOAD_PREFIX)
                    && uri.getPath().toLowerCase(Locale.ROOT).endsWith(".apk");
        } catch (URISyntaxException | NullPointerException error) { return false; }
    }

    static Result parse(JSONObject json) {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return Result.of(Status.NO_RELEASE);
        AppVersion tag = AppVersion.parse(json.optString("tag_name"));
        if (tag == null) tag = AppVersion.parse(json.optString("name"));
        JSONArray assets = json.optJSONArray("assets");
        Release chosen = null;
        AppVersion chosenVersion = null;
        if (assets != null) for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) continue;
            String name = asset.optString("name"), url = asset.optString("browser_download_url");
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".apk") || lower.contains("debug") || !validDownload(url)
                    || !"uploaded".equals(asset.optString("state", "uploaded")) || asset.optLong("size", 1) <= 0) continue;
            Matcher filename = ASSET_VERSION.matcher(name);
            AppVersion version = filename.find() ? AppVersion.parse(filename.group(1)) : tag;
            if (version != null && (chosenVersion == null || version.compareTo(chosenVersion) > 0)) {
                chosenVersion = version;
                chosen = new Release(version.display, url);
            }
        }
        return chosen == null ? Result.of(Status.NO_APK) : Result.available(chosen);
    }

    @Override public Result call() throws IOException {
        HttpURLConnection connection = connections.open(new URL(API_URL));
        try {
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
            connection.setRequestProperty("User-Agent", "E-Checklist-Android");
            int status = connection.getResponseCode();
            if (status == 404) return Result.of(Status.NO_RELEASE);
            if (status == 429 || (status == 403 && "0".equals(connection.getHeaderField("X-RateLimit-Remaining"))))
                return Result.of(Status.RATE_LIMITED);
            if (status != 200) throw new IOException("GitHub HTTP " + status);
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream body = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int length;
                while ((length = input.read(buffer)) != -1) {
                    if (body.size() + length > MAX_RESPONSE) throw new IOException("Release response too large");
                    body.write(buffer, 0, length);
                }
                return parse(new JSONObject(new String(body.toByteArray(), StandardCharsets.UTF_8)));
            } catch (JSONException error) { throw new IOException("Invalid GitHub release response", error); }
        } finally { connection.disconnect(); }
    }
}
