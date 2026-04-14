package org.smm.archetype.shared.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogMarkers")
class LogMarkersUTest {

    @Test
    @DisplayName("应包含 4 个业务日志标记：USER、SECURITY、AUDIT、SYSTEM")
    void should_contain_four_markers() {
        assertThat(LogMarkers.USER).isNotNull();
        assertThat(LogMarkers.SECURITY).isNotNull();
        assertThat(LogMarkers.AUDIT).isNotNull();
        assertThat(LogMarkers.SYSTEM).isNotNull();
    }

    @Test
    @DisplayName("每个 Marker 的 name 应正确")
    void should_marker_names_be_correct() {
        assertThat(LogMarkers.USER.getName()).isEqualTo("USER");
        assertThat(LogMarkers.SECURITY.getName()).isEqualTo("SECURITY");
        assertThat(LogMarkers.AUDIT.getName()).isEqualTo("AUDIT");
        assertThat(LogMarkers.SYSTEM.getName()).isEqualTo("SYSTEM");
    }

}
