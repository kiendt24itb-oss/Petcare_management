package com.example.petcare_management.service;

import com.example.petcare_management.dto.BookingRequest;
import com.example.petcare_management.dto.BookingResponse;
import com.example.petcare_management.dto.Overview;
import com.example.petcare_management.entity.*;
import com.example.petcare_management.entity.enums.BookingStatus;
import com.example.petcare_management.entity.enums.PaymentStatus;
import com.example.petcare_management.entity.enums.AccountStatus;
import com.example.petcare_management.repository.*;
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final PetRepository petRepository;
    private final ServiceRepository serviceEntityRepository;
    private final StaffRepository staffRepository;
    private final BookingLogRepository bookingLogRepository;
    private final AccountRepository accountRepository;

    // =============================================================================
    // ➕ 1. TẠO MỚI LỊCH HẸN (Tích hợp check vi phạm & CodeGenerator)
    // =============================================================================
    @Override
    @Transactional
    public BookingResponse createBooking(String username, BookingRequest request) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng từ tài khoản này!"));

        // 🌟 KIỂM TRA ĐIỀU KIỆN VI PHẠM ĐỂ PHÂN LUỒNG TỰ ĐỘNG
        BookingStatus initialStatus = BookingStatus.WAITING;
        if (customer.getViolationCount() != null && customer.getViolationCount() >= 3) {
            initialStatus = BookingStatus.PENDING_APPROVAL; // Ép vào hàng chờ Admin duyệt tay
        }

        Booking booking = Booking.builder()
                .bookingCode(generateNextBookingCode())
                .customer(customer)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH")
                .paymentStatus(PaymentStatus.UNPAID)
                .status(initialStatus)
                .note(request.getNote())
                .totalPrice(BigDecimal.ZERO)
                .bookingDetails(new ArrayList<>())
                .build();

        BigDecimal runningTotal = BigDecimal.ZERO;
        List<BookingDetail> details = new ArrayList<>();
        List<String> petNames = new ArrayList<>();

        if (request.getItems() != null) {
            for (BookingRequest.BookingItemDTO item : request.getItems()) {
                Pet pet = petRepository.findById(item.getPetId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng có ID: " + item.getPetId()));

                if (!pet.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                    throw new RuntimeException("Bé cưng " + pet.getPetName() + " không thuộc quyền sở hữu của bạn!");
                }

                ServiceEntity serviceEntity = serviceEntityRepository.findById(item.getServiceId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ có ID: " + item.getServiceId()));

                BookingDetail detail = BookingDetail.builder()
                        .booking(booking)
                        .pet(pet)
                        .service(serviceEntity)
                        .room(serviceEntity.getRoom()) // Gán phòng mặc định từ Dịch vụ sang
                        .price(serviceEntity.getPrice())
                        .build();

                if (item.getStaffId() != null) {
                    Staff staff = staffRepository.findById(item.getStaffId()).orElse(null);
                    detail.setStaff(staff);
                }

                details.add(detail);
                if (!petNames.contains(pet.getPetName())) {
                    petNames.add(pet.getPetName());
                }

                runningTotal = runningTotal.add(serviceEntity.getPrice());
            }
        }

        booking.setBookingDetails(details);
        booking.setTotalPrice(runningTotal);

        Booking savedBooking = bookingRepository.save(booking);

        String petsStr = String.join(", ", petNames);
        String logText;
        if (savedBooking.getStatus() == BookingStatus.PENDING_APPROVAL) {
            logText = String.format("Hệ thống kích hoạt cảnh báo vi phạm (%d lần bùng). Đơn đặt lịch [%s] của bé (%s) chuyển vào trạng thái [Chờ Admin phê duyệt].",
                    customer.getViolationCount(), savedBooking.getBookingCode(), petsStr);
        } else {
            logText = "Hệ thống tiếp nhận thành công lịch hẹn " + savedBooking.getBookingCode() + " của bé (" + petsStr + ") vào danh sách chờ phục vụ.";
        }

        BookingLog initLog = BookingLog.builder()
                .booking(savedBooking)
                .logTime(LocalTime.now())
                .logText(logText)
                .build();
        bookingLogRepository.save(initLog);

        return mapToResponse(savedBooking);
    }

    // =============================================================================
    // 👑 2. [ADMIN] PHÊ DUYỆT ĐƠN CHO TÀI KHOẢN VI PHẠM (Reset về 0)
    // =============================================================================
    @Override
    @Transactional
    public void approveBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn cần duyệt!"));

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Đơn này không nằm trong danh sách cần phê duyệt!");
        }

        Customer customer = booking.getCustomer();
        booking.setStatus(BookingStatus.WAITING);
        customer.setViolationCount(0); // Được duyệt thành công -> Tha thứ đưa vi phạm về mốc 0

        bookingRepository.save(booking);
        customerRepository.save(customer);

        BookingLog log = BookingLog.builder()
                .booking(booking)
                .logTime(LocalTime.now())
                .logText(String.format("Admin phê duyệt thành công đơn [%s]. Số lần vi phạm của khách hàng [%s] đã được đặt lại về mốc 0.",
                        booking.getBookingCode(), customer.getFullName()))
                .build();
        bookingLogRepository.save(log);
    }

    // =============================================================================
    // 👑 3. [ADMIN] TỪ CHỐI ĐƠN (Cộng vi phạm & Khóa tự động nếu chạm mốc 5)
    // =============================================================================
    @Override
    @Transactional
    public void rejectBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn cần từ chối!"));

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Đơn này không nằm trong danh sách cần phê duyệt!");
        }

        Customer customer = booking.getCustomer();
        Account account = customer.getAccount();

        booking.setStatus(BookingStatus.REJECTED);

        int currentViolation = (customer.getViolationCount() != null ? customer.getViolationCount() : 0) + 1;
        customer.setViolationCount(currentViolation);

        String autoLockNotice = "";
        if (currentViolation >= 5) {
            if (account != null) {
                account.setStatus(AccountStatus.LOCKED); // Khóa tài khoản vĩnh viễn ở mốc 5
                accountRepository.save(account);
                autoLockNotice = String.format(" 🚨 Tài khoản [%s] đã bị KHÓA TỰ ĐỘNG do tích lũy chạm mốc %d lần vi phạm.", account.getUsername(), currentViolation);
            }
        }

        bookingRepository.save(booking);
        customerRepository.save(customer);

        BookingLog log = BookingLog.builder()
                .booking(booking)
                .logTime(LocalTime.now())
                .logText(String.format("Admin từ chối đơn [%s]. Khách hàng bị tính thêm 1 lần vi phạm (Hiện tại: %d/5).%s",
                        booking.getBookingCode(), currentViolation, autoLockNotice))
                .build();
        bookingLogRepository.save(log);
    }

    // =============================================================================
    // ❌ 4. HỦY LỊCH HẸN (Tính toán phạt sát giờ < 1 tiếng)
    // =============================================================================
    @Override
    @Transactional
    public void cancelBooking(String username, Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn cần hủy!"));

        if (!booking.getCustomer().getAccount().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền hủy lịch hẹn của người khác!");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Lịch hẹn này đã đóng hoặc đã hủy trước đó!");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Lịch hẹn đã thanh toán, vui lòng liên hệ quầy lễ tân để được hoàn tiền!");
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(booking.getBookingDate(), booking.getBookingTime());
        LocalDateTime now = LocalDateTime.now();

        Customer customer = booking.getCustomer();
        String penaltyNotice = "";

        // Kiểm tra xem thời gian hủy có cách thời gian hẹn dưới 60 phút hay không
        if (now.isAfter(bookingDateTime) || Duration.between(now, bookingDateTime).toMinutes() < 60) {
            int currentViolation = (customer.getViolationCount() != null ? customer.getViolationCount() : 0) + 1;
            customer.setViolationCount(currentViolation);
            customerRepository.save(customer);
            penaltyNotice = String.format(" ⚠️ Cảnh báo: Bạn hủy lịch sát giờ hẹn (< 1 tiếng). Hệ thống cộng 1 điểm phạt vi phạm (Hiện tại: %d lần).", currentViolation);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        BookingLog cancelLog = BookingLog.builder()
                .booking(booking)
                .logTime(LocalTime.now())
                .logText("Lịch hẹn " + booking.getBookingCode() + " đã bị khách hàng hủy bỏ trên hệ thống." + penaltyNotice)
                .build();
        bookingLogRepository.save(cancelLog);
    }

    // =============================================================================
    // 👑 5. [ADMIN] MỞ KHÓA TÀI KHOẢN VI PHẠM (Hạ vi phạm từ 5 xuống mốc 3)
    // =============================================================================
    @Transactional
    public void unlockCustomerAccount(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng!"));

        Account account = customer.getAccount();
        if (account == null) {
            throw new RuntimeException("Tài khoản liên kết với khách hàng này không tồn tại!");
        }

        if (account.getStatus() != AccountStatus.LOCKED) {
            throw new IllegalStateException("Tài khoản này hiện tại không bị khóa!");
        }

        // 1. Mở khóa kích hoạt lại tài khoản
        account.setStatus(AccountStatus.ACTIVE);

        // 2. Ép điểm phạt lùi từ mốc 5 về mốc 3 để đơn tiếp theo tiếp tục bị ép duyệt tay
        customer.setViolationCount(3);

        accountRepository.save(account);
        customerRepository.save(customer);

        log.info("🔓 Admin đã mở khóa tài khoản thành công cho [{}]. Số lần vi phạm lùi về mốc: 3.", account.getUsername());
    }

    // =============================================================================
    // 🛠️ 6. CHỈNH SỬA LỊCH HẸN VÀ CẬP NHẬT PHÒNG
    // =============================================================================
    @Override
    @Transactional
    public BookingResponse updateBooking(Integer bookingId, String username, BookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn cần sửa!"));

        if (!booking.getCustomer().getAccount().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa lịch hẹn này!");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Lịch hẹn đã hoàn thành hoặc đã hủy, không thể chỉnh sửa!");
        }

        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setNote(request.getNote());
        if (request.getPaymentMethod() != null) {
            booking.setPaymentMethod(request.getPaymentMethod());
        }

        booking.getBookingDetails().clear();

        BigDecimal runningTotal = BigDecimal.ZERO;
        List<BookingDetail> newDetails = new ArrayList<>();

        if (request.getItems() != null) {
            for (BookingRequest.BookingItemDTO item : request.getItems()) {
                Pet pet = petRepository.findById(item.getPetId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng!"));
                ServiceEntity serviceEntity = serviceEntityRepository.findById(item.getServiceId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ!"));

                BookingDetail detail = BookingDetail.builder()
                        .booking(booking)
                        .pet(pet)
                        .service(serviceEntity)
                        .room(serviceEntity.getRoom()) // Đồng bộ phòng mặc định khi sửa đổi
                        .price(serviceEntity.getPrice())
                        .build();

                if (item.getStaffId() != null) {
                    Staff staff = staffRepository.findById(item.getStaffId()).orElse(null);
                    detail.setStaff(staff);
                }
                newDetails.add(detail);

                runningTotal = runningTotal.add(serviceEntity.getPrice());
            }
        }

        booking.getBookingDetails().addAll(newDetails);
        booking.setTotalPrice(runningTotal);

        Booking savedBooking = bookingRepository.save(booking);

        BookingLog updateLog = BookingLog.builder()
                .booking(savedBooking)
                .logTime(LocalTime.now())
                .logText("Khách hàng chủ động chỉnh sửa lại thông tin dịch vụ / thời gian hẹn trên giao diện.")
                .build();
        bookingLogRepository.save(updateLog);

        return mapToResponse(savedBooking);
    }

    // =============================================================================
    // 📥 7. LẤY DANH SÁCH LỊCH HẸN (Sắp xếp tăng dần theo ngày/giờ)
    // =============================================================================
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUsername(String username) {
        return bookingRepository.findByCustomer_Account_UsernameOrderByBookingDateAscBookingTimeAsc(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =============================================================================
    // 🔮 HÀM GEN MÃ CODE TỰ ĐỘNG
    // =============================================================================
    private String generateNextBookingCode() {
        String latestCode = bookingRepository.findLatestBookingCode();
        return CodeGenerator.generateNextCode(latestCode, "BK");
    }

    // =============================================================================
    // 🔄 MAP DỮ LIỆU SANG RESPONSE DTO TRẢ VỀ FRONT-END
    // =============================================================================
    private BookingResponse mapToResponse(Booking booking) {
        List<BookingResponse.DetailResponse> detailDTOs = booking.getBookingDetails().stream()
                .map(d -> {
                    String categoryStr = "SPA";
                    if (d.getService() != null && d.getService().getCategory() != null) {
                        categoryStr = d.getService().getCategory().toString();
                    }

                    return BookingResponse.DetailResponse.builder()
                            .detailId(d.getDetailId())
                            .petName(d.getPet() != null ? d.getPet().getPetName() : "Không rõ")
                            .serviceName(d.getService() != null ? d.getService().getServiceName() : "Không rõ")
                            .price(d.getPrice())
                            .staffName(d.getStaff() != null ? d.getStaff().getFullName() : "Đang chờ điều phối")
                            .roomName(d.getRoom() != null ? d.getRoom().getRoomName() : "Đang sắp xếp phòng")
                            .petId(d.getPet() != null ? d.getPet().getPetId() : null)
                            .serviceId(d.getService() != null ? d.getService().getServiceId() : null)
                            .staffId(d.getStaff() != null ? d.getStaff().getStaffId() : null)
                            .categoryType(categoryStr)
                            .build();
                })
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Khách vãng lai")
                .totalPrice(booking.getTotalPrice())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .actualStartTime(booking.getActualStartTime()) // 🌟 THÊM DÒNG NÀY VÀO ĐÂY NÍ NHA
                .paymentStatus(booking.getPaymentStatus())
                .status(booking.getStatus())
                .paymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod() : "CASH")
                .note(booking.getNote())
                .details(detailDTOs)
                .build();
    }

    // =============================================================================
    // 👑 8. [ADMIN] CHỦ ĐỘNG KHÓA TÀI KHOẢN (Ghét là khóa, ép vi phạm lên mốc 5)
    // =============================================================================
    @Override
    @Transactional
    public void lockCustomerAccount(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng cần khóa!"));

        Account account = customer.getAccount();
        if (account == null) {
            throw new RuntimeException("Tài khoản liên kết với khách hàng này không tồn tại!");
        }

        // 1. Ép trạng thái tài khoản sang LOCKED luôn, bất kể trước đó là gì
        account.setStatus(AccountStatus.LOCKED);

        // 2. Đẩy điểm phạt lên mốc 5 để đồng bộ với logic hệ thống tự động khóa
        customer.setViolationCount(5);

        accountRepository.save(account);
        customerRepository.save(customer);

        log.info("🔒 Admin đã chủ động KHÓA TÀI KHOẢN thành công cho khách hàng [{}]. Số lần vi phạm ép lên mốc: 5.", account.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getPendingApprovalBookings() {
        // Lọc trong Repo các booking có trạng thái là PENDING_APPROVAL
        // Giả định repo của ông có hàm tìm theo status: findByStatus(BookingStatus.PENDING_APPROVAL)
        return bookingRepository.findByStatus(BookingStatus.PENDING_APPROVAL)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =============================================================================
// ✂️ 9. [STAFF] NHÂN VIÊN BẤM VÀO CA SỚM
// =============================================================================
    @Override
    @Transactional
    public BookingResponse staffStartEarly(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn!"));

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalStateException("Lịch hẹn này không ở trạng thái chờ để có thể vào ca!");
        }

        // 1. Chuyển trạng thái sang PROCESSING & lưu thời gian thực tế bấm nút
        booking.setStatus(BookingStatus.PROCESSING);
        booking.setActualStartTime(LocalDateTime.now()); // Đã cấu hình ở Entity bảng to

        Booking savedBooking = bookingRepository.save(booking);

        // 2. Ghi Log lịch trình
        BookingLog log = BookingLog.builder()
                .booking(savedBooking)
                .logTime(LocalTime.now())
                .logText("⚡ Nhân viên đã chủ động bấm [Vào ca sớm]. Hệ thống bắt đầu tính giờ làm dịch vụ từ lúc này.")
                .build();
        bookingLogRepository.save(log);

        return mapToResponse(savedBooking);
    }

    // =============================================================================
// ✂️ 10. [STAFF] NHÂN VIÊN CHỦ ĐỘNG HỦY LỊCH HẸN
// =============================================================================
    @Override
    @Transactional
    public void staffCancelBooking(Integer bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn cần hủy!"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Lịch hẹn đã hoàn thành hoặc đã hủy trước đó!");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            booking.setNote(booking.getNote() + " | Lý do hủy của nhân viên: " + reason);
        }
        bookingRepository.save(booking);

        BookingLog cancelLog = BookingLog.builder()
                .booking(booking)
                .logTime(LocalTime.now())
                .logText("❌ Lịch hẹn đã bị nhân viên hủy bỏ. Lý do: " + (reason != null ? reason : "Không có lý do cụ thể."))
                .build();
        bookingLogRepository.save(cancelLog);
    }

    @Override
    @Transactional
    public BookingResponse staffCompleteEarly(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn!"));

        if (booking.getStatus() != BookingStatus.PROCESSING) {
            throw new IllegalStateException("Ca này chưa bấm vào làm hoặc đã đóng rồi!");
        }

        // Ép thẳng sang COMPLETED luôn
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        // Ghi log lịch trình
        BookingLog log = BookingLog.builder()
                .booking(savedBooking)
                .logTime(LocalTime.now())
                .logText("✅ Nhân viên chủ động bấm [Hoàn thành sớm] trên giao diện. Ca làm việc kết thúc thành công.")
                .build();
        bookingLogRepository.save(log);

        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByStaffUsername(String username) {
        return bookingRepository.findByStaffUsername(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Overview getDashboardStats() {
        LocalDate today = LocalDate.now(); // Hệ thống tự động bốc ngày hiện tại (năm 2026)
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        // 1. Lấy doanh thu tháng hiện tại
        BigDecimal currentMonthRev = bookingRepository.calculateMonthlyRevenue(currentMonth, currentYear);

        // 🔥 2. LẤY SỐ ĐƠN HÔM NAY (Hàm mới thêm vào nè ní)
        long todayCount = bookingRepository.countActiveBookingsByDate(today);

        // 3. Tính toán mốc thời gian 6 tháng trước phục vụ vẽ biểu đồ
        LocalDate sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1);
        List<Object[]> rawPeriodData = bookingRepository.getRevenueByPeriod(sixMonthsAgo);

        List<Overview.MonthlyRevenueDTO> sixMonthsList = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate targetMonth = today.minusMonths(i);
            int m = targetMonth.getMonthValue();
            int y = targetMonth.getYear();
            String label = String.format("Tháng %02d/%d", m, y);

            BigDecimal amount = BigDecimal.ZERO;
            for (Object[] row : rawPeriodData) {
                int rowYear = ((Number) row[0]).intValue();
                int rowMonth = ((Number) row[1]).intValue();
                if (rowYear == y && rowMonth == m) {
                    amount = new BigDecimal(row[2].toString());
                    break;
                }
            }
            sixMonthsList.add(new Overview.MonthlyRevenueDTO(label, amount));
        }

        // 4. Trả về kết quả bọc gọn gàng trong DTO
        return Overview.builder()
                .currentMonthRevenue(currentMonthRev)
                .todayBookingsCount(todayCount) // 🌟 ĐÍT SỐ LIỆU ĐƠN VÀO ĐÂY NHA NÍ!
                .sixMonthsRevenue(sixMonthsList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        // Lấy toàn bộ đơn hàng trong cơ sở dữ liệu
        return bookingRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}