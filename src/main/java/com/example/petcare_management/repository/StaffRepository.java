package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Staff;
import com.example.petcare_management.dto.StaffResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    // 🔮 Lấy mã nhân viên mới nhất (Giữ nguyên)
    @Query(value = "SELECT s.staff_code FROM staffs s WHERE s.staff_code LIKE 'NV-A%' ORDER BY s.staff_code DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLatestStaffCode();

    // 👤 Tìm staff theo username phục vụ luồng cũ
    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.account LEFT JOIN FETCH s.position WHERE s.account.username = :username")
    Optional<Staff> findByAccountUsernameWithAccount(@Param("username") String username);

    // ⚡ Lọc theo mã chức vụ phòng ban
    @Query("SELECT s FROM Staff s WHERE s.position.positionCode = :positionCode")
    List<Staff> findByPosition_PositionCode(@Param("positionCode") String positionCode);

    // 🌟 KHỚP NỐI: Tìm kiếm nhân viên bằng Email chuẩn xác để phục vụ logic Auto-Link
    Optional<Staff> findByEmail(String email);

    // 🛡️ RÀ SOÁT: Check trùng phục vụ lúc Admin thêm mới hoặc sửa nhân viên
    boolean existsByEmail(String email);
    boolean existsByCccd(String cccd);

    // ================= 🌟 PHẦN NÂNG CẤP ĐÃ ĐỒNG BỘ THỨ TỰ CONSTRUCTOR @AllArgsConstructor =================

    // 1️⃣ Lấy TOÀN BỘ danh sách nhân viên (LEFT JOIN toàn diện, xếp chuẩn theo thứ tự biến trong DTO)
    @Query("SELECT new com.example.petcare_management.dto.StaffResponse(" +
            "s.staffId, s.staffCode, s.fullName, s.gender, s.email, s.phone, s.avatar, s.address, " +
            "p.positionName, s.salary, s.bonus, s.totalSalary, a.username, s.hireDate, s.cccd, p.positionId) " +
            "FROM Staff s " +
            "LEFT JOIN s.account a " +
            "LEFT JOIN s.position p " +
            "ORDER BY s.staffId DESC")
    List<StaffResponse> getAllStaffDetails();

    // 2️⃣ TÌM KIẾM NÂNG CAO CHO ADMIN: (Xếp chuẩn theo thứ tự biến trong DTO)
    @Query("SELECT new com.example.petcare_management.dto.StaffResponse(" +
            "s.staffId, s.staffCode, s.fullName, s.gender, s.email, s.phone, s.avatar, s.address, " +
            "p.positionName, s.salary, s.bonus, s.totalSalary, a.username, s.hireDate, s.cccd, p.positionId) " +
            "FROM Staff s " +
            "LEFT JOIN s.account a " +
            "LEFT JOIN s.position p " +
            "WHERE (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR s.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:positionId IS NULL OR p.positionId = :positionId) " +
            "ORDER BY s.staffId DESC")
    List<StaffResponse> searchStaffs(@Param("keyword") String keyword,
                                     @Param("positionId") Integer positionId);

}