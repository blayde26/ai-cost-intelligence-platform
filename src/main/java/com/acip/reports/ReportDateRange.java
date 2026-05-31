package com.acip.reports;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public record ReportDateRange(OffsetDateTime startDate, OffsetDateTime endDateExclusive) {

    public static ReportDateRange parse(String startDate, String endDate) {
        OffsetDateTime start = parseStart(startDate);
        OffsetDateTime endExclusive = parseEndExclusive(endDate);
        if (start != null && endExclusive != null && !start.isBefore(endExclusive)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        return new ReportDateRange(start, endExclusive);
    }

    public String whereClause(String alias) {
        return whereParts(alias).whereClause();
    }

    public List<Object> args() {
        return whereParts("").args();
    }

    QueryParts whereParts(String alias) {
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (startDate != null) {
            conditions.add(prefix + "request_timestamp >= ?");
            args.add(startDate);
        }
        if (endDateExclusive != null) {
            conditions.add(prefix + "request_timestamp < ?");
            args.add(endDateExclusive);
        }
        return new QueryParts(
                conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions),
                args
        );
    }

    private static OffsetDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDateOrDateTime(value, false);
    }

    private static OffsetDateTime parseEndExclusive(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDateOrDateTime(value, true);
    }

    private static OffsetDateTime parseDateOrDateTime(String value, boolean endExclusive) {
        try {
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value);
                return (endExclusive ? date.plusDays(1) : date).atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates must be ISO-8601 dates or date-times");
        }
    }

    record QueryParts(String whereClause, List<Object> args) {
    }
}
