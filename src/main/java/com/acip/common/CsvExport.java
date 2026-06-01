package com.acip.common;

import java.util.List;

public final class CsvExport {

    private CsvExport() {
    }

    public static String render(List<String> headers, List<? extends List<?>> rows) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, headers);
        for (List<?> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(index)));
        }
        csv.append('\n');
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String raw = String.valueOf(value);
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }
}
