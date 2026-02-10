package ua.kpi.sc.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserControllerTest {

    @Test
    void canInstantiate() {
        assertThat(new UserController()).isNotNull();
    }
}
