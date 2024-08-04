package com.example.physiokalendar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Therapist;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Long> {
    @Query("SELECT t FROM Therapist t WHERE t.name LIKE %:name%")
    List<Therapist> findByNameContaining(String name);

    public Object findById(String id);

}
