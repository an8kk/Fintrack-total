package com.fintrack.backend.repository;

import java.util.Optional;
import com.fintrack.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findBySaltEdgeCustomerId(String saltEdgeCustomerId);

    Optional<User> findByUsername(String username);
}