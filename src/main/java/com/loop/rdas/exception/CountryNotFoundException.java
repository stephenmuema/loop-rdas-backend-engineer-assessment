package com.loop.rdas.exception;

/**
 * Thrown when a requested country (or currency grouping) is not present in the
 * materialized catalog. Mapped to HTTP 404 by the global handler.
 */
public class CountryNotFoundException extends RuntimeException {

    public CountryNotFoundException(String message) {
        super(message);
    }
}
