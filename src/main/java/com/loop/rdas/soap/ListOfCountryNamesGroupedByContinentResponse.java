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
 * JAXB binding for the {@code ListOfCountryNamesGroupedByContinentResponse}.
 */
@Getter
@Setter
@XmlRootElement(name = "ListOfCountryNamesGroupedByContinentResponse", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class ListOfCountryNamesGroupedByContinentResponse {

    @XmlElementWrapper(name = "ListOfCountryNamesGroupedByContinentResult", namespace = SoapNamespace.NS)
    @XmlElement(name = "tCountryCodeAndNameGroupedByContinent", namespace = SoapNamespace.NS)
    private List<TCountryCodeAndNameGroupedByContinent> groups = new ArrayList<>();
}
