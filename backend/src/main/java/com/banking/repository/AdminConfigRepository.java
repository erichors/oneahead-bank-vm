package com.banking.repository;

import com.banking.model.AdminConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminConfigRepository extends JpaRepository<AdminConfig, Long> {
    Optional<AdminConfig> findByConfigKey(String configKey);
}
