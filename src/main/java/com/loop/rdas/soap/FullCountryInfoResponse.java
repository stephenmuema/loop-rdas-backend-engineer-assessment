package com.loop.rdas.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * JAXB binding for the {@code FullCountryInfoResponse} SOAP response.
 */
@Getter
@Setter
@XmlRootElement(name = "FullCountryInfoResponse", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class FullCountryInfoResponse {

    @XmlElement(name = "FullCountryInfoResult", namespace = SoapNamespace.NS)
    private TCountryInfo fullCountryInfoResult;
}
