package com.jerry.electrical_checklist;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Numeric stable versions: 1.9 < 1.10, and v1.19.0 equals 1.19. */
public final class AppVersion implements Comparable<AppVersion> {
    private static final Pattern FORMAT = Pattern.compile("^[vV]?([0-9]+(?:\\.[0-9]+){0,3})(?:\\+[A-Za-z0-9.-]+)?$");
    private final List<BigInteger> parts;
    public final String display;
    private AppVersion(String display, List<BigInteger> parts) { this.display = display; this.parts = parts; }
    public static AppVersion parse(String text) {
        if (text == null || text.length() > 100) return null;
        Matcher match = FORMAT.matcher(text.trim());
        if (!match.matches()) return null;
        List<BigInteger> parts = new ArrayList<>();
        for (String part : match.group(1).split("\\.")) parts.add(new BigInteger(part));
        return new AppVersion(match.group(1), parts);
    }
    @Override public int compareTo(AppVersion other) {
        for (int i = 0; i < Math.max(parts.size(), other.parts.size()); i++) {
            BigInteger left = i < parts.size() ? parts.get(i) : BigInteger.ZERO;
            BigInteger right = i < other.parts.size() ? other.parts.get(i) : BigInteger.ZERO;
            int result = left.compareTo(right);
            if (result != 0) return result;
        }
        return 0;
    }
    public String key() {
        int length = parts.size();
        while (length > 1 && parts.get(length - 1).signum() == 0) length--;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) key.append('.');
            key.append(parts.get(i));
        }
        return key.toString();
    }
}
