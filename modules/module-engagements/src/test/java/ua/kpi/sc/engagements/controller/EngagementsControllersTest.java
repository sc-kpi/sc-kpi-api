package ua.kpi.sc.engagements.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EngagementsControllersTest {

    @Test
    void canInstantiateClubController() {
        assertThat(new ClubController()).isNotNull();
    }

    @Test
    void canInstantiateProjectController() {
        assertThat(new ProjectController()).isNotNull();
    }
}
