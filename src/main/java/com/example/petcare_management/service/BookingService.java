package com.example.petcare_management.service;

import com.example.petcare_management.dto.BookingRequest;
import com.example.petcare_management.dto.BookingResponse;
import com.example.petcare_management.dto.Overview;

import java.util.List;

public interface BookingService {

    // 📥 Hàm đặt lịch mới (Khách hàng)
    BookingResponse createBooking(String username, BookingRequest request);

    // 📋 Hàm xem danh sách lịch hẹn (Khách hàng)
    List<BookingResponse> getBookingsByUsername(String username);

    // ❌ Hàm hủy lịch hẹn (Khách hàng)
    void cancelBooking(String username, Integer bookingId);

    // 📝 Hàm chỉnh sửa lịch hẹn (Khách hàng)
    BookingResponse updateBooking(Integer bookingId, String username, BookingRequest request);

    // 👑 HÀM MỚI (ADMIN): Phê duyệt đơn chờ duyệt của tài khoản vi phạm
    void approveBooking(Integer bookingId);

    // 👑 HÀM MỚI (ADMIN): Từ chối đơn chờ duyệt (Cộng điểm phạt & Tự động khóa nếu chạm mốc 5)
    void rejectBooking(Integer bookingId);

    // 👑 HÀM MỚI (ADMIN): Mở khóa tài khoản vi phạm (Ép điểm phạt từ 5 xuống mốc 3)
    void unlockCustomerAccount(Integer customerId); // 🌟 THÊM DÒNG NÀY VÀO LÀ HẾT BÁO ĐỎ CẢ CONTROLLER LẪN SERVICE!

    void lockCustomerAccount(Integer customerId);

    List<BookingResponse> getPendingApprovalBookings();

    // ✂️ HÀM MỚI (STAFF): Nhân viên chủ động bấm vào ca sớm
    BookingResponse staffStartEarly(Integer bookingId);

    // ✂️ HÀM MỚI (STAFF): Nhân viên chủ động hủy ca hẹn
    void staffCancelBooking(Integer bookingId, String reason);

    // 🌟 THÊM DÒNG NÀY NÈ NÍ: Nhân viên làm xong nhanh, chủ động bấm kết thúc sớm (Không đợi Cron Job)
    BookingResponse staffCompleteEarly(Integer bookingId);

    List<BookingResponse> getBookingsByStaffUsername(String username);

    // 📊 HÀM MỚI (ADMIN): Lấy số liệu thống kê doanh thu tháng này & 6 tháng gần nhất cho Dashboard
    Overview getDashboardStats();

    List<BookingResponse> getAllBookings();
}