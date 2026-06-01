package com.acip.capture;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsageImportControllerTest {

    private final UsageImportProvider csvProvider = mock(UsageImportProvider.class);

    @Test
    void importsThroughProviderRegistry() {
        UsageImportController controller = controller();
        UsageImportResult expected = new UsageImportResult(1, 0, List.of());
        when(csvProvider.importUsage("payload")).thenReturn(expected);

        assertThat(controller.importUsage("csv", "payload")).isSameAs(expected);
        assertThat(controller.importCsv("payload")).isSameAs(expected);
    }

    @Test
    void previewsThroughProviderRegistry() {
        UsageImportController controller = controller();
        UsageImportResult expected = new UsageImportResult(2, 1, List.of(new UsageImportError(3, "provider is required.")));
        when(csvProvider.previewUsage("payload")).thenReturn(expected);

        assertThat(controller.previewUsage("csv", "payload")).isSameAs(expected);
    }

    @Test
    void rejectsUnknownProvider() {
        UsageImportController controller = controller();
        assertThatThrownBy(() -> controller.importUsage("unknown", "payload"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usage import provider not found");
    }

    @Test
    void downloadsSampleCsv() {
        UsageImportController controller = controller();
        ResponseEntity<String> response = controller.sample("advanced");

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("acip-advanced-usage-sample.csv");
        assertThat(response.getBody()).contains("provider,model,storyKey");
    }

    private UsageImportController controller() {
        when(csvProvider.importProviderKey()).thenReturn("csv");
        return new UsageImportController(List.of(csvProvider), new UsageImportSampleService());
    }
}
