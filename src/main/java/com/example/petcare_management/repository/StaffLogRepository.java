package com.example.petcare_management.repository;

import com.example.petcare_management.entity.StaffLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StaffLogRepository extends JpaRepository<StaffLog, Integer> {

    // Lấy danh sách log của toàn bộ nhân viên trong khoảng thời gian (Từ đầu ngày đến cuối ngày hôm nay)
    // Sắp xếp theo cái nào mới ghi thì hiện lên đầu (Desc)
    @Query("SELECT sl FROM StaffLog sl " +
            "WHERE sl.logTime >= :startOfDay AND sl.logTime <= :endOfDay " +
            "ORDER BY sl.logTime DESC")
    List<StaffLog> findLogsByDateRange(@Param("startOfDay") LocalDateTime startOfDay,
                                       @Param("endOfDay") LocalDateTime endOfDay);
}