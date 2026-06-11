package com.loop.rdas.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.loop.rdas.controller.CountryController;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/countries");
        return request;
    }

    @Test
    void handlesConstraintViolation_as400() {
        var response = handler.handleConstraintViolation(
                new ConstraintViolationException("isoCode invalid", Collections.emptySet()), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("isoCode invalid");
    }

    @Test
    void handlesTypeMismatch_as400() {
        var ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "size", null, new RuntimeException("nope"));
        var response = handler.handleTypeMismatch(ex, request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("size");
    }

    @Test
    void handlesBadSort_as400() {
        var ex = new PropertyReferenceException("bogus", TypeInformation.of(String.class), List.of());
        var response = handler.handleBadSort(ex, request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("bogus");
    }

    @Test
    void handlesIllegalArgument_as400() {
        var response = handler.handleIllegalArgument(
                new IllegalArgumentException("bad value"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesNotFound_as404() {
        var response = handler.handleNotFound(
                new CountryNotFoundException("missing"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("missing");
    }

    @Test
    void handlesSoap_as502() {
        var response = handler.handleSoap(
                new SoapIntegrationException("upstream down"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handlesGeneric_as500() {
        var response = handler.handleGeneric(new RuntimeException("boom"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handlesBodyValidation_collectsFieldErrors() throws Exception {
        Method method = CountryController.class.getMethod(
                "searchCountries", com.loop.rdas.dto.CountrySearchRequest.class,
                org.springframework.data.domain.Pageable.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BindingResult bindingResult =
                new BeanPropertyBindingResult(new com.loop.rdas.dto.CountrySearchRequest(), "filters");
        bindingResult.rejectValue("name", "Size", "too long");
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleBodyValidation(ex, request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("name");
    }
}
