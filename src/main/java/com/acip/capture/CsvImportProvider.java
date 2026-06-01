package com.acip.capture;

import com.acip.pricing.PricingService;
import com.acip.usage.AttributionInference;
import com.acip.usage.AttributionInferenceService;
import com.acip.usage.AttributionSource;
import com.acip.usage.AttributionStatus;
import com.acip.usage.AttributionStatusService;
import com.acip.usage.UsageEvent;
import com.acip.usage.UsageEventRepository;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkTrackingProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CsvImportProvider implements UsageCaptureProvider, UsageImportProvider {

    private final CsvRowParser csvRowParser = new CsvRowParser();
    private final UsageEventRepository usageEventRepository;
    private final PricingService pricingService;
    private final WorkTrackingProvider workTrackingProvider;
    private final AttributionInferenceService attributionInferenceService;
    private final AttributionStatusService attributionStatusService;

    public CsvImportProvider(
            UsageEventRepository usageEventRepository,
            PricingService pricingService,
            WorkTrackingProvider workTrackingProvider,
            AttributionInferenceService attributionInferenceService,
            AttributionStatusService attributionStatusService
    ) {
        this.usageEventRepository = usageEventRepository;
        this.pricingService = pricingService;
        this.workTrackingProvider = workTrackingProvider;
        this.attributionInferenceService = attributionInferenceService;
        this.attributionStatusService = attributionStatusService;
    }

    @Override
    public UsageCaptureSource source() {
        return UsageCaptureSource.CSV_IMPORT;
    }

    @Override
    public UsageCaptureMethod method() {
        return UsageCaptureMethod.FILE_IMPORT;
    }

    @Override
    public UsageCaptureConfidence defaultConfidence() {
        return UsageCaptureConfidence.MEDIUM;
    }

    @Override
    public String providerKey() {
        return "MANUAL_CSV_IMPORT";
    }

    @Override
    public String importProviderKey() {
        return "csv";
    }

    @Override
    public String displayName() {
        return "CSV Usage Import";
    }

    @Override
    public UsageImportResult importUsage(String payload) {
        return importCsv(payload);
    }

    @Override
    public UsageImportResult previewUsage(String payload) {
        return previewCsv(payload);
    }

    public UsageImportResult importCsv(String csv) {
        return parseCsv(csv, true);
    }

    public UsageImportResult previewCsv(String csv) {
        return parseCsv(csv, false);
    }

    private UsageImportResult parseCsv(String csv, boolean persist) {
        List<UsageImportError> errors = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return new UsageImportResult(0, 1, List.of(new UsageImportError(0, "CSV body is required.")));
        }

        List<String> rows = csv.lines()
                .filter(line -> !line.isBlank())
                .toList();
        if (rows.isEmpty()) {
            return new UsageImportResult(0, 1, List.of(new UsageImportError(0, "CSV body is required.")));
        }

        List<String> headers;
        try {
            headers = csvRowParser.parseLine(rows.getFirst()).stream()
                    .map(this::normalizeHeader)
                    .toList();
        } catch (RuntimeException exception) {
            return new UsageImportResult(0, rows.size(), List.of(new UsageImportError(1, exception.getMessage())));
        }

        int imported = 0;
        for (int index = 1; index < rows.size(); index++) {
            int rowNumber = index + 1;
            try {
                Map<String, String> row = row(headers, csvRowParser.parseLine(rows.get(index)));
                processRow(row, persist);
                imported++;
            } catch (RuntimeException exception) {
                errors.add(new UsageImportError(rowNumber, exception.getMessage()));
            }
        }
        return new UsageImportResult(imported, errors.size(), errors);
    }

    private void processRow(Map<String, String> row, boolean persist) {
        String provider = required(row, "provider");
        String model = required(row, "model");
        String userKey = required(row, "userkey");
        String teamKey = value(row, "teamkey");
        OffsetDateTime timestamp = OffsetDateTime.parse(required(row, "requesttimestamp"));
        int promptTokens = integer(row, "prompttokens", 0);
        int completionTokens = integer(row, "completiontokens", 0);
        int totalTokens = integer(row, "totaltokens", promptTokens + completionTokens);
        if (totalTokens == 0 && promptTokens + completionTokens > 0) {
            totalTokens = promptTokens + completionTokens;
        }
        if (totalTokens < 0 || promptTokens < 0 || completionTokens < 0) {
            throw new IllegalArgumentException("Token counts cannot be negative.");
        }
        if (totalTokens > 0 && promptTokens == 0 && completionTokens == 0) {
            promptTokens = totalTokens;
        }
        Optional<BigDecimal> importedCost = decimal(row, "estimatedcostusd");
        BigDecimal cost = importedCost.orElse(pricingService.estimateCostUsd(provider, model, promptTokens, completionTokens));

        AttributionInference inference = attributionInferenceService.infer(value(row, "storykey"), value(row, "branch"));
        WorkItem story = findStory(inference.storyKey()).orElse(null);
        AttributionStatus attributionStatus = attributionStatusService.classify(inference.storyKey());
        String resolvedTeamKey = firstNonBlank(teamKey, story == null ? null : story.teamKey());
        if (resolvedTeamKey == null) {
            throw new IllegalArgumentException("teamKey is required when the row cannot be resolved to a known story.");
        }

        UsageEvent event = new UsageEvent(
                UUID.randomUUID(),
                provider,
                model,
                inference.storyKey(),
                firstNonBlank(story == null ? null : story.epicKey(), value(row, "epickey")),
                resolvedTeamKey,
                userKey,
                promptTokens,
                completionTokens,
                totalTokens,
                cost,
                integer(row, "latencyms", 0),
                timestamp,
                firstNonBlank(value(row, "environment"), "import"),
                firstNonBlank(story == null ? null : story.workType(), value(row, "worktype"), "UNKNOWN"),
                firstNonBlank(value(row, "requeststatus"), "SUCCEEDED"),
                attributionStatus,
                requestHash(row),
                source(),
                providerKey(),
                method(),
                defaultConfidence(),
                inference.source(),
                inference.confidence(),
                inference.source() == AttributionSource.INFERRED_BRANCH ? inference.storyKey() : null,
                inference.reason(),
                value(row, "repository"),
                value(row, "branch"),
                value(row, "commithash"),
                value(row, "initiativekey"),
                value(row, "initiativename"),
                false,
                null,
                null
        );
        if (persist) {
            usageEventRepository.save(event);
        }
    }

    private Map<String, String> row(List<String> headers, List<String> values) {
        if (values.size() > headers.size()) {
            throw new IllegalArgumentException("CSV row has more values than headers.");
        }
        Map<String, String> row = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            row.put(headers.get(index), index < values.size() ? values.get(index) : null);
        }
        return row;
    }

    private Optional<WorkItem> findStory(String storyKey) {
        try {
            return workTrackingProvider.findStoryByKey(storyKey);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String required(Map<String, String> row, String key) {
        String value = value(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required.");
        }
        return value;
    }

    private String value(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeHeader(String header) {
        return header.replace("_", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private int integer(Map<String, String> row, String key, int defaultValue) {
        String value = value(row, key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private Optional<BigDecimal> decimal(Map<String, String> row, String key) {
        String value = value(row, key);
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String requestHash(Map<String, String> row) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = row.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce("", (left, right) -> left + "|" + right);
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for imported usage hashing.", exception);
        }
    }
}
