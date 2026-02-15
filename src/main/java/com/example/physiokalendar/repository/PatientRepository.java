package com.example.physiokalendar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.physiokalendar.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Paginated query for patients with optional search and BWO filter.
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "  LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(p.telefon) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:isBWO IS NULL OR p.isBWO = :isBWO)")
    Page<Patient> findPatientsFiltered(
            @Param("search") String search,
            @Param("isBWO") Boolean isBWO,
            Pageable pageable);

    @Query(value = "SELECT * FROM patient p WHERE p.first_name LIKE :firstName AND p.last_name LIKE :lastName LIMIT 1", nativeQuery = true)
    Patient findFirstByFirstNameAndLastNameLike(@Param("firstName") String firstName, @Param("lastName") String lastName);

    // Exact match for patient lookup (used during import to prevent duplicates)
    @Query("SELECT p FROM Patient p WHERE p.firstName = :firstName AND p.lastName = :lastName")
    Patient findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    java.util.Optional<Patient> findByEmail(String email);
}
