package com.acip.usage;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsageEventControllerTest {

    private final UsageEventRepository repository = mock(UsageEventRepository.class);
    private final UsageEventController controller = new UsageEventController(repository);

    @Test
    void returnsEventByIdWhenPresent() {
        UUID id = UUID.randomUUID();
        UsageEvent event = mock(UsageEvent.class);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        assertThat(controller.eventById(id)).isSameAs(event);
    }

    @Test
    void throwsNotFoundWhenEventIsMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.eventById(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
