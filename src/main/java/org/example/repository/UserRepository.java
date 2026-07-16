package org.example.repository;

import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findAll();

    @Override
    @Query("SELECT count(u) FROM User u WHERE u.isDeleted = false")
    long count();

    @Override
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findById(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.pin = :pin AND u.isDeleted = false")
    Optional<User> findByPin(@Param("pin") String pin);

    @Query("SELECT count(u) FROM User u WHERE u.role = :role AND u.active = true AND u.isDeleted = false")
    long countByRoleAndActiveTrue(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true AND u.isDeleted = false")
    List<User> findByRoleAndActiveTrue(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isDeleted = false")
    List<User> findByRole(@Param("role") User.Role role);
}