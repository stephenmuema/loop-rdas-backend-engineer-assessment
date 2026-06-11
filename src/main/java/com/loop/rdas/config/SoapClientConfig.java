package com.loop.rdas.config;

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
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

/**
 * Wires the Spring-WS {@link WebServiceTemplate} used to call the CountryInfo
 * SOAP service.
 *
 * <p>The marshaller is bound to every request/response payload class RDAS uses
 * to build its catalog. The message sender enforces explicit connect/read
 * timeouts so a slow upstream cannot exhaust the harvester threads. Full SOAP
 * envelope logging is available at DEBUG via Spring-WS message tracing — set
 * {@code org.springframework.ws.client.MessageTracing} to DEBUG/TRACE.</p>
 */
@Configuration
public class SoapClientConfig {

    @Value("${rdas.soap.endpoint:http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso}")
    private String endpoint;

    @Value("${rdas.soap.connect-timeout-ms:5000}")
    private long connectTimeoutMs;

    @Value("${rdas.soap.read-timeout-ms:10000}")
    private long readTimeoutMs;

    @Bean
    public Jaxb2Marshaller countryInfoMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                FullCountryInfoRequest.class,
                FullCountryInfoResponse.class,
                ListOfContinentsByNameRequest.class,
                ListOfContinentsByNameResponse.class,
                ListOfCurrenciesByNameRequest.class,
                ListOfCurrenciesByNameResponse.class,
                ListOfLanguagesByNameRequest.class,
                ListOfLanguagesByNameResponse.class,
                ListOfCountryNamesGroupedByContinentRequest.class,
                ListOfCountryNamesGroupedByContinentResponse.class);
        return marshaller;
    }

    @Bean
    public HttpUrlConnectionMessageSender countryInfoMessageSender() {
        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(connectTimeoutMs));
        sender.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return sender;
    }

    @Bean
    public WebServiceTemplate countryInfoWebServiceTemplate(Jaxb2Marshaller countryInfoMarshaller,
                                                            HttpUrlConnectionMessageSender countryInfoMessageSender) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(countryInfoMarshaller);
        template.setUnmarshaller(countryInfoMarshaller);
        template.setDefaultUri(endpoint);
        template.setMessageSender(countryInfoMessageSender);
        return template;
    }
}
