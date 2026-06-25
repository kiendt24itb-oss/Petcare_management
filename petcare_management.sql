-- =============================================================================
-- KHỞI TẠO DATABASE PETCARE MANAGEMENT (BẢN CHUẨN HOÀN THIỆN VẬN HÀNH)
-- =============================================================================
DROP DATABASE IF EXISTS petcare_management;
CREATE DATABASE petcare_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE petcare_management;

-- 1. BẢNG TÀI KHOẢN (ACCOUNTS)
CREATE TABLE accounts (
    account_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'STAFF', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. BẢNG CHỨC VỤ (POSITIONS)
CREATE TABLE positions (
    position_id INT PRIMARY KEY AUTO_INCREMENT,
    position_code VARCHAR(20) UNIQUE NOT NULL, 
    position_name VARCHAR(100) NOT NULL,      
    description TEXT,
    base_allowance DECIMAL(38,2) DEFAULT 0.00          
);

-- 3. BẢNG KHÁCH HÀNG (CUSTOMERS)
CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(10) NOT NULL UNIQUE, 
    account_id INT NOT NULL UNIQUE,
    full_name VARCHAR(250) NOT NULL,
    age INT,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    email VARCHAR(250) UNIQUE, 
    phone VARCHAR(15),
    address VARCHAR(500),
    avatar VARCHAR(500) DEFAULT NULL,
    violation_count INT DEFAULT 0, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 4. BẢNG NHÂN VIÊN (STAFFS)
CREATE TABLE staffs (
    staff_id INT PRIMARY KEY AUTO_INCREMENT,
    staff_code VARCHAR(10) NOT NULL UNIQUE, 
    account_id INT UNIQUE NOT NULL,
    full_name VARCHAR(250) NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    email VARCHAR(250) UNIQUE,
    cccd VARCHAR(12) NOT NULL UNIQUE,
    phone VARCHAR(15),
    avatar VARCHAR(500) DEFAULT NULL,
    address VARCHAR(500),
    position_id INT,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
    salary DECIMAL(38,2) DEFAULT 0.00,
    bonus DECIMAL(38,2) DEFAULT 0.00,
    total_salary DECIMAL(38,2) GENERATED ALWAYS AS (salary + bonus) STORED,
    hire_date DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (position_id) REFERENCES positions(position_id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 5. BẢNG ĐIỂM DANH NHÂN VIÊN (STAFF_ATTENDANCE)
CREATE TABLE staff_attendance (
    attendance_id INT PRIMARY KEY AUTO_INCREMENT,
    staff_id INT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time DATETIME NOT NULL,
    check_out_time DATETIME DEFAULT NULL,
    status ENUM('PRESENT', 'LATE', 'ABSENT') DEFAULT 'PRESENT',
    note TEXT,

    FOREIGN KEY (staff_id) REFERENCES staffs(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY unique_staff_date (staff_id, attendance_date)
);

CREATE TABLE staff_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    staff_id INT NOT NULL,
    log_time DATETIME DEFAULT CURRENT_TIMESTAMP, -- Lưu cả ngày và giờ để hiển thị cho chuẩn
    log_text TEXT NOT NULL,  -- Ví dụ: "Nhân viên Nguyễn Văn A đã check-in muộn 15 phút"
    
    FOREIGN KEY (staff_id) REFERENCES staffs(staff_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 6. BẢNG PHÒNG & VỊ TRÍ (ROOMS)
CREATE TABLE rooms (
    room_id INT PRIMARY KEY AUTO_INCREMENT,
    room_code VARCHAR(10) NOT NULL UNIQUE,     
    room_name VARCHAR(100) NOT NULL,          
    room_category ENUM('HEALTH', 'SPA', 'HOTEL') NOT NULL, 
    room_type ENUM('STANDARD', 'DELUXE', 'VIP', 'SPA_TABLE', 'SPA_TUB', 'CLINIC_ROOM', 'SURGERY_ROOM') DEFAULT 'STANDARD',  
    status ENUM('AVAILABLE', 'BUSY', 'BOOKED', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    note TEXT
);

-- 7. BẢNG DỊCH VỤ (SERVICES) - 🌟 ĐÃ THÊM KHÓA NGOẠI room_id ĐỊNH DANH PHÒNG MẶC ĐỊNH
CREATE TABLE services (
    service_id INT PRIMARY KEY AUTO_INCREMENT,
    service_code VARCHAR(10) NOT NULL UNIQUE, 
    service_name VARCHAR(150) NOT NULL,
    category ENUM('HEALTH', 'SPA', 'HOTEL') NOT NULL, 
    price DECIMAL(38,2) NOT NULL,
    duration_minutes INT,                      
    room_id INT, -- Phòng mặc định thực hiện dịch vụ này
    description TEXT,
    status TINYINT(1) DEFAULT 1,

    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 8. BẢNG THÚ CƯNG (PETS)
CREATE TABLE pets (
    pet_id INT PRIMARY KEY AUTO_INCREMENT,
    pet_code VARCHAR(10) NOT NULL UNIQUE, 
    customer_id INT NOT NULL,
    pet_name VARCHAR(250) NOT NULL,
    species VARCHAR(100),
    breed VARCHAR(100),
    age INT,
    weight DOUBLE, 
    gender ENUM('MALE', 'FEMALE'),
    avatar VARCHAR(500) DEFAULT NULL,
    health_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 9. BẢNG ĐẶT LỊCH CHÍNH (BOOKINGS) - 🌟 ĐÃ THÊM REJECTED VÀ payment_method
CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_code VARCHAR(10) NOT NULL UNIQUE, 
    customer_id INT NOT NULL, 
    total_price DECIMAL(38,2) NOT NULL DEFAULT 0.00, 
    booking_date DATE NOT NULL,
    booking_time TIME NOT NULL,
    payment_status ENUM('UNPAID', 'PAID') DEFAULT 'UNPAID', 
    payment_method VARCHAR(50) DEFAULT 'CASH', -- Phương thức thanh toán (CASH, BANK_TRANSFER, VNPAY...)
    status ENUM('PENDING_APPROVAL', 'WAITING', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'REJECTED') DEFAULT 'WAITING', 
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_booking_date (booking_date)
);

-- 10. BẢNG CHI TIẾT ĐẶT LỊCH (BOOKING_DETAILS)
CREATE TABLE booking_details (
    detail_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    pet_id INT NOT NULL,      
    service_id INT NOT NULL,  
    staff_id INT,              
    room_id INT,               
    price DECIMAL(38,2) NOT NULL DEFAULT 0.00, 

    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (pet_id) REFERENCES pets(pet_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES staffs(staff_id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 11. BẢNG NHẬT KÝ LỊCH SỬ BIẾN ĐỘNG (TIMELINE REAL-TIME)
CREATE TABLE booking_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    log_time TIME NOT NULL, 
    log_text TEXT NOT NULL,  
    
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 12. BẢNG THÔNG BÁO HỆ THỐNG (NOTIFICATIONS)
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,                   
    title VARCHAR(255) NOT NULL,               
    content TEXT NOT NULL,                     
    is_read BOOLEAN DEFAULT FALSE,             
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE ON UPDATE CASCADE
);


-- =============================================================================
-- DỮ LIỆU MẪU CHUẨN ĐỂ TEST ĐIỀU PHỐI VÀ DASHBOARD VẬN HÀNH
-- =============================================================================

-- [1] DATA ACCOUNTS
INSERT INTO accounts (account_id, username, email, password, role, status) VALUES 
(1, 'kien12', 'admin@petcare.com', '101001', 'ADMIN', 'ACTIVE'),
(2, 'nv_spa_01', 'staff1@petcare.com', '123456', 'STAFF', 'ACTIVE'), 
(3, 'bs_minh_vet', 'minh_vet@petcare.com', '123456', 'STAFF', 'ACTIVE'),
(4, 'nv_le_tan', 'letan@petcare.com', '123456', 'STAFF', 'ACTIVE'),
(5, 'khach_kien', 'kienkhach@gmail.com', 'password123', 'CUSTOMER', 'ACTIVE'),
(6, 'hoang_spam', 'hoangpham@gmail.com', 'password123', 'CUSTOMER', 'ACTIVE'),
(7, 'linh_badboy', 'linhnguyen@gmail.com', 'password123', 'CUSTOMER', 'LOCKED'),
(8, 'ngoc_trinh', 'trinhnguyen@gmail.com', 'password123', 'CUSTOMER', 'ACTIVE'),
(9, 'den_vau', 'denvau@gmail.com', 'password123', 'CUSTOMER', 'ACTIVE');

-- [2] DATA POSITIONS
INSERT INTO positions (position_id, position_code, position_name, description, base_allowance) VALUES 
(1, 'BS', 'Bác sĩ thú y', 'Khám bệnh, chẩn đoán, phẫu thuật', 1500000.00),
(2, 'KTV', 'Kỹ thuật viên Spa', 'Tắm rửa, cắt tỉa lông thẩm mỹ', 500000.00),
(3, 'LT', 'Nhân viên lễ tân', 'Check-in, trực tổng đài, thu ngân', 300000.00);

-- [3] DATA CUSTOMERS
INSERT INTO customers (customer_id, customer_code, account_id, full_name, phone, address, violation_count) VALUES 
(1, 'KH-001', 5, 'Nguyễn Trung Kiên', '0912345678', 'Hải Châu, Đà Nẵng', 0),   
(2, 'KH-002', 6, 'Phạm Minh Hoàng', '0905999888', 'Thanh Khê, Đà Nẵng', 3),  
(3, 'KH-003', 7, 'Nguyễn Tiến Linh', '0935111222', 'Sơn Trà, Đà Nẵng', 5),   
(4, 'KH-004', 8, 'Trần Ngọc Trinh', '0905666777', 'Liên Chiểu, Đà Nẵng', 1),  
(5, 'KH-005', 9, 'Nguyễn Đức Cường', '0905444555', 'Cẩm Lệ, Đà Nẵng', 0);

-- [4] DATA STAFFS
INSERT INTO staffs (staff_id, staff_code, account_id, full_name, email, cccd, phone, position_id, salary, bonus) VALUES 
(1, 'NV-001', 2, 'Lê Văn Spa', 'staff1@petcare.com', '012345678912', '0905111222', 2, 8000000, 200000), 
(2, 'NV-002', 3, 'Bác Sĩ Nguyễn Minh', 'minh_vet@petcare.com', '012345678913', '0905333444', 1, 16000000, 800000),
(3, 'NV-003', 4, 'Trần Thu Lễ Tân', 'letan@petcare.com', '012345678914', '0905444333', 3, 7000000, 100000);

-- [5] DATA ROOMS
INSERT INTO rooms (room_id, room_code, room_name, room_category, room_type, status) VALUES 
(1, 'R-H01', 'Phòng Khám Lâm Sàng 1', 'HEALTH', 'CLINIC_ROOM', 'AVAILABLE'),
(2, 'R-H02', 'Phòng Phẫu Thuật Vô Trùng', 'HEALTH', 'SURGERY_ROOM', 'BUSY'),
(3, 'R-S01', 'Bàn Cắt Tỉa Thẩm Mỹ 1', 'SPA', 'SPA_TABLE', 'AVAILABLE'),
(4, 'R-S02', 'Bồn Tắm Sục Khử Trùng 1', 'SPA', 'SPA_TUB', 'AVAILABLE'),
(5, 'R-O01', 'Khách Sạn Chuồng Thường S1', 'HOTEL', 'STANDARD', 'AVAILABLE'),
(6, 'R-O02', 'Khách Sạn Căn Hộ Deluxe D1', 'HOTEL', 'DELUXE', 'BUSY'),
(7, 'R-O03', 'Biệt Thự Thú Cưng VIP V1', 'HOTEL', 'VIP', 'MAINTENANCE');

-- [6] DATA SERVICES (Đã gắn room_id mặc định)
INSERT INTO services (service_id, service_code, service_name, category, price, duration_minutes, room_id, description) VALUES 
(1, 'DV-H01', 'Khám tổng quát lâm sàng', 'HEALTH', 150000, 30, 1, 'Kiểm tra tổng thể tai, mắt, mũi, miệng, nhịp tim'),
(2, 'DV-H02', 'Tiêm vắc xin 7 bệnh', 'HEALTH', 250000, 15, 1, 'Tiêm phòng định kỳ các bệnh truyền nhiễm nguy hiểm'),
(3, 'DV-H03', 'Xét nghiệm máu tổng hợp', 'HEALTH', 400000, 45, 1, 'Phân tích các chỉ số sinh hóa, phát hiện bệnh tiềm ẩn'),
(4, 'DV-S01', 'Combo Tắm Rửa Khử Mùi', 'SPA', 200000, 45, 4, 'Tắm, sấy khô, chải lông khử mùi hôi chuyên sâu'),
(5, 'DV-S02', 'Cắt tỉa lông tạo kiểu', 'SPA', 300000, 60, 3, 'Cắt tỉa thẩm mỹ theo yêu cầu của chủ nuôi bởi KTV'),
(6, 'DV-S03', 'Combo Cạo vôi răng & Cắt móng', 'SPA', 180000, 30, 3, 'Vệ sinh răng miệng, lấy cao răng nhẹ và mài dũa móng'),
(7, 'DV-O01', 'Phòng Lưu Trú Standard', 'HOTEL', 200000, 1440, 5, 'Phòng tiêu chuẩn, điều hòa, ăn ngày 2 bữa tiêu chuẩn'),
(8, 'DV-O02', 'Phòng Lưu Trú Deluxe', 'HOTEL', 350000, 1440, 6, 'Không gian rộng rãi, camera giám sát 24/7 cho chủ xem'),
(9, 'DV-O03', 'Phòng Lưu Trú VIP', 'HOTEL', 500000, 1440, 7, 'Phòng VIP biệt thự, có sân chơi riêng, thực đơn bò mỹ');

-- [7] DATA PETS
INSERT INTO pets (pet_id, pet_code, customer_id, pet_name, species, breed, age, weight, gender) VALUES 
(1, 'TC-001', 1, 'Ngáo Husky', 'Chó', 'Husky', 2, 15.5, 'MALE'),
(2, 'TC-002', 1, 'Mèo Mun', 'Mèo', 'Anh lông ngắn', 1, 4.2, 'FEMALE'),
(3, 'TC-003', 2, 'Lu Lu', 'Chó', 'Chihuahua', 3, 2.5, 'MALE'),
(4, 'TC-004', 4, 'Cún Chảnh', 'Chó', 'Poodle', 1, 3.8, 'FEMALE'),
(5, 'TC-005', 5, 'Mèo Mập', 'Mèo', 'Ba Tư', 4, 6.0, 'MALE');

-- [8] DATA BOOKINGS (Sử dụng ngày hiện tại của năm 2026 để test chuẩn Dashboard)
INSERT INTO bookings (booking_id, booking_code, customer_id, total_price, booking_date, booking_time, payment_status, payment_method, status) VALUES 
(1, 'BK-001', 1, 350000, '2026-06-18', '09:00:00', 'PAID', 'CASH', 'COMPLETED'),
(2, 'BK-002', 2, 500000, '2026-06-18', '14:00:00', 'UNPAID', 'CASH', 'PENDING_APPROVAL'),
(3, 'BK-003', 4, 300000, '2026-06-18', '10:30:00', 'UNPAID', 'BANK_TRANSFER', 'WAITING'),
(4, 'BK-004', 5, 550000, '2026-06-18', '13:00:00', 'PAID', 'VNPAY', 'PROCESSING'),
(5, 'BK-005', 4, 250000, '2026-06-18', '16:00:00', 'UNPAID', 'CASH', 'REJECTED');

-- [9] DATA BOOKING DETAILS
INSERT INTO booking_details (detail_id, booking_id, pet_id, service_id, staff_id, room_id, price) VALUES 
(1, 1, 1, 1, 2, 1, 150000), 
(2, 1, 1, 4, 1, 4, 200000), 
(3, 2, 3, 9, NULL, 7, 500000), 
(4, 3, 4, 5, 1, 3, 300000), 
(5, 4, 5, 3, 2, 1, 400000), 
(6, 4, 5, 6, 1, 3, 150000),
(7, 5, 4, 2, 2, 1, 250000);

-- [10] DATA TIMELINE LOGS
INSERT INTO booking_logs (booking_id, log_time, log_text) VALUES 
(1, '08:45:00', 'Khách hàng Nguyễn Trung Kiên đưa bé Ngáo Husky đến quầy check-in.'),
(1, '09:00:00', 'Bác sĩ Nguyễn Minh hoàn thành khám tổng quát lâm sàng.'),
(1, '09:45:00', 'KTV Lê Văn Spa hoàn thành gói tắm khử mùi. Bàn giao thú cưng và thanh toán thành công.'),
(2, '11:20:00', 'Hệ thống kích hoạt cảnh báo: Khách hàng Phạm Minh Hoàng có 3 lần vi phạm hủy lịch liên tiếp.'),
(2, '11:20:05', 'Đơn đặt lịch BK-002 bị chuyển trạng thái sang [Chờ Admin duyệt tay]. Không xếp phòng tự động.'),
(3, '10:00:00', 'Hệ thống nhận lịch hẹn trực tuyến từ tài khoản Trần Ngọc Trinh. Trạng thái: Chờ thực hiện.'),
(4, '13:00:00', 'Bé Mèo Mập đã được tiếp nhận vào phòng khám để lấy mẫu Xét nghiệm máu tổng hợp.'),
(4, '13:45:00', 'Lấy mẫu thành công. Bé được chuyển sang khu vực Spa để lấy cao răng và cắt móng.'),
(5, '16:15:00', 'Đơn đặt lịch bị Admin từ chối phê duyệt do tài khoản có dấu hiệu cố tình spam đặt/hủy.');

-- [11] DATA NOTIFICATIONS
INSERT INTO notifications (notification_id, account_id, title, content, is_read) VALUES 
(1, 6, 'Lịch hẹn đang chờ xét duyệt', 'Lịch hẹn BK-002 của bạn đang chờ Admin phê duyệt trực tiếp do tài khoản có lịch sử hủy lịch nhiều lần.', FALSE),
(2, 6, 'Cảnh báo vi phạm chính sách', 'Tài khoản của bạn đã cán mốc 3 lần hủy lịch liên tiếp. Chức năng tự động duyệt đã tạm khóa.', TRUE),
(3, 1, 'Yêu cầu duyệt đơn hàng mới', 'Khách hàng vi phạm Phạm Minh Hoàng vừa thực hiện đặt ca lưu trú VIP (BK-002). Vui lòng kiểm tra!', FALSE),
(8, 8, 'Lịch hẹn bị từ chối', 'Rất tiếc, đơn đặt lịch BK-005 của bạn đã bị Admin từ chối do phát hiện hành vi lạm dụng đặt chỗ.', FALSE);