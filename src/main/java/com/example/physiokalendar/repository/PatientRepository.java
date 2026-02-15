package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.physiokalendar.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query(value = "SELECT * FROM patient p WHERE p.first_name LIKE :firstName AND p.last_name LIKE :lastName LIMIT 1", nativeQuery = true)
    Patient findFirstByFirstNameAndLastNameLike(@Param("firstName") String firstName, @Param("lastName") String lastName);

    // Exact match for patient lookup (used during import to prevent duplicates)
    @Query("SELECT p FROM Patient p WHERE p.firstName = :firstName AND p.lastName = :lastName")
    Patient findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    java.util.Optional<Patient> findByEmail(String email);
}
