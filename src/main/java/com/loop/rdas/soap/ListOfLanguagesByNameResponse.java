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
 * JAXB binding for the {@code ListOfLanguagesByNameResponse} SOAP response.
 */
@Getter
@Setter
@XmlRootElement(name = "ListOfLanguagesByNameResponse", namespace = SoapNamespace.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class ListOfLanguagesByNameResponse {

    @XmlElementWrapper(name = "ListOfLanguagesByNameResult", namespace = SoapNamespace.NS)
    @XmlElement(name = "tLanguage", namespace = SoapNamespace.NS)
    private List<TLanguage> languages = new ArrayList<>();
}
