package com.gymplatform.modules.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndTenantId(String email, String tenantId);
    boolean existsByEmailAndTenantId(String email, String tenantId);
    Optional<User> findBySsoSubjectId(String ssoSubjectId);
}
