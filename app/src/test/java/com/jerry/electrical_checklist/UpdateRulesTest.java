package com.jerry.electrical_checklist;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class UpdateRulesTest {
    private final long time = 1800000000000L;
    @Test public void successfulChecksWaitOneDay() {
        assertFalse(UpdateRules.checkDue(time, true, time + UpdateRules.DAY - 1));
        assertTrue(UpdateRules.checkDue(time, true, time + UpdateRules.DAY));
    }
    @Test public void failuresRetryAfterOneHour() {
        assertFalse(UpdateRules.checkDue(time, false, time + UpdateRules.RETRY - 1));
        assertTrue(UpdateRules.checkDue(time, false, time + UpdateRules.RETRY));
    }
    @Test public void firstRunAndClockRollbackAllowCheck() {
        assertTrue(UpdateRules.checkDue(0, true, time));
        assertTrue(UpdateRules.checkDue(time, true, time - 1));
    }
    @Test public void skipMatchesEquivalentVersionButNotLaterVersion() {
        assertTrue(UpdateRules.suppressed(AppVersion.parse("v2.0.0"), Collections.singleton("2"), "", 0, time));
        assertFalse(UpdateRules.suppressed(AppVersion.parse("2.1"), Collections.singleton("2"), "", 0, time));
    }
    @Test public void reminderExpiresAtExactDeadline() {
        assertTrue(UpdateRules.suppressed(AppVersion.parse("2.0"), Collections.emptySet(), "2", time + UpdateRules.DAY, time));
        assertFalse(UpdateRules.suppressed(AppVersion.parse("2.0"), Collections.emptySet(), "2", time + UpdateRules.DAY, time + UpdateRules.DAY));
    }
    @Test public void reminderDoesNotHideNewVersion() {
        assertFalse(UpdateRules.suppressed(AppVersion.parse("2.1"), Collections.emptySet(), "2", time + UpdateRules.DAY, time));
    }
}
