package de.likeherotozero.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmissionRepository extends JpaRepository<EmissionRecord, Long> {
    @EntityGraph(attributePaths = "country")
    Optional<EmissionRecord> findFirstByCountry_CodeOrderByYearDesc(String countryCode);

    @EntityGraph(attributePaths = "country")
    Optional<EmissionRecord> findByCountry_CodeAndYear(String countryCode, int year);

    boolean existsByCountry_CodeAndYear(String countryCode, int year);
}
