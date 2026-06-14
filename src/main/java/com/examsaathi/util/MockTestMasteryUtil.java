package com.examsaathi.util;

import com.examsaathi.entity.TestAttempt;

import java.util.Locale;

public final class MockTestMasteryUtil {

    private MockTestMasteryUtil() {}

    public static TestAttempt.MasteryLevel resolveMasteryLevel(double percentage) {
        if (percentage <= 40) {
            return TestAttempt.MasteryLevel.BEGINNER;
        }
        if (percentage <= 60) {
            return TestAttempt.MasteryLevel.DEVELOPING;
        }
        if (percentage <= 80) {
            return TestAttempt.MasteryLevel.PROFICIENT;
        }
        return TestAttempt.MasteryLevel.MASTERED;
    }

    public static String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static boolean labelsMatch(String a, String b) {
        String na = normalizeLabel(a);
        String nb = normalizeLabel(b);
        if (na.equals(nb)) {
            return true;
        }
        return fuzzySubjectMatch(na, nb) || fuzzyTopicMatch(na, nb);
    }

    private static boolean fuzzySubjectMatch(String a, String b) {
        if (a.startsWith("english") && b.startsWith("english")) {
            return true;
        }
        if ((a.contains("math") || a.contains("quant")) && (b.contains("math") || b.contains("quant"))) {
            return true;
        }
        if (a.contains("reasoning") && b.contains("reasoning")) {
            return true;
        }
        if ((a.equals("gk") || a.contains("general knowledge")) &&
            (b.equals("gk") || b.contains("general knowledge"))) {
            return true;
        }
        return false;
    }

    private static boolean fuzzyTopicMatch(String a, String b) {
        if (a.contains("synonym") && b.contains("synonym")) {
            return true;
        }
        if (a.contains("vocab") && b.contains("vocab")) {
            return true;
        }
        return false;
    }

    public static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
