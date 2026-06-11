package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for a {@code tCountryCodeAndName} element — one country (ISO
 * code + name) within a continent grouping.
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class TCountryCodeAndName {

    @XmlElement(name = "sISOCode", namespace = SoapNamespace.NS)
    private String isoCode;

    @XmlElement(name = "sName", namespace = SoapNamespace.NS)
    private String name;
}
