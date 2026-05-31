package com.acip.usage;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BranchStoryKeyParser {

    private static final Pattern STORY_KEY_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");

    public Optional<String> parse(String branch) {
        if (branch == null || branch.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = STORY_KEY_PATTERN.matcher(branch.toUpperCase());
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
