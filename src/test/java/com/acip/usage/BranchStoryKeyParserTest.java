package com.acip.usage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BranchStoryKeyParserTest {

    private final BranchStoryKeyParser parser = new BranchStoryKeyParser();

    @Test
    void extractsUppercaseStoryKeyFromCommonBranchName() {
        assertThat(parser.parse("feature/KAN-9-tax-service")).contains("KAN-9");
    }

    @Test
    void normalizesLowercaseStoryKey() {
        assertThat(parser.parse("bugfix/pay-1001-checkout")).contains("PAY-1001");
    }

    @Test
    void returnsEmptyWhenBranchHasNoStoryKey() {
        assertThat(parser.parse("main")).isEmpty();
    }
}
