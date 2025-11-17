package com.trip.expense_splitter.controller;

import com.trip.expense_splitter.User;
import com.trip.expense_splitter.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // Using final keyword and Constructor Injection (Best Practice)
    private final UserRepository userRepository;

    // Constructor Injection (Spring automatically injects dependencies here)
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1. API to create a new User
    // Method: POST /api/users
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // 2. API to get all Users
    // Method: GET /api/users
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}