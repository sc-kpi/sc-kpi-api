package ua.kpi.sc.audit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditControllerTest {

    @Test
    void canInstantiate() {
        assertThat(new AuditController()).isNotNull();
    }
}
