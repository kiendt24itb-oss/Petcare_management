package com.example.petcare_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @PostMapping("/avatar")
    public ResponseEntity<String> uploadAvatar(
            @RequestParam("file") MultipartFile file) {

        try {

            String fileName =
                    System.currentTimeMillis()
                            + "_"
                            + file.getOriginalFilename();

            Path uploadPath =
                    Paths.get("uploads/avatarUsers");

            Files.createDirectories(uploadPath);

            Path filePath =
                    uploadPath.resolve(fileName);

            Files.write(filePath, file.getBytes());

            return ResponseEntity.ok(
                    "/uploads/avatarUsers/" + fileName
            );

        } catch (Exception e) {
            e.printStackTrace(); // 🌟 Thêm dòng này để nhìn thấy lỗi thật ở Console Java
            return ResponseEntity.badRequest().body("Upload thất bại: " + e.getMessage());
        }
    }
}
