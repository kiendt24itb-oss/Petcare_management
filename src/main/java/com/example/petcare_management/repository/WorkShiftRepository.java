package com.example.petcare_management.repository;

import com.example.petcare_management.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Integer> {

    // Ép giờ chính xác: Nằm trong khoảng từ start_time đến trước end_time của ca nào thì lấy ca đó
    @Query(value = "SELECT * FROM work_shifts WHERE :now >= start_time AND :now < end_time LIMIT 1", nativeQuery = true)
    Optional<WorkShift> findActiveShiftByTime(@Param("now") LocalTime now);
}