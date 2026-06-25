package com.example.petcare_management.service;

import com.example.petcare_management.dto.StaffRequest;
import com.example.petcare_management.dto.StaffResponse;
import com.example.petcare_management.entity.*;
import com.example.petcare_management.entity.enums.Role;
import com.example.petcare_management.repository.*;
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(StaffRequest request) {
        // 1. Chỉ chặn nếu trùng số CCCD cá nhân
        if (staffRepository.existsByCccd(request.getCccd())) {
            throw new RuntimeException("Số CCCD này đã được đăng ký trên hệ thống!");
        }

        // 2. Kiểm tra xem Email đã tồn tại dưới bảng Staff chưa (Trùng Staff khác thì mới chặn)
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email này đã tồn tại trong danh sách nhân viên!");
        }

        Account savedAccount = null;

        // 3. Quét tìm xem dưới bảng Account đã có tài khoản nào đăng ký bằng Email này chưa
        java.util.Optional<Account> existingAccountOpt = accountRepository.findByEmail(request.getEmail());

        if (existingAccountOpt.isPresent()) {
            // 🔥 GIẢI QUYẾT LỖI 1: Nếu tài khoản đã có sẵn (do đăng ký trước), bốc ra dùng luôn
            savedAccount = existingAccountOpt.get();

            // Tiện tay nâng cấp Role lên STAFF luôn nếu tài khoản gốc đang là CUSTOMER
            if (savedAccount.getRole() != Role.STAFF) {
                savedAccount.setRole(Role.STAFF);
                accountRepository.save(savedAccount);
            }
        } else {
            // Luồng thông thường: Nếu chưa có tài khoản nào dùng email này, Admin tự tạo Account mới
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                if (accountRepository.existsByUsername(request.getUsername())) {
                    throw new RuntimeException("Tên tài khoản (username) đã tồn tại trên hệ thống!");
                }
                Account account = Account.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword())) // Đã băm pass chuẩn bài
                        .role(Role.STAFF)
                        .build();
                savedAccount = accountRepository.save(account);
            }
        }

        // 4. Tìm chức vụ và lưu Staff như bình thường
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ hợp lệ!"));

        String nextStaffCode = generateNextStaffCode();

        Staff staff = Staff.builder()
                .staffCode(nextStaffCode)
                .account(savedAccount) // Tự động liên kết tài khoản (nếu có)
                .fullName(request.getFullName())
                .gender(request.getGender())
                .email(request.getEmail())
                .cccd(request.getCccd())
                .phone(request.getPhone())
                .avatar(request.getAvatar())
                .address(request.getAddress())
                .position(position)
                .salary(request.getSalary())
                .bonus(request.getBonus())
                .build();

        return mapToResponse(staffRepository.save(staff));
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(Integer staffId, StaffRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên!"));

        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Check null cho staff.getAccount() đề phòng nhân viên chưa được gán tài khoản
        if (!isAdmin && (staff.getAccount() == null || !staff.getAccount().getUsername().equals(currentUsername))) {
            throw new RuntimeException("Ní không có quyền chỉnh sửa hồ sơ của nhân viên khác!");
        }

        staff.setFullName(request.getFullName());
        staff.setGender(request.getGender());
        staff.setPhone(request.getPhone());
        staff.setAvatar(request.getAvatar());
        staff.setAddress(request.getAddress());
        staff.setCccd(request.getCccd());
        staff.setEmail(request.getEmail());

        if (isAdmin) {
            Position position = positionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ hợp lệ!"));
            staff.setPosition(position);
            staff.setSalary(request.getSalary());
            staff.setBonus(request.getBonus());
        }

        return mapToResponse(staffRepository.save(staff));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffByUsername(String username) {
        Staff staff = staffRepository.findByAccountUsernameWithAccount(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên ứng với tài khoản này!"));
        return mapToResponse(staff);
    }

    // 🌟 ĐÃ NÂNG CẤP HỢP NHẤT: Xử lý an toàn luồng lấy thông tin cá nhân hoặc tự động gán tài khoản theo Email
    @Override
    @Transactional // 🌟 Nhớ phải có Transactional vì luồng này sẽ tự động INSERT nếu chưa có hồ sơ
    public StaffResponse getStaffByUsernameOrAutoLink(String username) {
        // 1. Quét tìm xem tài khoản đăng nhập này đã liên kết với Staff nào từ trước chưa
        Optional<Staff> staffOpt = staffRepository.findByAccountUsernameWithAccount(username);
        if (staffOpt.isPresent()) {
            return mapToResponse(staffOpt.get());
        }

        // 2. Nếu CHƯA LIÊN KẾT, lấy thực thể Account để bốc Email ra dò tìm
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hợp lệ trên hệ thống!"));

        // 3. Quét dưới bảng staffs xem có nhân viên nào trùng email của tài khoản này không (Auto-Link xuôi)
        Optional<Staff> staffByEmailOpt = staffRepository.findByEmail(account.getEmail());

        if (staffByEmailOpt.isPresent()) {
            Staff staff = staffByEmailOpt.get();
            // Nếu tìm thấy nhân viên và chưa được gán tài khoản nào -> Tiến hành kết nối!
            if (staff.getAccount() == null) {
                staff.setAccount(account);
                return mapToResponse(staffRepository.save(staff));
            } else if (!staff.getAccount().getUsername().equals(username)) {
                throw new RuntimeException("Email của nhân viên này đã được gán cho một tài khoản khác!");
            }
            return mapToResponse(staff);
        }

        // 🔥 4. TRƯỜNG HỢP CUỐI CÙNG: Tài khoản có quyền STAFF nhưng Admin chưa hề tạo hồ sơ mồi!
        // Hệ thống tự động sinh hồ sơ ban đầu cho Nhân viên tự vào cập nhật
        String nextStaffCode = generateNextStaffCode(); // Tự sinh mã NV-Axxx chuẩn chỉ

        Staff newStaff = Staff.builder()
                .staffCode(nextStaffCode)
                .account(account) // Link luôn tài khoản đăng nhập vào
                .fullName(account.getUsername()) // Tạm thời lấy username làm họ tên, họ tự sửa sau
                .email(account.getEmail()) // Lấy lại Email lúc đăng ký
                .gender(com.example.petcare_management.entity.enums.Gender.MALE) // Mặc định đại 1 cái giới tính
                .salary(java.math.BigDecimal.ZERO) // Lương chờ Admin cấu hình (để tạm = 0)
                .bonus(java.math.BigDecimal.ZERO)  // Thưởng chờ Admin cấu hình (để tạm = 0)
                .position(null) // Chức vụ chờ Admin gán sau
                .build();

        // Lưu xuống DB để tạo mới hồ sơ thành công
        Staff savedStaff = staffRepository.save(newStaff);

        return mapToResponse(savedStaff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaffs() {
        return staffRepository.getAllStaffDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> searchStaffs(String keyword, Integer positionId) {
        String processedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return staffRepository.searchStaffs(processedKeyword, positionId);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên có ID: " + staffId));
        return mapToResponse(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffsByDepartment(String department) {
        String targetPositionCode;

        switch (department.toUpperCase()) {
            case "SPA": targetPositionCode = "KTV"; break;
            case "HEALTH": targetPositionCode = "BS"; break;
            case "HOTEL": targetPositionCode = "LT"; break;
            default:
                throw new IllegalArgumentException("Phòng ban không hợp lệ nka ní: " + department);
        }

        return staffRepository.findByPosition_PositionCode(targetPositionCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateNextStaffCode() {
        String latestCode = staffRepository.findLatestStaffCode().orElse(null);
        return CodeGenerator.generateNextCode(latestCode, "NV");
    }

    private StaffResponse mapToResponse(Staff staff) {
        return StaffResponse.builder()
                .staffId(staff.getStaffId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .gender(staff.getGender())
                .email(staff.getEmail())
                .phone(staff.getPhone())
                .avatar(staff.getAvatar())
                .address(staff.getAddress())
                .cccd(staff.getCccd())
                .positionId(staff.getPosition() != null ? staff.getPosition().getPositionId() : null)
                .positionName(staff.getPosition() != null ? staff.getPosition().getPositionName() : "Chưa cấu hình (Chờ Admin gán)")
                .salary(staff.getSalary())
                .bonus(staff.getBonus())
                .totalSalary(staff.getTotalSalary() != null ? staff.getTotalSalary() : (staff.getSalary() != null && staff.getBonus() != null ? staff.getSalary().add(staff.getBonus()) : java.math.BigDecimal.ZERO))
                .username(staff.getAccount() != null ? staff.getAccount().getUsername() : "Chưa cấp tài khoản")
                .hireDate(staff.getHireDate())
                .build();
    }
}