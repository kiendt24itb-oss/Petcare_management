package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Room;
import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.entity.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    /**
     * 🚪 Tìm kiếm thực thể Phòng dựa vào Mã phòng (roomCode)
     */
    Optional<Room> findByRoomCode(String roomCode);

    // 🔮 Bốc mã phòng lớn nhất phục vụ bộ CodeGenerator nhảy số tự động (R-001, R-002...)
    @Query(value = "SELECT r.room_code FROM rooms r WHERE r.room_code LIKE 'R-%' ORDER BY r.room_code DESC LIMIT 1", nativeQuery = true)
    String findLatestRoomCode();

    // 1. Tìm kiếm phòng theo tên (Dùng LIKE để tìm kiếm gần đúng, bỏ qua hoa thường)
    List<Room> findByRoomNameContainingIgnoreCase(String roomName);

    // 2. Lọc phòng theo Khối (HEALTH, SPA, HOTEL)
    List<Room> findByRoomCategory(ServiceCategory roomCategory);

    // 3. Kết hợp cả Tìm kiếm tên + Lọc theo Khối
    List<Room> findByRoomNameContainingIgnoreCaseAndRoomCategory(String roomName, ServiceCategory roomCategory);

    // 🌟 Kết hợp lọc theo Danh mục và Trạng thái phòng (Ví dụ: Tìm phòng trống thuộc khối HOTEL)
    List<Room> findByRoomCategoryAndStatus(ServiceCategory roomCategory, RoomStatus status);

    // 🔥 [BỔ SUNG THÊM ĐỂ ĐỒNG BỘ DASHBOARD]: Đếm tổng số phòng đang trống trên toàn hệ thống
    long countByStatus(RoomStatus status);
}