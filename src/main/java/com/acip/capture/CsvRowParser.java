package com.acip.capture;

import java.util.ArrayList;
import java.util.List;

public class CsvRowParser {

    public List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("CSV row has an unterminated quoted value.");
        }
        values.add(current.toString().trim());
        return values;
    }
}
