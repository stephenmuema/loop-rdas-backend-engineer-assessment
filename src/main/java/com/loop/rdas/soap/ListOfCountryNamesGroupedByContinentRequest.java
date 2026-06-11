package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * JAXB binding for the parameterless
 * {@code ListOfCountryNamesGroupedByContinent} request. This is the seed
 * operation that enumerates every country (ISO code + name) for the catalog.
 */
@XmlRootElement(name = "ListOfCountryNamesGroupedByContinent", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class ListOfCountryNamesGroupedByContinentRequest {
}
