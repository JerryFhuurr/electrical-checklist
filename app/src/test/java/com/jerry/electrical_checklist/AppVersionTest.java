package com.jerry.electrical_checklist;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppVersionTest {
    @Test public void comparesNumericPartsInsteadOfTextOrDecimals() {
        assertTrue(UpdateRules.newer("v1.10", "1.9"));
        assertFalse(UpdateRules.newer("1.9", "1.10"));
        assertTrue(UpdateRules.newer("2.0", "1.999"));
    }
    @Test public void ignoresTrailingZerosAndBuildMetadata() {
        assertEquals(0, AppVersion.parse("v1.19.0+build.2").compareTo(AppVersion.parse("1.19")));
        assertFalse(UpdateRules.newer("1.19.0", "1.19"));
    }
    @Test public void canonicalKeysKeepEquivalentVersionsSkipped() {
        assertEquals("1.19", AppVersion.parse("V01.019.0.0").key());
        assertEquals("0", AppVersion.parse("0.0.0").key());
    }
    @Test public void acceptsWhitespaceAndFourPartVersions() {
        assertEquals("1.2.3.4", AppVersion.parse(" v1.2.3.4 ").display);
        assertTrue(UpdateRules.newer("1.2.3.4", "1.2.3"));
    }
    @Test public void largeComponentsDoNotOverflow() {
        assertTrue(UpdateRules.newer("1.999999999999999999999", "1.2147483647"));
    }
    @Test public void rejectsMalformedAndPrereleaseVersions() {
        for (String value : new String[]{null, "", "Main", "1.2-rc1", "1..2", "-1.2", "1.2.3.4.5", "1.2/3", "NaN"})
            assertNull(value, AppVersion.parse(value));
        assertFalse(UpdateRules.newer("Main", "1.19"));
        assertFalse(UpdateRules.newer("2.0", "unknown"));
    }
}
