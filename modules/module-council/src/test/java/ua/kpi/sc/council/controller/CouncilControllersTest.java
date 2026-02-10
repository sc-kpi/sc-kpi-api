package ua.kpi.sc.council.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CouncilControllersTest {

    @Test
    void canInstantiateDepartmentController() {
        assertThat(new DepartmentController()).isNotNull();
    }
}
