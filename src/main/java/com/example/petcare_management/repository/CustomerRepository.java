package com.example.petcare_management.repository;

import com.example.petcare_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // 1. Phục vụ tự động sinh mã khách hàng tăng dần (KH-001 -> KH-002) lúc đăng ký thành công
    @Query(value = "SELECT c.customer_code FROM customers c WHERE c.customer_code LIKE 'KH-%' ORDER BY c.customer_code DESC LIMIT 1", nativeQuery = true)
    String findLatestCustomerCode();

    // 2. Phục vụ tính năng Khách hàng tự đăng nhập và xem/sửa thông tin cá nhân của họ
    Optional<Customer> findByAccount_Username(String username);

    // 3. 🌟 PHỤC VỤ THANH TÌM KIẾM CHUNG TRÊN DASHBOARD
    // Admin gõ từ khóa, tìm xuyên ba trường: Mã KH, Họ tên, hoặc Số điện thoại
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Customer> searchGlobalFromDashboard(@Param("keyword") String keyword);
}