package com.example.petcare_management.controller;

import com.example.petcare_management.dto.ServiceRequest;
import com.example.petcare_management.dto.ServiceResponse;
import com.example.petcare_management.entity.enums.ServiceCategory;
import com.example.petcare_management.service.ServiceEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ServiceEntityController {

    private final ServiceEntityService serviceEntityService;

    // 📥 1. Admin thêm mới dịch vụ (Request đã có roomCode, xử lý ngầm ở Service)
    @PostMapping("/create")
    public ResponseEntity<ServiceResponse> createService(@RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceEntityService.createService(request));
    }

    // 📝 2. Admin cập nhật thông tin/giá cả dịch vụ
    @PutMapping("/update/{serviceId}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Integer serviceId,
            @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceEntityService.updateService(serviceId, request));
    }

    // 📋 3. Admin lấy TOÀN BỘ dịch vụ (Response trả về cấu trúc phẳng có roomCode cho JS render)
    @GetMapping("/all")
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        return ResponseEntity.ok(serviceEntityService.getAllServices());
    }

    // 🔍 4. Khách hàng/Staff lọc nhanh danh sách dịch vụ ĐANG MỞ theo danh mục
    @GetMapping("/active")
    public ResponseEntity<List<ServiceResponse>> getActiveServices(@RequestParam ServiceCategory category) {
        return ResponseEntity.ok(serviceEntityService.getActiveServicesByCategory(category));
    }

    /**
     * 🔄 5. Admin Bật/Tắt trạng thái kinh doanh của dịch vụ
     * ĐÃ SỬA: Trả về đối tượng JSON sạch { "message": "..." } thay vì chuỗi Plain Text để tránh lỗi Fetch API ở FE
     */
    @PatchMapping("/toggle/{serviceId}")
    public ResponseEntity<Map<String, String>> toggleServiceStatus(@PathVariable Integer serviceId) {
        serviceEntityService.toggleServiceStatus(serviceId);
        return ResponseEntity.ok(Map.of("message", "Đổi trạng thái dịch vụ thành công nka ní!"));
    }

    // 🔍 6. 🌟 TÌM KIẾM NÂNG CAO
    @GetMapping("/search")
    public ResponseEntity<List<ServiceResponse>> searchServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String keyword) {
        List<ServiceResponse> responses = serviceEntityService.searchServices(category, keyword);
        return ResponseEntity.ok(responses);
    }
}