package com.loop.rdas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A reference-list entry (continent, currency or language) harvested from the
 * SOAP service and served from the local catalog. Stored in a single table
 * keyed by {@code (type, code)}.
 */
@Entity
@Table(
        name = "reference_item",
        uniqueConstraints = @UniqueConstraint(name = "ux_reference_type_code", columnNames = {"type", "code"}),
        indexes = @Index(name = "ix_reference_type", columnList = "type"))
@Getter
@Setter
public class ReferenceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private ReferenceType type;

    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;
}
