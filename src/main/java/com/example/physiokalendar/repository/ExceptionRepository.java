package com.example.physiokalendar.repository;

import com.example.physiokalendar.entity.Exception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExceptionRepository extends JpaRepository<Exception, Long> {
}
