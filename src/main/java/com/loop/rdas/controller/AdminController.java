package com.loop.rdas.controller;

import com.loop.rdas.dto.CatalogStatusResponse;
import com.loop.rdas.service.CatalogHarvestService;
import com.loop.rdas.service.CountryQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operational APIs for the catalog: inspect status and trigger an out-of-band
 * refresh.
 *
 * <p>In production these would sit behind an authenticated admin scope /
 * network policy; that is called out in the README as a deliberate boundary.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/catalog")
public class AdminController {

    private final CatalogHarvestService harvestService;
    private final CountryQueryService queryService;

    public AdminController(CatalogHarvestService harvestService, CountryQueryService queryService) {
        this.harvestService = harvestService;
        this.queryService = queryService;
    }

    @GetMapping("/status")
    public CatalogStatusResponse status() {
        return queryService.getCatalogStatus();
    }

    /**
     * Triggers an asynchronous full refresh and returns 202 with the current
     * status. The harvest runs in the background and respects the SOAP rate
     * limit; concurrent triggers are coalesced (single-flight).
     */
    @PostMapping("/refresh")
    public ResponseEntity<CatalogStatusResponse> refresh() {
        harvestService.refreshAsync();
        return ResponseEntity.accepted().body(queryService.getCatalogStatus());
    }
}
