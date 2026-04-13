package org.smm.archetype.client.log.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogMarkers")
class LogMarkersUTest {

    @Test
    @DisplayName("应包含 5 个业务日志标记")
    void should_contain_five_markers() {
        assertThat(LogMarkers.ORDER).isNotNull();
        assertThat(LogMarkers.PAYMENT).isNotNull();
        assertThat(LogMarkers.USER).isNotNull();
        assertThat(LogMarkers.SECURITY).isNotNull();
        assertThat(LogMarkers.AUDIT).isNotNull();
    }
}
