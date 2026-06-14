package com.examsaathi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV line parser supporting quoted fields with embedded commas.
 */
public final class CsvLineParser {

    private CsvLineParser() {}

    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        if (line == null) {
            return fields;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
