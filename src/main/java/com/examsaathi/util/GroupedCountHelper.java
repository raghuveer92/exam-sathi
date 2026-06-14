package com.examsaathi.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts JPA GROUP BY projection rows into subject-id keyed maps. */
public final class GroupedCountHelper {

    private GroupedCountHelper() {}

    public static Map<Long, Integer> toIntMap(List<Object[]> rows) {
        Map<Long, Integer> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    public static Map<Long, Double> toDoubleMap(List<Object[]> rows) {
        Map<Long, Double> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).doubleValue());
        }
        return map;
    }
}
