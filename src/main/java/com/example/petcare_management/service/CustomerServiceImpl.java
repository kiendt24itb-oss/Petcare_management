package com.example.petcare_management.service;

import com.example.petcare_management.dto.CustomerProfileResponse;
import com.example.petcare_management.dto.CustomerRequest;
import com.example.petcare_management.dto.CustomerResponse;
import com.example.petcare_management.entity.Account;
import com.example.petcare_management.entity.Customer;
import com.example.petcare_management.repository.AccountRepository;
import com.example.petcare_management.repository.CustomerRepository;
import com.example.petcare_management.repository.PetRepository;
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService { // ✅ ĐÃ GIỮ ĐÚNG TÊN CLASS CỦA NÍ

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public CustomerResponse createOrUpdateProfile(String username, CustomerRequest request) {
        // 1. Tìm Account của khứa đang sửa từ Token/Session
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trên hệ thống!"));

        // 2. Đồng bộ Email chuẩn: Nếu đổi sang mail mới toanh, check xem có đụng hàng với ai không
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(account.getEmail()) && accountRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email này đã được sử dụng bởi một tài khoản khác!");
            }
            account.setEmail(newEmail);
            accountRepository.save(account);
        }

        // 3. Tìm bản ghi Customer chắc chắn đã được tạo sẵn từ lúc đăng ký hoặc getProfile lần đầu
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu Khách hàng liên kết!"));

        // 4. Đổ dữ liệu cập nhật từ Form vào Entity Customer
        customer.setFullName(request.getFullName());
        customer.setAge(request.getAge());
        customer.setGender(request.getGender());
        customer.setEmail(account.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            customer.setAvatar(request.getAvatar());
        }

        // 5. Lưu xuống DB và trả về Response sạch đẹp
        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    // 🌟 ĐÃ SỬA: Tự động khởi tạo hồ sơ Customer nếu tài khoản mới toanh chưa có thông tin
    @Override
    @Transactional // ✅ Đổi thành Transactional thường để ghi nhận lưu xuống DB
    public CustomerResponse getProfileByUsername(String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseGet(() -> {
                    // 1. Nếu chưa có dòng nào ở bảng customers, bốc Account gốc ra lấy thông tin nền
                    Account account = accountRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại trên hệ thống!"));

                    // 2. Gọi CodeGenerator lấy mã nhảy tự động kế tiếp (Ví dụ: KH-A001)
                    String latestCode = customerRepository.findLatestCustomerCode();
                    String nextCode = CodeGenerator.generateNextCode(latestCode, "KH");

                    // 3. Khởi tạo thực thể Customer mặc định ban đầu
                    Customer newCustomer = Customer.builder()
                            .customerCode(nextCode)
                            .account(account)
                            .fullName(account.getUsername()) // Tạm lấy username đắp vào Họ Tên
                            .email(account.getEmail())
                            .violationCount(0)
                            .build();

                    // 4. Lưu xuống DB để các lần gọi sau không bị lọt vào đây nữa
                    return customerRepository.save(newCustomer);
                });

        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfileWithPetsByUsername(String username) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại hoặc chưa cập nhật thông tin!"));

        CustomerProfileResponse response = new CustomerProfileResponse();
        response.setFullName(customer.getFullName());
        response.setCustomerCode(customer.getCustomerCode());

        List<CustomerProfileResponse.PetDTO> petDTOs = petRepository.findByCustomer_Account_Username(username)
                .stream()
                .map(pet -> {
                    CustomerProfileResponse.PetDTO dto = new CustomerProfileResponse.PetDTO();
                    dto.setPetId(pet.getPetId());
                    dto.setPetName(pet.getPetName());
                    dto.setSpecies(pet.getSpecies());
                    return dto;
                })
                .collect(Collectors.toList());

        response.setPets(petDTOs);
        return response;
    }

    // 👑 TÍNH NĂNG CHO ADMIN: Lấy tất cả khách hàng đổ lên bảng quản lý
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 👑 TÍNH NĂNG CHO ADMIN: Tìm kiếm khách hàng liên thông với thanh Search của Dashboard cha
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchGlobalFromDashboard(keyword.trim()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 🌟 LUỒNG TẠO MỚI TỰ ĐỘNG (Bỏ hẳn chữ @Override để không lo lỗi compile lệch interface)
    @Transactional
    public CustomerResponse registerNewCustomer(Account account, String fullName) {
        String latestCode = customerRepository.findLatestCustomerCode();
        String nextCode = CodeGenerator.generateNextCode(latestCode, "KH");

        Customer customer = Customer.builder()
                .customerCode(nextCode)
                .account(account)
                .fullName(fullName)
                .email(account.getEmail())
                .violationCount(0)
                .build();

        return mapToResponse(customerRepository.save(customer));
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .customerCode(customer.getCustomerCode())
                .username(customer.getAccount().getUsername())
                .fullName(customer.getFullName())
                .age(customer.getAge())
                .gender(customer.getGender())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .avatar(customer.getAvatar())
                .violationCount(customer.getViolationCount())
                .accountStatus(customer.getAccount().getStatus())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}