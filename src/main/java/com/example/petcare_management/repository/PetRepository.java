package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {

    // 🌟 Bốc mã thú cưng lớn nhất hiện tại để nạp vào bộ CodeGenerator của ní
    @Query(value = "SELECT p.pet_code FROM pets p WHERE p.pet_code LIKE 'TC-%' ORDER BY p.pet_code DESC LIMIT 1", nativeQuery = true)
    String findLatestPetCode();

    // Lấy toàn bộ danh sách thú cưng của riêng một khách hàng dựa vào username đang đăng nhập
    List<Pet> findByCustomer_Account_Username(String username);

    /**
     * 👑 DÀNH CHO ADMIN: Bốc sạch toàn bộ thú cưng kèm thông tin khách hàng trong 1 câu Query duy nhất
     * Giải quyết triệt để lỗi N+1 Query giúp tối ưu hóa hiệu năng hệ thống.
     */
    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.customer")
    List<Pet> findAllWithCustomer();
}