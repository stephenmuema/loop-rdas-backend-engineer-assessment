package com.loop.rdas.controller;

import com.loop.rdas.dto.ReferenceItemResponse;
import com.loop.rdas.model.ReferenceType;
import com.loop.rdas.service.CountryQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference-list APIs (continents, currencies, languages), served from the
 * local catalog.
 */
@RestController
@RequestMapping("/api/v1")
public class ReferenceController {

    private final CountryQueryService queryService;

    public ReferenceController(CountryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/continents")
    public List<ReferenceItemResponse> continents() {
        return queryService.getReferenceItems(ReferenceType.CONTINENT);
    }

    @GetMapping("/currencies")
    public List<ReferenceItemResponse> currencies() {
        return queryService.getReferenceItems(ReferenceType.CURRENCY);
    }

    @GetMapping("/languages")
    public List<ReferenceItemResponse> languages() {
        return queryService.getReferenceItems(ReferenceType.LANGUAGE);
    }
}
