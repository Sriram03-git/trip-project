package com.trip.expense_splitter.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.expense_splitter.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // This method is used by the security layer to find users by their email (username)
    Optional<User> findByEmail(String email);
}