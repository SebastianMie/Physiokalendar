package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Therapist;


@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Long> {

    Therapist findByFirstName(String firstName);

    java.util.List<Therapist> findByIsActiveTrue();

    java.util.Optional<Therapist> findByEmail(String email);
}
