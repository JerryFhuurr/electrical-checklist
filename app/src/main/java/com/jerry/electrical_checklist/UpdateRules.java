package com.jerry.electrical_checklist;

import java.util.Set;

final class UpdateRules {
    static final long DAY = 24L * 60 * 60 * 1000;
    static final long RETRY = 60L * 60 * 1000;
    private UpdateRules() { }
    static boolean checkDue(long lastAttempt, boolean successful, long now) {
        return lastAttempt <= 0 || now < lastAttempt || now - lastAttempt >= (successful ? DAY : RETRY);
    }
    static boolean suppressed(AppVersion version, Set<String> skipped, String remindVersion, long remindAfter, long now) {
        return skipped.contains(version.key()) || (version.key().equals(remindVersion) && now < remindAfter);
    }
    static boolean newer(String available, String installed) {
        AppVersion next = AppVersion.parse(available), current = AppVersion.parse(installed);
        return next != null && current != null && next.compareTo(current) > 0;
    }
}
