package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.example.physiokalendar.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    User findByTherapistId(Long therapistId);
}
