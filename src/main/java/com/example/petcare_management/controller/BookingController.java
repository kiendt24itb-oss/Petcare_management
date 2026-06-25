package com.example.petcare_management.controller;

import com.example.petcare_management.dto.BookingRequest;
import com.example.petcare_management.dto.BookingResponse;
import com.example.petcare_management.dto.Overview;
import com.example.petcare_management.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BookingController {

    private final BookingService bookingService;

    // =============================================================================
    // 👤 PHASE 1: ENDPOINTS DÀNH CHO KHÁCH HÀNG (CUSTOMER)
    // =============================================================================

    /**
     * 📥 1. API ĐẶT LỊCH HẸN MỚI
     * POST http://localhost:8080/api/bookings/create
     */
    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        BookingResponse response = bookingService.createBooking(username, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 📋 2. API XEM LỊCH SỬ ĐẶT LỊCH (Phân luồng thông minh: Admin / Nhân viên / Khách hàng)
     * GET http://localhost:8080/api/bookings/my-bookings
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        // 🔍 1. Kiểm tra các quyền (Roles) của tài khoản từ Token
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));

        List<BookingResponse> responses;

        if (isAdmin) {
            // 👑 NẾU LÀ ADMIN: Lấy tất cả mọi đơn hàng của toàn hệ thống hôm nay/mọi lúc để vận hành
            // Bạn có thể tận dụng hoặc viết thêm hàm findAllBookings() trong BookingService
            responses = bookingService.getAllBookings();
            System.out.println("👑 Hệ thống phân luồng: Tài khoản ADMIN [" + username + "] đang kiểm soát toàn bộ đơn hàng!");
        } else if (isStaff) {
            // ✂️ NẾU LÀ STAFF: Quét đơn theo Staff phụ trách
            responses = bookingService.getBookingsByStaffUsername(username);
            System.out.println("🤖 Hệ thống phân luồng: Tài khoản STAFF [" + username + "] đang lấy lịch trình làm việc!");
        } else {
            // 👤 NẾU LÀ CUSTOMER: Chỉ lấy đơn do mình tự đặt
            responses = bookingService.getBookingsByUsername(username);
            System.out.println("👤 Hệ thống phân luồng: Khách hàng [" + username + "] đang xem lịch sử đặt lịch!");
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * ❌ 3. API HỦY LỊCH HẸN (Tự check phạt sát giờ < 1 tiếng ở Service)
     * DELETE http://localhost:8080/api/bookings/cancel/{id}
     */
    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<String> cancelBooking(
            @PathVariable("id") Integer bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        bookingService.cancelBooking(username, bookingId);
        return ResponseEntity.ok("🎉 Hủy lịch hẹn thành công (Hệ thống đã rà soát giờ phạt nếu có)!");
    }

    /**
     * 📝 4. API CHỈNH SỬA LỊCH HẸN
     * PUT http://localhost:8080/api/bookings/update/{id}
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable("id") Integer bookingId,
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        BookingResponse response = bookingService.updateBooking(bookingId, username, request);
        return ResponseEntity.ok(response);
    }

    // =============================================================================
    // 👑 PHASE 2: ENDPOINTS DÀNH CHO QUẢN TRỊ VIÊN (ADMIN) - LOGIC VÒNG LẶP VI PHẠM
    // =============================================================================

    /**
     * 📋 4.5. ADMIN LẤY DANH SÁCH LỊCH HẸN ĐANG CHỜ PHÊ DUYỆT (PENDING_APPROVAL)
     * GET http://localhost:8080/api/bookings/admin/pending-list
     * 🌟 THÊM ENDPOINT NÀY ĐỂ OVERVIEW.JS GỌI DATA THẬT NÈ NÍ!
     */
    @GetMapping("/admin/pending-list")
    public ResponseEntity<List<BookingResponse>> getPendingApprovalBookings() {
        // Tìm và trả về các booking có status = BookingStatus.PENDING_APPROVAL
        // Ông nhớ khai báo hàm này trong BookingService và BookingServiceImpl nhé
        List<BookingResponse> responses = bookingService.getPendingApprovalBookings();
        return ResponseEntity.ok(responses);
    }

    /**
     * ✔️ 5. ADMIN PHÊ DUYỆT ĐƠN BỊ ÉP PHẢI DUYỆT TAY
     * PUT http://localhost:8080/api/bookings/admin/approve-booking/{id}
     * 🌟 SỬA URL THÀNH 'approve-booking' ĐỂ KHỚP VỚI OVERVIEW.JS
     */
    @PutMapping("/admin/approve-booking/{id}")
    public ResponseEntity<String> approveBooking(@PathVariable("id") Integer bookingId) {
        bookingService.approveBooking(bookingId);
        return ResponseEntity.ok("✅ Admin duyệt đơn thành công! Điểm phạt của khách đã reset về 0.");
    }

    /**
     * ✖️ 6. ADMIN TỪ CHỐI ĐƠN (Cộng vi phạm, chạm 5 tự động khóa acc)
     * PUT http://localhost:8080/api/bookings/admin/reject-booking/{id}
     * 🌟 SỬA URL THÀNH 'reject-booking' ĐỂ KHỚP VỚI OVERVIEW.JS
     */
    @PutMapping("/admin/reject-booking/{id}")
    public ResponseEntity<String> rejectBooking(@PathVariable("id") Integer bookingId) {
        bookingService.rejectBooking(bookingId);
        return ResponseEntity.ok("❌ Admin từ chối đơn thành công! Đã ghi nhận điểm phạt phạt vi phạm.");
    }

    /**
     * 🔓 7. ADMIN MỞ KHÓA TÀI KHOẢN
     * PUT http://localhost:8080/api/bookings/admin/unlock-account/{customerId}
     */
    @PutMapping("/admin/unlock-account/{customerId}")
    public ResponseEntity<String> unlockAccount(@PathVariable("customerId") Integer customerId) {
        bookingService.unlockCustomerAccount(customerId);
        return ResponseEntity.ok("🔓 Đã mở khóa tài khoản thành công! Điểm phạt của khách đã lùi về mốc 3 để tiếp tục theo dõi duyệt tay.");
    }

    /**
     * 🔒 8. ADMIN CHỦ ĐỘNG KHÓA TÀI KHOẢN
     * PUT http://localhost:8080/api/bookings/admin/lock-account/{customerId}
     */
    @PutMapping("/admin/lock-account/{customerId}")
    public ResponseEntity<String> lockAccount(@PathVariable("customerId") Integer customerId) {
        bookingService.lockCustomerAccount(customerId);
        return ResponseEntity.ok("🔒 Đã chủ động khóa tài khoản khách hàng thành công!");
    }

    // =============================================================================
    // ✂️ PHASE 3: ENDPOINTS DÀNH CHO NHÂN VIÊN (STAFF)
    // =============================================================================

    /**
     * ⚡ 9. NHÂN VIÊN CHỦ ĐỘNG BẤM VÀO CA SỚM
     * PUT http://localhost:8080/api/bookings/staff/start-early/{id}
     */
    @PutMapping("/staff/start-early/{id}")
    public ResponseEntity<BookingResponse> staffStartEarly(@PathVariable("id") Integer bookingId) {
        BookingResponse response = bookingService.staffStartEarly(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * ❌ 10. NHÂN VIÊN CHỦ ĐỘNG HỦY CA HẸN (Có kèm lý do hủy)
     * PUT http://localhost:8080/api/bookings/staff/cancel/{id}?reason=...
     */
    @PutMapping("/staff/cancel/{id}")
    public ResponseEntity<String> staffCancelBooking(
            @PathVariable("id") Integer bookingId,
            @RequestParam(value = "reason", required = false) String reason) {
        bookingService.staffCancelBooking(bookingId, reason);
        return ResponseEntity.ok("❌ Nhân viên đã hủy lịch hẹn thành công!");
    }

    /**
     * 🏁 API: NHÂN VIÊN BẤM HOÀN THÀNH SỚM (ĐÓNG CA CHỦ ĐỘNG)
     * PUT http://localhost:8080/api/bookings/staff/complete-early/{id}
     */
    @PutMapping("/staff/complete-early/{id}")
    public ResponseEntity<BookingResponse> staffCompleteEarly(@PathVariable("id") Integer bookingId) {
        BookingResponse response = bookingService.staffCompleteEarly(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * 📊 API LẤY SỐ LIỆU THỐNG KÊ DOANH THU (Tháng hiện tại & 6 Tháng gần nhất)
     * GET http://localhost:8080/api/bookings/admin/dashboard-stats
     */
    @GetMapping("/admin/dashboard-stats")
    public ResponseEntity<Overview> getDashboardStats() {
        Overview response = bookingService.getDashboardStats();
        return ResponseEntity.ok(response);
    }
}