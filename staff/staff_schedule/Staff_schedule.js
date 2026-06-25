// =============================================================================
// 🗄️ FILE: Staff_schedule.js
// Xử lý Liên thông dữ liệu lịch trình của Nhân viên với Server (Gọi API Real-time)
// =============================================================================

// 🌐 Cấu hình địa chỉ Server tập trung - Sửa một nơi, áp dụng toàn bộ file
const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// Bộ nhớ tạm lưu trữ danh sách lịch đặt thật từ API đổ về và ID đơn đang chọn
let realBookings = [];
let activeFilter = "ALL";
let selectedBookingId = null;
let currentToken = null; // Lưu token dùng chung cho các hàm hành động

document.addEventListener("DOMContentLoaded", function () {
  // 🔑 Nhặt token trực tiếp chuẩn bảo mật 3 tầng giống hệt bên Profile
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");

  if (!token) {
    token = localStorage.getItem("petcare_token");
  }
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  // 🧹 KIỂM TRA & CHẶN TOKEN HẾT HẠN TỪ VÒNG GỬI XE
  if (
    token === "null" ||
    token === "undefined" ||
    !token ||
    token.trim() === ""
  ) {
    console.error("❌ Front-End chặn đứng: Không tìm thấy Token hợp lệ!");
    showLocalToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  currentToken = token; // Gán vào biến toàn cục để tái sử dụng
  console.log("🔒 Đăng nhập lịch trình thông quan thành công!");

  // 🚀 Lấy dữ liệu thật từ Server đổ lên lưới danh sách
  fetchStaffSchedule();

  // 1. Gán sự kiện bộ lọc (Tabs)
  const filterBtns = document.querySelectorAll(".btn-filter");
  filterBtns.forEach((btn) => {
    btn.addEventListener("click", (e) => {
      filterBtns.forEach((b) => b.classList.remove("active"));
      e.target.classList.add("active");
      activeFilter = e.target.getAttribute("data-filter");
      renderBookingGrid();
    });
  });

  // 2. Đóng Modal
  document.getElementById("close-modal").addEventListener("click", closeModal);
  window.addEventListener("click", (e) => {
    if (e.target === document.getElementById("task-modal")) closeModal();
  });

  // 3. Lắng nghe nút [BẮT ĐẦU LÀM] -> Gọi API Vào ca sớm
  document
    .getElementById("btn-start-task")
    .addEventListener("click", async () => {
      if (!selectedBookingId) return;
      // ✅ GIỮ NGUYÊN: Khớp chuẩn SecurityConfig đường dẫn /api/bookings/staff/...
      await handleUpdateStatus(
        `staff/start-early/${selectedBookingId}`,
        "Bắt đầu làm",
      );
    });

  // 4. Lắng nghe nút [HOÀN THÀNH CA] -> Gọi API Hoàn thành sớm chủ động
  document
    .getElementById("btn-complete-task")
    .addEventListener("click", async () => {
      if (!selectedBookingId) return;
      // ✅ GIỮ NGUYÊN: Khớp chuẩn SecurityConfig đường dẫn /api/bookings/staff/...
      await handleUpdateStatus(
        `staff/complete-early/${selectedBookingId}`,
        "Hoàn thành ca",
      );
    });

  // 5. Lắng nghe nút [HỦY LỊCH] -> Gọi API Nhân viên hủy ca
  document
    .getElementById("btn-cancel-task")
    .addEventListener("click", async () => {
      if (!selectedBookingId) return;
      const reason = prompt("Ní vui lòng nhập lý do hủy đơn này (bắt buộc):");
      if (reason === null) return; // Bấm hủy prompt

      if (!reason.trim()) {
        showLocalToast(
          "Ní phải nhập lý do thì hệ thống mới cho hủy nha!",
          "error",
        );
        return;
      }

      // ✅ GIỮ NGUYÊN: Khớp chuẩn SecurityConfig đường dẫn /api/bookings/staff/...
      await handleUpdateStatus(
        `staff/cancel/${selectedBookingId}?reason=${encodeURIComponent(reason)}`,
        "Hủy lịch",
      );
    });
});

// =============================================================================
// 📥 HÀM GỌI API LẤY DANH SÁCH LỊCH ĐẶT CỦA KHÁCH HÀNG (ĐÃ SỬA LỖI 403)
// =============================================================================
async function fetchStaffSchedule() {
  try {
    // ✅ CHUẨN HOÁ: Gọi vào đầu phân luồng thông minh, Backend tự bóc Token để trả đúng lịch của Staff
    const response = await fetch(`${BASE_URL}/bookings/my-bookings`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${currentToken}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      realBookings = await response.json();
      console.log("📅 Dữ liệu lịch trình thực tế:", realBookings);
      renderBookingGrid();
    } else {
      showLocalToast(
        `Không thể lấy danh sách ca làm việc! Mã lỗi: ${response.status}`,
        "error",
      );
    }
  } catch (error) {
    console.error("Lỗi kết nối API lịch hẹn:", error);
    showLocalToast("Mất kết nối đường truyền tới máy chủ!", "error");
  }
}

// =============================================================================
// 🔄 HÀM ĐỔ DỮ LIỆU CARD LÊN GIAO DIỆN CHÍNH (ĐÃ CHUẨN HÓA SỬ DỤNG DATA THẬT)
// =============================================================================
function renderBookingGrid() {
  const grid = document.getElementById("booking-grid-list");
  if (!grid) return;

  // Lọc theo tab trạng thái đang chọn
  let filteredData = realBookings.filter((item) => {
    if (activeFilter === "ALL") return true;
    return item.status === activeFilter;
  });

  if (filteredData.length === 0) {
    grid.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: #a0aec0; padding: 40px 0;">Không có ca làm việc nào ở trạng thái này.</p>`;
    return;
  }

  let html = "";
  filteredData.forEach((item) => {
    const firstDetail =
      item.details && item.details.length > 0 ? item.details[0] : null;
    const petDisplay = firstDetail ? firstDetail.petName : "Thú cưng";
    const serviceDisplay = firstDetail
      ? firstDetail.serviceName
      : "Dịch vụ đã đăng ký";

    html += `
      <div class="booking-card ${item.status}" onclick="openTaskDetails(${item.bookingId})">
        <div class="card-top">
          <span class="booking-id">${item.bookingCode}</span>
          <span class="badge ${item.status}">${translateStatus(item.status)}</span>
        </div>
        <div class="card-pet">
          <i class="fa-solid fa-paw" style="color:var(--primary-color)"></i> Bé: ${petDisplay}
        </div>
        <div class="card-service">${serviceDisplay} ${item.details.length > 1 ? `(+${item.details.length - 1})` : ""}</div>
        <div class="card-bottom">
          <span><i class="fa-regular fa-user"></i> Khách: <b>${item.customerName}</b></span>
          <span><i class="fa-regular fa-clock"></i> <b>${item.bookingTime.substring(0, 5)}</b></span>
        </div>
      </div>
    `;
  });
  grid.innerHTML = html;
}

// =============================================================================
// 👁️ MỞ MODAL XEM CHI TIẾT & ĐIỀU KHIỂN NÚT BẤM REAL-TIME
// =============================================================================
function openTaskDetails(id) {
  selectedBookingId = id;
  const task = realBookings.find((item) => item.bookingId === id);
  if (!task) return;

  const firstDetail =
    task.details && task.details.length > 0 ? task.details[0] : null;

  document.getElementById("md-booking-id").innerText = `#${task.bookingCode}`;
  document.getElementById("md-customer-name").innerText = task.customerName;
  document.getElementById("md-customer-phone").innerText =
    "Đã bảo mật qua Token";
  document.getElementById("md-pet-info").innerText = firstDetail
    ? `Bé ${firstDetail.petName} (Phòng: ${firstDetail.roomName})`
    : "Chưa có thông tin";
  document.getElementById("md-service-name").innerText = firstDetail
    ? firstDetail.serviceName
    : "Chưa chỉ định";
  document.getElementById("md-service-price").innerText = formatVND(
    task.totalPrice,
  );
  document.getElementById("md-booking-time").innerText =
    `${task.bookingTime.substring(0, 5)} - ${formatDate(task.bookingDate)}`;
  document.getElementById("md-customer-note").innerText = task.note
    ? `"${task.note}"`
    : "Khách không để lại ghi chú.";

  const mdBadge = document.getElementById("md-status-badge");
  mdBadge.innerText = translateStatus(task.status);
  mdBadge.className = `badge ${task.status}`;

  const btnCancel = document.getElementById("btn-cancel-task");
  const btnStart = document.getElementById("btn-start-task");
  const btnComplete = document.getElementById("btn-complete-task");

  if (task.status === "WAITING") {
    btnCancel.disabled = false;
    btnStart.disabled = false;
    btnComplete.disabled = true;
  } else if (task.status === "PROCESSING") {
    btnCancel.disabled = true;
    btnStart.disabled = true;
    btnComplete.disabled = false;
  } else {
    btnCancel.disabled = true;
    btnStart.disabled = true;
    btnComplete.disabled = true;
  }

  document.getElementById("task-modal").classList.add("open");
}

function closeModal() {
  document.getElementById("task-modal").classList.remove("open");
}

// =============================================================================
// 🚀 TRUNG TÂM XỬ LÝ GỌI CÁC API THAY ĐỔI TRẠNG THÁI (PUT)
// =============================================================================
async function handleUpdateStatus(endpoint, actionName) {
  try {
    // ✅ ĐÃ KIỂM TRA: Nối chuỗi mượt mà ra chuẩn URL /api/bookings/staff/...
    const response = await fetch(`${BASE_URL}/bookings/${endpoint}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${currentToken}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      showLocalToast(
        `🎉 Thực hiện thao tác [${actionName}] thành công!`,
        "success",
      );
      closeModal();
      fetchStaffSchedule(); // Tải lại dữ liệu đồng bộ giao diện
    } else {
      const errorText = await response.text();
      showLocalToast(
        `Thao tác thất bại: ${errorText || response.status}`,
        "error",
      );
    }
  } catch (error) {
    console.error(`Lỗi thực hiện tác vụ ${actionName}:`, error);
    showLocalToast("Mất kết nối đường truyền mạng!", "error");
  }
}

// =============================================================================
// 🛠️ HÀM BỔ TRỢ CHUYỂN ĐỔI NGÔN NGỮ & TIỀN TỆ ĐỒNG BỘ HỆ THỐNG
// =============================================================================
function translateStatus(status) {
  switch (status) {
    case "WAITING":
      return "Đang đợi ⏱️";
    case "PROCESSING":
      return "Đang làm đồ 🛁";
    case "COMPLETED":
      return "Đã xong ✅";
    case "CANCELLED":
      return "Đã hủy ❌";
    case "PENDING_APPROVAL":
      return "Chờ duyệt 👑";
    case "REJECTED":
      return "Từ chối ✖️";
    default:
      return status;
  }
}

function formatDate(dateStr) {
  if (!dateStr) return "";
  const parts = dateStr.split("-");
  if (parts.length !== 3) return dateStr;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

function formatVND(value) {
  if (value === null || value === undefined) return "0đ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}

function showLocalToast(message, type = "success") {
  let container = null;
  if (window.parent && window.parent.document) {
    container = window.parent.document.getElementById("pet-toast-container");
  }
  if (!container) {
    container = document.getElementById("pet-toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "pet-toast-container";
      document.body.appendChild(container);
    }
  }
  const toast = document.createElement("div");
  toast.className = `pet-toast ${type}`;
  const icon = type === "success" ? "🎉" : "❌";
  toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3000);
}
