package com.gymplatform.modules.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID>,
        JpaSpecificationExecutor<Member> {
    Optional<Member> findByMemberNumber(String memberNumber);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByUserId(UUID userId);
    boolean existsByEmail(String email);
}
