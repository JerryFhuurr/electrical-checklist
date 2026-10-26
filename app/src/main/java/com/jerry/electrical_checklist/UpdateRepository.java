package com.jerry.electrical_checklist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import com.jerry.electrical_checklist.GitHubReleaseClient.Result;
import com.jerry.electrical_checklist.GitHubReleaseClient.Status;

/** One request at a time, shared across activities; all callbacks run on the main thread. */
final class UpdateRepository {
    final UpdatePreferences preferences;
    private final Callable<Result> source;
    private final LongSupplier clock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private boolean checking;
    private Result lastResult;
    private long completedAt;
    UpdateRepository(Context context) { this(new UpdatePreferences(context), new GitHubReleaseClient(), System::currentTimeMillis); }
    UpdateRepository(UpdatePreferences preferences, Callable<Result> source, LongSupplier clock) {
        this.preferences = preferences; this.source = source; this.clock = clock;
    }
    long now() { return clock.getAsLong(); }
    synchronized boolean isChecking() { return checking; }
    synchronized Result resultSince(long started) { return lastResult != null && completedAt >= started ? lastResult : null; }
    synchronized void check(boolean force, Consumer<Result> listener) {
        if (checking) { listeners.add(listener); return; }
        if (!force && !preferences.checkDue(now())) {
            GitHubReleaseClient.Release cached = preferences.cached(now());
            Result result = cached == null ? Result.of(Status.NO_RELEASE) : Result.available(cached);
            main.post(() -> listener.accept(result)); return;
        }
        checking = true; listeners.add(listener); preferences.attempted(now());
        worker.execute(() -> {
            Result result;
            try { result = source.call(); }
            catch (Exception error) { result = Result.of(Status.ERROR); }
            preferences.record(result, now());
            Result response = result;
            main.post(() -> finish(response));
        });
    }
    private void finish(Result result) {
        List<Consumer<Result>> callbacks;
        synchronized (this) {
            checking = false; lastResult = result; completedAt = now();
            callbacks = new ArrayList<>(listeners); listeners.clear();
        }
        for (Consumer<Result> callback : callbacks) callback.accept(result);
    }
    void close() { worker.shutdownNow(); main.removeCallbacksAndMessages(null); synchronized (this) { listeners.clear(); } }
}
