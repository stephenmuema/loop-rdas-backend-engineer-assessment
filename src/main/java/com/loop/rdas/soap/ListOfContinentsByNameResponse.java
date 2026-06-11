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
 * JAXB binding for the {@code ListOfContinentsByNameResponse} SOAP response.
 */
@Getter
@Setter
@XmlRootElement(name = "ListOfContinentsByNameResponse", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class ListOfContinentsByNameResponse {

    @XmlElementWrapper(name = "ListOfContinentsByNameResult", namespace = SoapNamespace.NS)
    @XmlElement(name = "tContinent", namespace = SoapNamespace.NS)
    private List<TContinent> continents = new ArrayList<>();
}
