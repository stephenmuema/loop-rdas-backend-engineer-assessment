package com.loop.rdas.dto;

import com.loop.rdas.model.ReferenceItem;

/**
 * A reference-list entry (continent, currency or language).
 */
public record ReferenceItemResponse(String code, String name) {

    public static ReferenceItemResponse from(ReferenceItem item) {
        return new ReferenceItemResponse(item.getCode(), item.getName());
    }
}
