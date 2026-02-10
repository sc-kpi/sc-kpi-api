package ua.kpi.sc.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ServletWebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler("https://api.example.com");

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/v1/test");
        webRequest = new ServletWebRequest(mockRequest);
    }

    @Test
    void handleApiException_returnsProblemDetailWithCorrectStatus() {
        var ex = new BadRequestException("invalid field");

        ResponseEntity<Object> response = handler.handleApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("invalid field");
        assertThat(body.getTitle()).isEqualTo("Bad Request");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/400"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    void handleGenericException_returns500WithTypeAndInstance() {
        var ex = new RuntimeException("unexpected");

        ResponseEntity<Object> response = handler.handleGenericException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Internal Server Error");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/500"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleMethodArgumentNotValid_returnsValidationErrorsWithInstance() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "must not be blank"));

        var parameter = new MethodParameter(
                Object.class.getMethod("toString"), -1);
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Validation Error");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/validation"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));

        var errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().get("field")).isEqualTo("name");
        assertThat(errors.getFirst().get("message")).isEqualTo("must not be blank");
    }

    @Test
    void handleHttpMessageNotReadable_returns400WithTypeAndInstance() {
        var ex = new HttpMessageNotReadableException("Could not read JSON", (Throwable) null, null);

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Bad Request");
        assertThat(body.getDetail()).isEqualTo("Malformed request body");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/400"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    void handleExceptionInternal_enrichesStandardSpringExceptions() throws Exception {
        var ex = new NoResourceFoundException(HttpMethod.GET, "/static/missing.js", "static/missing.js");

        ResponseEntity<Object> response = handler.handleException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/404"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    void handleApiException_withNonServletWebRequest_setsNoInstance() {
        var ex = new BadRequestException("invalid field");
        WebRequest nonServletRequest = mock(WebRequest.class);

        ResponseEntity<Object> response = handler.handleApiException(ex, nonServletRequest);

        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getInstance()).isNull();
    }

    @Test
    void handleMissingServletRequestParameter_returns400WithParameterName() {
        var ex = new MissingServletRequestParameterException("page", "int");

        ResponseEntity<Object> response = handler.handleMissingServletRequestParameter(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Bad Request");
        assertThat(body.getDetail()).isEqualTo("Required parameter 'page' is missing");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/400"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    void handleHttpRequestMethodNotSupported_returns405WithMethodName() {
        var ex = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<Object> response = handler.handleHttpRequestMethodNotSupported(
                ex, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Method Not Allowed");
        assertThat(body.getDetail()).isEqualTo("HTTP method 'PATCH' is not supported for this endpoint");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/405"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    void handleTypeMismatch_returns400WithExpectedType() {
        var ex = new TypeMismatchException("abc", Integer.class);

        ResponseEntity<Object> response = handler.handleTypeMismatch(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Bad Request");
        assertThat(body.getType()).isEqualTo(URI.create("https://api.example.com/errors/400"));
        assertThat(body.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }
}
