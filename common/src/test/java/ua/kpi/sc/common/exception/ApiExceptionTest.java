package ua.kpi.sc.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {

    @Test
    void apiException_holdsStatusAndMessage() {
        var ex = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "something broke");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getMessage()).isEqualTo("something broke");
    }

    @Test
    void badRequestException_mapsTo400() {
        var ex = new BadRequestException("bad input");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("bad input");
    }

    @Test
    void conflictException_mapsTo409() {
        var ex = new ConflictException("already exists");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("already exists");
    }

    @Test
    void forbiddenException_mapsTo403() {
        var ex = new ForbiddenException("no access");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).isEqualTo("no access");
    }

    @Test
    void unauthorizedException_mapsTo401() {
        var ex = new UnauthorizedException("not logged in");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getMessage()).isEqualTo("not logged in");
    }

    @Test
    void resourceNotFoundException_messageConstructor() {
        var ex = new ResourceNotFoundException("not here");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("not here");
    }

    @Test
    void resourceNotFoundException_resourceAndIdConstructor() {
        var ex = new ResourceNotFoundException("User", 42L);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("User not found with id: 42");
    }
}
