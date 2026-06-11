package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for a {@code tCurrency} element ({@code ListOfCurrenciesByName}).
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class TCurrency {

    @XmlElement(name = "sISOCode", namespace = SoapNamespace.NS)
    private String isoCode;

    @XmlElement(name = "sName", namespace = SoapNamespace.NS)
    private String name;
}
