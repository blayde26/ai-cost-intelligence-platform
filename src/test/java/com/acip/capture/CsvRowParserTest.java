package com.acip.capture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvRowParserTest {

    private final CsvRowParser parser = new CsvRowParser();

    @Test
    void parsesSimpleCsvLine() {
        assertThat(parser.parseLine("provider,model,totalTokens"))
                .containsExactly("provider", "model", "totalTokens");
    }

    @Test
    void parsesQuotedCommasAndEscapedQuotes() {
        assertThat(parser.parseLine("OPENAI,\"gpt, imported\",\"said \"\"hello\"\"\""))
                .containsExactly("OPENAI", "gpt, imported", "said \"hello\"");
    }

    @Test
    void rejectsUnterminatedQuotedValue() {
        assertThatThrownBy(() -> parser.parseLine("OPENAI,\"gpt-4o-mini"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unterminated");
    }
}
