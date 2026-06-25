package com.example.petcare_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 1. Thêm dòng import này

@SpringBootApplication
@EnableScheduling // 2. Thêm bùa kích hoạt chạy ngầm ở đây nè ní!
public class PetcareManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetcareManagementApplication.class, args);
    }

}