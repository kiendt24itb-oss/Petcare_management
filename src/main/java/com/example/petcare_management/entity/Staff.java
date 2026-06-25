package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "staffs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_code", nullable = false, unique = true, length = 10)
    private String staffCode; // NV-A001, NV-A002...

    // ✅ Đã sửa: Cho phép tạo Staff trước, gắn Account sau (hoặc ngược lại)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true, unique = true)
    private Account account;

    // 🌟 CHÚ Ý 1: Đổi sang nullable = true để khi tự kích hoạt hồ sơ rỗng, hệ thống không bị crash.
    // Nhân viên sẽ tự sửa lại họ tên thật qua UI sau.
    @Column(name = "full_name", nullable = true, length = 250)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "email", unique = true, length = 250)
    private String email;

    // 🌟 CHÚ Ý 2: Đổi sang nullable = true vì tài khoản "đột biến" từ UI sang sẽ chưa có số CCCD ngay lập tức.
    @Column(name = "cccd", nullable = true, unique = true, length = 12)
    private String cccd;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "address", length = 500)
    private String address;

    // ✅ Đã kiểm tra: nullable mặc định của @JoinColumn là true, cấu hình thế này là cực chuẩn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = true)
    private Position position; // Chức vụ của nhân viên (Chờ Admin gán)

    @Column(name = "salary")
    private BigDecimal salary; // Lương cơ bản

    @Column(name = "bonus")
    private BigDecimal bonus; // Tiền thưởng thêm

    @Column(name = "total_salary", insertable = false, updatable = false)
    private BigDecimal totalSalary;

    @CreationTimestamp
    @Column(name = "hire_date", updatable = false)
    private LocalDateTime hireDate;
}