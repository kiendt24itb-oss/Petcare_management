package com.example.petcare_management.repository;

import com.example.petcare_management.entity.ServiceEntity;
import com.example.petcare_management.entity.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Integer> {

    Optional<ServiceEntity> findByServiceCode(String serviceCode);

    List<ServiceEntity> findByStatusTrue();

    List<ServiceEntity> findByCategoryAndStatusTrue(ServiceCategory category);

    /**
     * 🚪 [BỔ SUNG] Tìm dịch vụ theo Mã phòng vận hành chỉ định
     * Giúp hệ thống quét nhanh xem Phòng (ví dụ: R-O001) đang được gán cho gói dịch vụ nào
     */
    @Query("SELECT s FROM ServiceEntity s WHERE s.room.roomCode = :roomCode")
    List<ServiceEntity> findByRoomCode(@Param("roomCode") String roomCode);

    /**
     * 🌟 Lấy mã lớn nhất theo từng danh mục cụ thể
     * Hỗ trợ tạo mã tự nhảy chuẩn định dạng: DV-H001, DV-S001, DV-O001...
     */
    @Query("SELECT MAX(s.serviceCode) FROM ServiceEntity s WHERE s.category = :category")
    String findMaxServiceCodeByCategory(@Param("category") ServiceCategory category);

    boolean existsByServiceCode(String serviceCode);

    /**
     * 🔍 QUERY TÌM KIẾM NÂNG CAO: Lọc theo Category (nếu có) và tìm kiếm gần đúng theo Tên
     */
    @Query("SELECT s FROM ServiceEntity s WHERE " +
            "(:category IS NULL OR s.category = :category) AND " +
            "(LOWER(s.serviceName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ServiceEntity> searchServices(@Param("category") ServiceCategory category, @Param("keyword") String keyword);

    /**
     * 🧹 GỠ PHÒNG KHỎI DỊCH VỤ KHI PHÒNG ĐI BẢO TRÌ
     * Set trường room về null cho tất cả các dịch vụ đang bám vào roomId này
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ServiceEntity s SET s.room = null WHERE s.room.roomId = :roomId")
    void detachRoomFromServices(@Param("roomId") Integer roomId);
}