package com.example.petcare_management.service;

import com.example.petcare_management.dto.BookingLogResponse;
import com.example.petcare_management.dto.DashboardResponse;
import com.example.petcare_management.dto.OperationFlowResponse;
import com.example.petcare_management.dto.StaffLogResponse;
import com.example.petcare_management.entity.Booking;
import com.example.petcare_management.entity.Room;
import com.example.petcare_management.entity.enums.BookingStatus;
import com.example.petcare_management.entity.enums.RoomStatus;
import com.example.petcare_management.repository.BookingRepository;
import com.example.petcare_management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingLogService bookingLogService;

    // 🌟 THÊM VÀO: Gọi Service của Log nhân sự để quét log chấm công
    private final StaffLogService staffLogService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        LocalDate today = LocalDate.now();

        // ----------------------------------------------------------------
        // 1. XỬ LÝ SỐ LIỆU TỔNG HỢP (4 Ô THỐNG KÊ PHÍA TRÊN)
        // ----------------------------------------------------------------

        // Ô 1: Tổng ca tiếp nhận hôm nay
        long totalTicketsToday = bookingRepository.countActiveBookingsByDate(today);

        // Ô 2 & Ô 3: Đang chờ và Đang thực hiện
        long waitingCount = bookingRepository.findByBookingDateAndStatus(today, BookingStatus.WAITING).size();
        long processingCount = bookingRepository.findByStatus(BookingStatus.PROCESSING).stream()
                .filter(b -> today.equals(b.getBookingDate()))
                .count();

        // Ô 4: Thống kê và tính toán công suất của TẤT CẢ CÁC PHÒNG
        List<Room> allRoomsInSystem = roomRepository.findAll();
        long totalRooms = allRoomsInSystem.size();

        long availableRooms = allRoomsInSystem.stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .count();

        long occupiedRooms = allRoomsInSystem.stream()
                .filter(r -> r.getStatus() == RoomStatus.BOOKED || r.getStatus() == RoomStatus.BUSY)
                .count();

        double hotelCapacityRate = 0.0;
        if (totalRooms > 0) {
            hotelCapacityRate = ((double) occupiedRooms / totalRooms) * 100;
        }

        // ----------------------------------------------------------------
        // 2. XỬ LÝ BẢNG LUỒNG ĐIỀU PHỐI (Ở GIỮA UI)
        // ----------------------------------------------------------------
        List<Booking> bookingsToday = new ArrayList<>(bookingRepository.findByBookingDateAndStatus(today, BookingStatus.WAITING));

        List<Booking> processingBookingsToday = bookingRepository.findByStatus(BookingStatus.PROCESSING).stream()
                .filter(b -> today.equals(b.getBookingDate()))
                .collect(Collectors.toList());

        bookingsToday.addAll(processingBookingsToday);

        List<Booking> uniqueBookings = bookingsToday.stream().distinct().collect(Collectors.toList());

        List<OperationFlowResponse> operationFlows = uniqueBookings.stream().map(b -> {
            // 🌟 Lấy tên thú cưng từ BookingDetail (giống như logic lấy petName ở Booking_list.js)
            String petName = "Thú cưng";
            String ownerName = "Khách hàng";
            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                var firstDetail = b.getBookingDetails().iterator().next();
                if (firstDetail.getPet() != null) {
                    petName = firstDetail.getPet().getPetName(); // Lấy petName tường minh
                }
                // Lấy Customer FullName thông qua Customer -> Account hoặc trực tiếp tùy Entity của bạn
                if (b.getCustomer() != null && b.getCustomer().getAccount() != null) {
                    ownerName = b.getCustomer().getFullName(); // Lấy FullName của khách
                }
            }

            String serviceName = "Dịch vụ tổng hợp";
            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                var firstDetail = b.getBookingDetails().iterator().next();
                if (firstDetail.getService() != null) {
                    serviceName = firstDetail.getService().getServiceName();
                }
            }

            // 🌟 Lấy Staff FullName thay vì username
            String staffInCharge = "Chưa Chỉ Định";
            if (b.getBookingDetails() != null && !b.getBookingDetails().isEmpty()) {
                var firstDetail = b.getBookingDetails().iterator().next();
                if (firstDetail.getStaff() != null && firstDetail.getStaff().getAccount() != null) {
                    staffInCharge = firstDetail.getStaff().getFullName(); // Lấy FullName của nhân viên
                }
            }

            return OperationFlowResponse.builder()
                    .bookingCode(b.getBookingCode())
                    .petAndOwnerName(petName + " / " + ownerName)
                    .serviceName(serviceName)
                    .status(b.getStatus().name())
                    .staffInCharge(staffInCharge)
                    .build();
        }).collect(Collectors.toList());

        // ----------------------------------------------------------------
        // 3. 🌟 XỬ LÝ NHẬT KÝ BIẾN ĐỘNG VẬN HÀNH HỖN HỢP (GỘP ĐA LUỒNG)
        // ----------------------------------------------------------------
        List<BookingLogResponse> finalCombinedLogs = new ArrayList<>();

        // Luồng A: Đổ log từ bảng Đơn đặt lịch (Booking)
        List<BookingLogResponse> bookingLogs = bookingLogService.getDailyLogsForDashboard();
        if (bookingLogs != null) {
            finalCombinedLogs.addAll(bookingLogs);
        }

        // Luồng B: Đổ log từ bảng Chấm công nhân viên (Staff Log)
        try {
            List<StaffLogResponse> staffLogs = staffLogService.getTodayLogs();
            if (staffLogs != null) {
                for (StaffLogResponse sl : staffLogs) {

                    // 🌟 ĐÃ SỬA: Chuyển đổi LocalDateTime sang LocalTime bằng .toLocalTime()
                    // để khớp 100% với kiểu LocalTime của BookingLogResponse.logTime
                    java.time.LocalTime exactTime = (sl.getLogTime() != null)
                            ? sl.getLogTime().toLocalTime()
                            : java.time.LocalTime.now();

                    finalCombinedLogs.add(BookingLogResponse.builder()
                            .logTime(exactTime) // Không còn lo lỗi lệch kiểu dữ liệu nữa!
                            .logText(sl.getLogText())
                            .build());
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Lưu ý: Luồng trộn StaffLog gặp lỗi, bỏ qua để hệ thống chạy tiếp: " + e.getMessage());
        }

        // Luồng C: Sắp xếp trộn theo dòng thời gian giảm dần (Log mới nhất lên đầu)
        finalCombinedLogs.sort((b, a) -> {
            if (a.getLogTime() == null || b.getLogTime() == null) return 0;
            return a.getLogTime().compareTo(b.getLogTime()); // So sánh trực tiếp bằng LocalDateTime cực chuẩn
        });

        // ----------------------------------------------------------------
        // 4. ĐÓNG GÓI TRẢ VỀ DTO TỔNG HỢP CHO FRONT-END
        // ----------------------------------------------------------------
        return DashboardResponse.builder()
                .totalTicketsToday(totalTicketsToday)
                .waitingCount(waitingCount)
                .processingCount(processingCount)
                .hotelCapacityRate(hotelCapacityRate)
                .availableRooms(availableRooms)
                .operationFlows(operationFlows)
                .operationLogs(finalCombinedLogs) // 🌟 Bắn mảng đã gộp và làm sạch về UI
                .build();
    }
}