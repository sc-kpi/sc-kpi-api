package ua.kpi.sc.document.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentControllerTest {

    @Test
    void canInstantiate() {
        assertThat(new DocumentController()).isNotNull();
    }
}
