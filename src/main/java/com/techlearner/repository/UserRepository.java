package com.techlearner.repository;

import com.techlearner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Named query example
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.role = 'ADMIN'")
    Optional<User> findAdminByUsername(String username);
}
