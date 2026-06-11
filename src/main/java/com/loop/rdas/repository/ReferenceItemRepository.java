package com.loop.rdas.repository;

import com.loop.rdas.model.ReferenceItem;
import com.loop.rdas.model.ReferenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository over harvested reference lists (continents, currencies, languages).
 */
public interface ReferenceItemRepository extends JpaRepository<ReferenceItem, Long> {

    List<ReferenceItem> findByTypeOrderByNameAsc(ReferenceType type);

    Optional<ReferenceItem> findByTypeAndCodeIgnoreCase(ReferenceType type, String code);

    long countByType(ReferenceType type);
}
