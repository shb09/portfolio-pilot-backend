package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByVerificationTokenHash(String hash);

    Optional<User> findByResetTokenHash(String hash);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByEmailVerified(boolean verified);

    long countByRole(Role role);

    List<User> findTop5ByOrderByCreatedAtDesc();
}
