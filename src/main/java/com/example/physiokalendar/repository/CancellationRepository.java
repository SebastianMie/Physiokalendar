package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.physiokalendar.entity.Cancellation;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, Long> {
}
