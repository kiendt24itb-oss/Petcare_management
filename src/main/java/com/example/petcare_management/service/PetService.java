package com.example.petcare_management.service;

import com.example.petcare_management.dto.PetRequest;
import com.example.petcare_management.dto.PetResponse;
import java.util.List;

public interface PetService {
    // Thêm mới một bé thú cưng cho khách hàng đang đăng nhập
    PetResponse addPet(String username, PetRequest request);

    // Cập nhật thông tin thú cưng (Chỉ cho phép nếu đúng chủ nuôi)
    PetResponse updatePet(String username, Integer petId, PetRequest request);

    // Lấy danh sách toàn bộ thú cưng của khách hàng đang đăng nhập
    List<PetResponse> getMyPets(String username);

    // Xem chi tiết thông tin 1 bé thú cưng
    PetResponse getPetDetail(String username, Integer petId);

    // Lấy mã thú cưng lớn nhất hiện tại từ DB
    String findLatestPetCode();

    // Xóa bé thú cưng dựa theo username và petId (để bảo mật chống xóa chéo)
    void deletePet(String username, Integer petId);

    // 👑 DÀNH CHO ADMIN: Lấy toàn bộ thú cưng trong hệ thống kèm thông tin mapping chủ nuôi
    List<PetResponse> getAllPetsForAdmin();
}