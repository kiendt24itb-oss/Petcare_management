package com.example.petcare_management.repository;

import com.example.petcare_management.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    // Tìm nhanh tất cả chi tiết dịch vụ của một đầu mã Booking cụ thể
    List<BookingDetail> findByBooking_BookingId(Integer bookingId);
}