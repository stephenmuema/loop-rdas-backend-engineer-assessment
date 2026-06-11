package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for a {@code tContinent} element ({@code ListOfContinentsByName}).
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class TContinent {

    @XmlElement(name = "sCode", namespace = SoapNamespace.NS)
    private String code;

    @XmlElement(name = "sName", namespace = SoapNamespace.NS)
    private String name;
}
