package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // 👤 Tìm tài khoản bằng Username (Phục vụ đăng nhập, bảo mật và bốc SecurityContext)
    Optional<Account> findByUsername(String username);

    // 📧 🌟 THÊM: Tìm tài khoản bằng Email để phục vụ logic Auto-Link (Admin bốc Account từ Email Staff)
    Optional<Account> findByEmail(String email);

    // 🛡️ Kiểm tra trùng Username và Email khi đăng ký
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}