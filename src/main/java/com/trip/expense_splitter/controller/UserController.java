package com.trip.expense_splitter.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trip.expense_splitter.User;
import com.trip.expense_splitter.repository.UserRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // The incoming user object from the frontend has a null password.
        // We must set a non-null password to satisfy the database constraint.
        // A random UUID is a good temporary placeholder.
        user.setPassword(UUID.randomUUID().toString());

        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}