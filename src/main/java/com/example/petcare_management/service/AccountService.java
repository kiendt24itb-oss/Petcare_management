package com.example.petcare_management.service;

import com.example.petcare_management.config.JwtTokenProvider;
import com.example.petcare_management.dto.JwtResponse;
import com.example.petcare_management.dto.LoginRequest;
import com.example.petcare_management.dto.RegisterRequest;
import com.example.petcare_management.entity.Account;
import com.example.petcare_management.entity.Staff;
import com.example.petcare_management.entity.enums.AccountStatus;
import com.example.petcare_management.entity.enums.Role;
import com.example.petcare_management.repository.AccountRepository;
import com.example.petcare_management.repository.StaffRepository; // 🌟 THÊM: Tiêm Repo để quét người nhà
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🌟 THÊM: Đảm bảo tính nhất quán dữ liệu

import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StaffRepository staffRepository; // 🌟 THÊM: Để kiểm tra xem Admin đã tạo hồ sơ nhân viên trước chưa

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 1. LOGIC ĐĂNG KÝ TÀI KHOẢN (Mở cổng thông minh - Tự bắt cặp Staff)
    @Transactional // 🌟 THÊM: Đảm bảo tạo tài khoản và map ngược vào Staff chạy chung 1 phiên giao dịch
    public String register(RegisterRequest request) {
        // Kiểm tra trùng lặp Username
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập này có người xài rồi khứa ơi!");
        }

        // Kiểm tra trùng lặp Email dưới bảng Account
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email này đã được đăng ký hệ thống rồi!");
        }

        // 🎯 GIẢI QUYẾT LỖI 2: Quét tìm xem Email này đã được Admin tạo sẵn ở bảng Staffs chưa
        Optional<Staff> existingStaffOpt = staffRepository.findByEmail(request.getEmail());

        // Mặc định đăng ký tự do ngoài trang chủ là CUSTOMER
        Role targetRole = Role.CUSTOMER;

        // Nếu tìm thấy Email nằm trong bảng Staffs, tự hiểu đây là Nhân viên đang ra tạo tài khoản đăng nhập
        if (existingStaffOpt.isPresent()) {
            targetRole = Role.STAFF; // Ép luôn Role thành STAFF chuẩn chỉ
        }

        // Mã hóa mật khẩu bằng BCryptPasswordEncoder từ SecurityConfig
        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(targetRole) // Gán Role linh hoạt theo kết quả kiểm tra
                .status(AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        // 🔥 KÍCH HOẠT AUTO-LINK NGƯỢC: Nếu đúng là Staff của tiệm, cập nhật ngược ID tài khoản vào bảng Staffs
        if (existingStaffOpt.isPresent()) {
            Staff staff = existingStaffOpt.get();

            // Đề phòng trường hợp hiếm là hồ sơ nhân viên này đã bị tài khoản khác cướp mất link
            if (staff.getAccount() != null) {
                throw new RuntimeException("Hồ sơ nhân viên sở hữu Email này đã liên kết với một tài khoản khác!");
            }

            staff.setAccount(savedAccount); // Gán "sợi dây tơ hồng"
            staffRepository.save(staff);    // Cập nhật lại DB
        }

        return "Đăng ký tài khoản thành công!";
    }

    // 2. LOGIC ĐĂNG NHẬP (Sinh Token thật chứa thông tin cá nhân)
    public JwtResponse login(LoginRequest request) {
        // Tìm xem tài khoản có tồn tại không
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!"));

        // Chặn luôn từ vòng gửi xe nếu tài khoản đang bị khóa (LOCKED)
        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa do vi phạm chính sách!");
        }

        // Kiểm tra mật khẩu người dùng nhập với mật khẩu đã băm trong DB
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        // Gọi thằng JwtTokenProvider xịn để sinh ra chuỗi Token thật có chứa CLAIM ROLE
        String realToken = jwtTokenProvider.generateToken(account);

        // Trả về đúng quả JwtResponse đầy đủ vũ khí cho Front-end xài
        return JwtResponse.builder()
                .token(realToken)
                .type("Bearer")
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .role(account.getRole().name())
                .build();
    }
}