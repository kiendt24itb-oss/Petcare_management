com.example.petcare_management/
│
├── PetcareManagementApplication.java # Điểm khởi chạy ứng dụng Spring Boot
│
├── config/ # TẦNG CẤU HÌNH (CONFIG LAYER)
│ ├── SecurityConfig.java # Cấu hình bảo mật Spring Security, quy tắc phân quyền endpoint
│ ├── JwtTokenProvider.java # Tạo, validate, giải mã token JWT
│ └── JwtAuthenticationFilter.java # Filter kiểm tra & xác thực token JWT từ request header
│
├── entity/ # TẦNG THỰC THỂ DỮ LIỆU (ENTITY LAYER)
│ ├── Account.java # Thực thể tài khoản (Username, password, email, role)
│ ├── Customer.java # Thực thể khách hàng (Tên, số điện thoại, địa chỉ)
│ ├── Staff.java # Thực thể nhân viên (Tên, chuyên môn, lương)
│ ├── Pet.java # Thực thể thú cưng (Tên, chủng loại, tuổi, chủ sở hữu)
│ ├── Service.java # Thực thể dịch vụ (Tên, mô tả, giá cả)
│ ├── Room.java # Thực thể phòng (Số phòng, sức chứa, trạng thái)
│ ├── Booking.java # Thực thể đặt dịch vụ (Khách hàng, thú cưng, dịch vụ, ngày giờ)
│ ├── Attendance.java # Thực thể chấm công (Nhân viên, ngày, giờ vào/ra)
│ └── WaitingList.java # Thực thể danh sách chờ (Khách hàng, thú cưng, ngày yêu cầu)
│
├── repository/ # TẦNG TRỪU TƯỢNG DỮ LIỆU (REPOSITORY LAYER)
│ ├── AccountRepository.java # CRUD tài khoản từ cơ sở dữ liệu
│ ├── CustomerRepository.java # CRUD khách hàng từ cơ sở dữ liệu
│ ├── StaffRepository.java # CRUD nhân viên từ cơ sở dữ liệu
│ ├── PetRepository.java # CRUD thú cưng từ cơ sở dữ liệu
│ ├── ServiceRepository.java # CRUD dịch vụ từ cơ sở dữ liệu
│ ├── RoomRepository.java # CRUD phòng từ cơ sở dữ liệu
│ ├── BookingRepository.java # CRUD đặt dịch vụ từ cơ sở dữ liệu
│ ├── AttendanceRepository.java # CRUD chấm công từ cơ sở dữ liệu
│ └── WaitingListRepository.java # CRUD danh sách chờ từ cơ sở dữ liệu
│
├── service/ # TẦNG LOGIC NGHIỆP VỤ (SERVICE LAYER)
│ ├── AccountService.java # Xử lý logic đăng nhập, đăng ký, xác thực người dùng
│ ├── CustomerService.java # Xử lý logic quản lý khách hàng (CRUD, tìm kiếm)
│ ├── StaffService.java # Xử lý logic quản lý nhân viên (CRUD, phân công)
│ ├── PetService.java # Xử lý logic quản lý thú cưng (CRUD, gộp/tách khách hàng)
│ ├── ServiceService.java # Xử lý logic quản lý dịch vụ (CRUD, xác định giá)
│ ├── RoomService.java # Xử lý logic quản lý phòng (CRUD, kiểm tra trạng thái)
│ ├── BookingService.java # Xử lý logic đặt dịch vụ (tạo, cập nhật, hủy, kiểm tra ngày)
│ ├── AttendanceService.java # Xử lý logic chấm công (ghi nhận vào/ra, tính giờ làm)
│ └── WaitingListService.java # Xử lý logic danh sách chờ (thêm, xóa, chuyển sang booking)
│
├── controller/ # TẦNG ĐIỂM CUỐI API (CONTROLLER LAYER)
│ ├── AuthController.java # Endpoint đăng nhập (/auth/login), đăng ký (/auth/register)
│ ├── CustomerController.java # Endpoint CRUD khách hàng (/api/customers)
│ ├── StaffController.java # Endpoint CRUD nhân viên (/api/staffs)
│ ├── PetController.java # Endpoint CRUD thú cưng (/api/pets)
│ ├── ServiceController.java # Endpoint CRUD dịch vụ (/api/services)
│ ├── RoomController.java # Endpoint CRUD phòng (/api/rooms)
│ ├── BookingController.java # Endpoint CRUD đặt dịch vụ (/api/bookings)
│ ├── AttendanceController.java # Endpoint CRUD chấm công (/api/attendances)
│ └── WaitingListController.java # Endpoint CRUD danh sách chờ (/api/waiting-lists)
│
├── dto/ # TẦNG ĐỐI TƯỢNG TRUYỀN DỮ LIỆU (DTO LAYER)
│ ├── LoginRequest.java # DTO nhận username/password từ client khi đăng nhập
│ ├── RegisterRequest.java # DTO nhận thông tin đăng ký tài khoản từ client
│ ├── JwtResponse.java # DTO trả về token JWT & thông tin user sau khi xác thực
│ └── BookingRequestDTO.java # DTO nhận thông tin đặt dịch vụ từ client
│
├── exception/ # TẦNG XỬ LÝ LỖI (EXCEPTION LAYER)
│ ├── GlobalExceptionHandler.java # Handler toàn cục xử lý các exception & trả về lỗi HTTP thống nhất
│ └── ResourceNotFoundException.java # Exception tùy chỉnh khi tài nguyên không tìm thấy (404)
│
└── utils/ # TẦNG TIỆN ÍCH (UTILITIES)
└── [Các class hỗ trợ khác nếu có] # Helper, Validator, Converter
