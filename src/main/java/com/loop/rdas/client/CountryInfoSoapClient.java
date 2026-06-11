package com.loop.rdas.client;

import com.loop.rdas.exception.SoapIntegrationException;
import com.loop.rdas.soap.FullCountryInfoRequest;
import com.loop.rdas.soap.FullCountryInfoResponse;
import com.loop.rdas.soap.ListOfContinentsByNameRequest;
import com.loop.rdas.soap.ListOfContinentsByNameResponse;
import com.loop.rdas.soap.ListOfCountryNamesGroupedByContinentRequest;
import com.loop.rdas.soap.ListOfCountryNamesGroupedByContinentResponse;
import com.loop.rdas.soap.ListOfCurrenciesByNameRequest;
import com.loop.rdas.soap.ListOfCurrenciesByNameResponse;
import com.loop.rdas.soap.ListOfLanguagesByNameRequest;
import com.loop.rdas.soap.ListOfLanguagesByNameResponse;
import com.loop.rdas.soap.TContinent;
import com.loop.rdas.soap.TCountryCodeAndNameGroupedByContinent;
import com.loop.rdas.soap.TCountryInfo;
import com.loop.rdas.soap.TCurrency;
import com.loop.rdas.soap.TLanguage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Resilient SOAP client for the public CountryInfo service. This is the only
 * component in RDAS that performs outbound SOAP calls — exclusively from the
 * background harvester, never on a request thread.
 *
 * <p>Each call is wrapped, from outermost to innermost aspect, with:</p>
 * <ol>
 *   <li>{@link CircuitBreaker} (aspect order 1) — once the breaker opens, calls
 *       fast-fail without consuming the retry budget;</li>
 *   <li>{@link Retry} (aspect order 2) — up to 3 attempts with exponential
 *       backoff;</li>
 *   <li>{@link RateLimiter} — caps outbound throughput below the provider's
 *       100 requests/minute quota; every physical attempt acquires a permit.</li>
 * </ol>
 *
 * <p>When a call ultimately fails (or the breaker is open) the matching
 * {@code *Fallback} method translates the failure into a
 * {@link SoapIntegrationException}, which the harvester handles by aborting the
 * refresh and leaving the existing catalog in place.</p>
 */
@Component
public class CountryInfoSoapClient {

    private static final Logger log = LoggerFactory.getLogger(CountryInfoSoapClient.class);
    static final String BACKEND = "countryInfoSoap";

    private final WebServiceTemplate webServiceTemplate;

    public CountryInfoSoapClient(WebServiceTemplate countryInfoWebServiceTemplate) {
        this.webServiceTemplate = countryInfoWebServiceTemplate;
    }

    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "listContinentsFallback")
    public List<TContinent> listContinents() {
        log.debug("SOAP ListOfContinentsByName request");
        ListOfContinentsByNameResponse response = (ListOfContinentsByNameResponse)
                webServiceTemplate.marshalSendAndReceive(new ListOfContinentsByNameRequest());
        return response != null ? response.getContinents() : List.of();
    }

    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "listCurrenciesFallback")
    public List<TCurrency> listCurrencies() {
        log.debug("SOAP ListOfCurrenciesByName request");
        ListOfCurrenciesByNameResponse response = (ListOfCurrenciesByNameResponse)
                webServiceTemplate.marshalSendAndReceive(new ListOfCurrenciesByNameRequest());
        return response != null ? response.getCurrencies() : List.of();
    }

    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "listLanguagesFallback")
    public List<TLanguage> listLanguages() {
        log.debug("SOAP ListOfLanguagesByName request");
        ListOfLanguagesByNameResponse response = (ListOfLanguagesByNameResponse)
                webServiceTemplate.marshalSendAndReceive(new ListOfLanguagesByNameRequest());
        return response != null ? response.getLanguages() : List.of();
    }

    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "listGroupedFallback")
    public List<TCountryCodeAndNameGroupedByContinent> listCountriesGroupedByContinent() {
        log.debug("SOAP ListOfCountryNamesGroupedByContinent request");
        ListOfCountryNamesGroupedByContinentResponse response = (ListOfCountryNamesGroupedByContinentResponse)
                webServiceTemplate.marshalSendAndReceive(new ListOfCountryNamesGroupedByContinentRequest());
        return response != null ? response.getGroups() : List.of();
    }

    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND, fallbackMethod = "getFullCountryInfoFallback")
    public TCountryInfo getFullCountryInfo(String isoCode) {
        log.debug("SOAP FullCountryInfo request: isoCode='{}'", isoCode);
        FullCountryInfoResponse response = (FullCountryInfoResponse)
                webServiceTemplate.marshalSendAndReceive(new FullCountryInfoRequest(isoCode));
        if (response == null || response.getFullCountryInfoResult() == null) {
            throw new SoapIntegrationException(
                    "FullCountryInfo returned an empty result for ISO code '" + isoCode + "'");
        }
        return response.getFullCountryInfoResult();
    }

    // --- Fallbacks (package-private for testability) -------------------------

    @SuppressWarnings("unused")
    List<TContinent> listContinentsFallback(Throwable t) {
        throw fail("ListOfContinentsByName", t);
    }

    @SuppressWarnings("unused")
    List<TCurrency> listCurrenciesFallback(Throwable t) {
        throw fail("ListOfCurrenciesByName", t);
    }

    @SuppressWarnings("unused")
    List<TLanguage> listLanguagesFallback(Throwable t) {
        throw fail("ListOfLanguagesByName", t);
    }

    @SuppressWarnings("unused")
    List<TCountryCodeAndNameGroupedByContinent> listGroupedFallback(Throwable t) {
        throw fail("ListOfCountryNamesGroupedByContinent", t);
    }

    @SuppressWarnings("unused")
    TCountryInfo getFullCountryInfoFallback(String isoCode, Throwable t) {
        throw fail("FullCountryInfo(" + isoCode + ")", t);
    }

    private SoapIntegrationException fail(String operation, Throwable t) {
        log.error("SOAP {} failed: {}", operation, t.toString());
        return new SoapIntegrationException(
                "CountryInfo SOAP operation '" + operation + "' failed", t);
    }
}
