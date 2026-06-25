package com.example.petcare_management.controller;

import com.example.petcare_management.dto.PetRequest;
import com.example.petcare_management.dto.PetResponse;
import com.example.petcare_management.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /**
     * 👑 API MỚI: Lấy TOÀN BỘ danh sách thú cưng để hiển thị bên trang quản lý khách hàng
     * Vì nằm dưới nhánh /api/pets và không chứa chữ "admin", nó sẽ dùng chung cấu hình authenticated() cũ,
     * cam đoan không bao giờ lo bị dính lỗi 403 Forbidden nữa!
     * GET http://localhost:8080/api/pets/all-list
     */
    @GetMapping("/all-list")
    public ResponseEntity<List<PetResponse>> getAllPetsForAdmin() {
        return ResponseEntity.ok(petService.getAllPetsForAdmin());
    }

    /**
     * ➕ API: Thêm mới 1 bé thú cưng
     * POST http://localhost:8080/api/pets
     */
    @PostMapping
    public ResponseEntity<PetResponse> addPet(Authentication authentication, @RequestBody PetRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(petService.addPet(username, request));
    }

    /**
     * 📝 API: Chỉnh sửa thông tin thú cưng
     * PUT http://localhost:8080/api/pets/{petId}
     */
    @PutMapping("/{petId}")
    public ResponseEntity<PetResponse> updatePet(
            Authentication authentication,
            @PathVariable Integer petId,
            @RequestBody PetRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(petService.updatePet(username, petId, request));
    }

    /**
     * 📋 API: Lấy danh sách toàn bộ thú cưng của khách hàng đang đăng nhập để hiển thị lên bảng (Table)
     * GET http://localhost:8080/api/pets
     */
    @GetMapping
    public ResponseEntity<List<PetResponse>> getMyPets(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(petService.getMyPets(username));
    }

    /**
     * 🔍 API: Xem chi tiết thông tin của 1 bé Pet (Dùng khi bấm nút "Xem/Sửa")
     * GET http://localhost:8080/api/pets/{petId}
     */
    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPetDetail(Authentication authentication, @PathVariable Integer petId) {
        String username = authentication.getName();
        return ResponseEntity.ok(petService.getPetDetail(username, petId));
    }

    /**
     * 🔮 API BỔ SUNG: Lấy mã code tự sinh tiếp theo để hiển thị xem trước lên Form
     * GET http://localhost:8080/api/pets/next-code
     */
    @GetMapping("/next-code")
    public ResponseEntity<String> getNextPetCode() {
        String latestCode = petService.findLatestPetCode();
        String nextCode = com.example.petcare_management.util.CodeGenerator.generateNextCode(latestCode, "TC");
        return ResponseEntity.ok(nextCode);
    }

    /**
     * 🗑️ API BỔ SUNG: Xóa thú cưng khỏi hệ thống
     * DELETE http://localhost:8080/api/pets/{petId}
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<String> deletePet(Authentication authentication, @PathVariable Integer petId) {
        String username = authentication.getName();
        petService.deletePet(username, petId);
        return ResponseEntity.ok("Xóa thành công!");
    }
}