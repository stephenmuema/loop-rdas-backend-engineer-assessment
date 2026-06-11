package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for the {@code ListOfCurrenciesByNameResponse} SOAP response.
 */
@Getter
@Setter
@XmlRootElement(name = "ListOfCurrenciesByNameResponse", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class ListOfCurrenciesByNameResponse {

    @XmlElementWrapper(name = "ListOfCurrenciesByNameResult", namespace = SoapNamespace.NS)
    @XmlElement(name = "tCurrency", namespace = SoapNamespace.NS)
    private List<TCurrency> currencies = new ArrayList<>();
}
