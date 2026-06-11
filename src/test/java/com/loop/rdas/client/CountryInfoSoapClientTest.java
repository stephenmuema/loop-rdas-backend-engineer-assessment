package com.loop.rdas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.loop.rdas.exception.SoapIntegrationException;
import com.loop.rdas.soap.FullCountryInfoResponse;
import com.loop.rdas.soap.ListOfContinentsByNameResponse;
import com.loop.rdas.soap.ListOfCountryNamesGroupedByContinentResponse;
import com.loop.rdas.soap.ListOfCurrenciesByNameResponse;
import com.loop.rdas.soap.ListOfLanguagesByNameResponse;
import com.loop.rdas.soap.TContinent;
import com.loop.rdas.soap.TCountryInfo;
import com.loop.rdas.soap.TCurrency;
import com.loop.rdas.soap.TLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;

@ExtendWith(MockitoExtension.class)
class CountryInfoSoapClientTest {

    @Mock
    WebServiceTemplate webServiceTemplate;

    @InjectMocks
    CountryInfoSoapClient client;

    @Test
    void listContinents_mapsResponse() {
        ListOfContinentsByNameResponse response = new ListOfContinentsByNameResponse();
        TContinent af = new TContinent();
        af.setCode("AF");
        af.setName("Africa");
        response.getContinents().add(af);
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(response);

        assertThat(client.listContinents()).hasSize(1);
        assertThat(client.listContinents().get(0).getCode()).isEqualTo("AF");
    }

    @Test
    void listCurrencies_mapsResponse() {
        ListOfCurrenciesByNameResponse response = new ListOfCurrenciesByNameResponse();
        TCurrency c = new TCurrency();
        c.setIsoCode("KES");
        c.setName("Kenyan Shilling");
        response.getCurrencies().add(c);
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(response);

        assertThat(client.listCurrencies()).extracting(TCurrency::getIsoCode).containsExactly("KES");
    }

    @Test
    void listLanguages_mapsResponse() {
        ListOfLanguagesByNameResponse response = new ListOfLanguagesByNameResponse();
        TLanguage l = new TLanguage();
        l.setIsoCode("swa");
        l.setName("Swahili");
        response.getLanguages().add(l);
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(response);

        assertThat(client.listLanguages()).extracting(TLanguage::getName).containsExactly("Swahili");
    }

    @Test
    void listGrouped_returnsEmptyOnNullResponse() {
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(null);
        assertThat(client.listCountriesGroupedByContinent()).isEmpty();
    }

    @Test
    void getFullCountryInfo_mapsResult() {
        FullCountryInfoResponse response = new FullCountryInfoResponse();
        TCountryInfo info = new TCountryInfo();
        info.setIsoCode("KE");
        info.setName("Kenya");
        response.setFullCountryInfoResult(info);
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(response);

        assertThat(client.getFullCountryInfo("KE").getName()).isEqualTo("Kenya");
    }

    @Test
    void getFullCountryInfo_throwsOnEmptyResult() {
        when(webServiceTemplate.marshalSendAndReceive(any())).thenReturn(new FullCountryInfoResponse());
        assertThatThrownBy(() -> client.getFullCountryInfo("ZZ"))
                .isInstanceOf(SoapIntegrationException.class);
    }

    @Test
    void fallbacks_translateToSoapIntegrationException() {
        RuntimeException cause = new RuntimeException("boom");
        assertThatThrownBy(() -> client.listContinentsFallback(cause))
                .isInstanceOf(SoapIntegrationException.class);
        assertThatThrownBy(() -> client.listCurrenciesFallback(cause))
                .isInstanceOf(SoapIntegrationException.class);
        assertThatThrownBy(() -> client.listLanguagesFallback(cause))
                .isInstanceOf(SoapIntegrationException.class);
        assertThatThrownBy(() -> client.listGroupedFallback(cause))
                .isInstanceOf(SoapIntegrationException.class);
        assertThatThrownBy(() -> client.getFullCountryInfoFallback("KE", cause))
                .isInstanceOf(SoapIntegrationException.class);
    }
}
