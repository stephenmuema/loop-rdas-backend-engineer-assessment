package com.loop.rdas.exception;

/**
 * Thrown when an outbound call to the CountryInfo SOAP service fails (after
 * retries / while the circuit breaker is open). Surfaces only from the
 * harvester path; mapped to HTTP 502 if it ever reaches the web layer.
 */
public class SoapIntegrationException extends RuntimeException {

    public SoapIntegrationException(String message) {
        super(message);
    }

    public SoapIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
