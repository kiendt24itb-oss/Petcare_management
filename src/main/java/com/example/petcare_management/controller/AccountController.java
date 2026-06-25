package com.example.petcare_management.controller;

import com.example.petcare_management.dto.JwtResponse;
import com.example.petcare_management.dto.LoginRequest;
import com.example.petcare_management.dto.RegisterRequest;
import com.example.petcare_management.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // 1. API ĐĂNG KÝ: http://localhost:8080/api/accounts/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String result = accountService.register(request);
        return ResponseEntity.ok(result);
    }

    // 2. API ĐĂNG NHẬP: http://localhost:8080/api/accounts/login
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        JwtResponse response = accountService.login(request);
        return ResponseEntity.ok(response);
    }
}