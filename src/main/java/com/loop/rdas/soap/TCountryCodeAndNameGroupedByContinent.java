package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for a {@code tCountryCodeAndNameGroupedByContinent} element: a
 * continent plus the list of countries within it.
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class TCountryCodeAndNameGroupedByContinent {

    @XmlElement(name = "Continent", namespace = SoapNamespace.NS)
    private TContinent continent;

    @XmlElementWrapper(name = "CountryCodeAndNames", namespace = SoapNamespace.NS)
    @XmlElement(name = "tCountryCodeAndName", namespace = SoapNamespace.NS)
    private List<TCountryCodeAndName> countries = new ArrayList<>();
}
