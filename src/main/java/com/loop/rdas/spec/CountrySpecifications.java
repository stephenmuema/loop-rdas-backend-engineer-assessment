package com.loop.rdas.spec;

import com.loop.rdas.dto.CountrySearchRequest;
import com.loop.rdas.model.Country;
import com.loop.rdas.model.CountryLanguage;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds a JPA {@link Specification} from a {@link CountrySearchRequest}.
 *
 * <p>Each supplied filter contributes one AND-ed predicate. Continent and
 * language each match either the code or the human-readable name, so callers
 * can filter by {@code AF}/{@code Africa} or {@code swa}/{@code Swahili}
 * interchangeably.</p>
 */
public final class CountrySpecifications {

    private CountrySpecifications() {
    }

    public static Specification<Country> fromRequest(CountrySearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + req.getName().trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(req.getContinent())) {
                String value = req.getContinent().trim().toLowerCase();
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("continentCode")), value),
                        cb.like(cb.lower(root.get("continentName")), "%" + value + "%")));
            }

            if (StringUtils.hasText(req.getCurrency())) {
                String value = req.getCurrency().trim().toLowerCase();
                predicates.add(cb.or(
                        cb.equal(cb.lower(root.get("currencyCode")), value),
                        cb.like(cb.lower(root.get("currencyName")), "%" + value + "%")));
            }

            if (StringUtils.hasText(req.getLanguage())) {
                String value = req.getLanguage().trim().toLowerCase();
                // Join the embedded language collection; ensure DISTINCT rows.
                if (query != null) {
                    query.distinct(true);
                }
                Join<Country, CountryLanguage> lang = root.join("languages");
                predicates.add(cb.or(
                        cb.equal(cb.lower(lang.get("code")), value),
                        cb.like(cb.lower(lang.get("name")), "%" + value + "%")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
