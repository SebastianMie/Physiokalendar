package com.example.physiokalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.physiokalendar.entity.AppSetting;

import java.util.Optional;
import java.util.List;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    /**
     * Find setting by key
     */
    Optional<AppSetting> findByKey(String key);

    /**
     * Find all settings by category
     */
    List<AppSetting> findByCategory(String category);

    /**
     * Check if a setting key exists
     */
    boolean existsByKey(String key);
}
