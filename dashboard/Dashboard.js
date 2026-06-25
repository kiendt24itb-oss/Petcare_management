/**
 * 📊 FILE: Dashboard.js
 * Xử lý phân quyền Menu động, hiển thị Profile, chuông thông báo, và Bảo mật trang quản trị (Bản đồng bộ Token Iframe)
 */

const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// 📸 Chuỗi ảnh đại diện SVG gradient hồng mặc định siêu đẹp của ní khi chưa có avatar
const DEFAULT_SVG_AVATAR =
  "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0b3AtY29sb3I9IiNmZjlhOWUiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZWNmZWYiLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0idXJsKCNnKSIvPldjdXN0b21lcjxjaXJjbGUgY3g9IjUwIiBjeT0iNDIiIHI9IjE4IiBmaWxsPSIjZmZmIi8+PHBhdGggZD0iTTI1LDc1IEMyNSw1OCA3NSw1OCA3NSw3NSIgZmlsbD0iI2ZmZiIvPjwvc3ZnPg==";

document.addEventListener("DOMContentLoaded", function () {
  // 1. CHẶN VÀO LÉN: Kiểm tra Token trong localStorage
  const token = localStorage.getItem("petcare_token");
  const username = localStorage.getItem("petcare_username") || "Người dùng";
  const role = localStorage.getItem("petcare_role");
  const currentUserKey = "petcare_current_user";
  const savedUser = localStorage.getItem(currentUserKey);

  if (savedUser !== username) {
    localStorage.removeItem("petcare_avatar");
    localStorage.setItem(currentUserKey, username);
  }

  if (!token) {
    alert(
      "🔒 Phiên làm việc đã hết hạn hoặc bạn chưa đăng nhập! Vui lòng quay lại.",
    );
    window.location.href = "/index.html";
    return;
  }

  // 2. HIỂN THỊ THÔNG TIN USER LÊN MINI PROFILE
  document.getElementById("userProfileName").innerText = username;
  loadUserAvatar(token);

  const badge = document.getElementById("userRoleBadge");
  if (badge) {
    if (role === "ADMIN") {
      badge.innerText = "👑 Quản trị viên";
      badge.style.background = "#fff2e8";
      badge.style.color = "#fa541c";
    } else if (role === "STAFF") {
      badge.innerText = "🧑‍⚕️ Nhân viên";
      badge.style.background = "#e6f7ff";
      badge.style.color = "#1890ff";
    } else {
      badge.innerText = "🐾 Khách hàng";
      badge.style.background = "#f6ffed";
      badge.style.color = "#52c41a";
    }
  }

  // 3. DANH SÁCH MENU THEO TÍNH NĂNG
  const menuData = {
    CUSTOMER: [
      {
        text: "👤 Thông tin cá nhân",
        url: "/user/customer/Customer_info.html",
      },
      { text: "🐱 Thông tin thú cưng", url: "/user/pet/Pet_list.html" },
      { text: "📅 Đặt lịch dịch vụ", url: "/user/booking/Booking_list.html" },
    ],
    STAFF: [
      {
        text: "👤 Thông tin cá nhân",
        url: "/staff/staff_profile/Staff_profile.html",
      },
      {
        text: "⏰ Chấm công hàng ngày",
        url: "/staff/attendance/Attendance.html",
      },
      {
        text: "📅 Xem lịch làm việc",
        url: "/staff/staff_schedule/Staff_schedule.html",
      },
    ],
    ADMIN: [
      { text: "📊 Thống kê tổng quan", url: "/admin/overview/Overview.html" },
      {
        text: "👥 Quản lý nhân viên",
        url: "/admin/staffManagement/StaffManagement.html",
        showSearch: true, // Đánh dấu tab này được phép hiện ô Tìm kiếm cho Admin
      },
      {
        text: "👨‍💼 Quản lý khách hàng",
        url: "/admin/customerManagement/CustomerManagement.html",
        showSearch: true, // Đánh dấu tab này được phép hiện ô Tìm kiếm cho Admin
      },
      {
        text: "📦 Các gói dịch vụ",
        url: "/admin/serviceManagement/ServiceManagement.html",
      },
      {
        text: "🏨 Quản lý vận hành",
        url: "/admin/dashboardOperation/DashboardOperation.html",
      },
      {
        text: "🏢 Phòng & Vị trí làm việc",
        url: "/admin/roomManagement/RoomManagement.html",
      },
    ],
  };

  // 🛠️ ĐIỀU KHIỂN ĐỔI CHỖ LÀM SẠCH GIAO DIỆN TỪ VÒNG GỬI XE (CƠ BẢN THEO ROLE)
  const searchContainer = document.getElementById("topbarSearchContainer");
  const portalWrapper = document.getElementById("cosmicPortalWrapper");
  const customerNotiBell = document.getElementById("customerNotiBell");

  if (role === "CUSTOMER") {
    if (searchContainer) searchContainer.style.display = "none"; // Khách hàng: Ẩn tìm kiếm tất cả trang
    if (portalWrapper) portalWrapper.style.display = "none"; // Khách hàng: Ẩn vòng xoay
    if (customerNotiBell) {
      customerNotiBell.style.display = "inline-block"; // Khách hàng: Thay thế bằng Chuông tb
      initCustomerNotifications(token); // Kéo dữ liệu đổ chuông
    }
  } else if (role === "STAFF") {
    if (searchContainer) searchContainer.style.display = "none"; // Nhân viên: Ẩn tìm kiếm tất cả chức năng
    if (customerNotiBell) customerNotiBell.style.display = "none"; // Nhân viên: Ẩn chuông
    if (portalWrapper) portalWrapper.style.display = "inline-block"; // Nhân viên: Giữ vòng xoay
  } else if (role === "ADMIN") {
    if (customerNotiBell) customerNotiBell.style.display = "none"; // Admin: Luôn ẩn chuông thông báo
    if (portalWrapper) portalWrapper.style.display = "inline-block"; // Admin: Luôn giữ vòng xoay
    // Ô search của admin mặc định ẩn ở trang Thống kê đầu tiên, sẽ tự động xử lý khi click tab bên dưới
    if (searchContainer) searchContainer.style.display = "none";
  }

  // 4. ĐỔ MENU ĐỘNG RA SIDEBAR DỰA VÀO ROLE CỦA USER
  const menuContainer = document.getElementById("dynamicMenu");
  const iframeContainer = document.getElementById("mainFrame");
  const userMenu = menuData[role] || menuData["CUSTOMER"];

  if (menuContainer) {
    menuContainer.innerHTML = "";

    userMenu.forEach((item, index) => {
      const li = document.createElement("li");
      const a = document.createElement("a");

      const urlWithToken = `${item.url}?token=${encodeURIComponent(token)}`;

      a.href = urlWithToken;
      a.target = "mainFrame";
      a.innerHTML = item.text;

      // Nạp trang đầu tiên mặc định
      if (index === 0 && iframeContainer) {
        a.classList.add("active");
        iframeContainer.src = urlWithToken;

        // Nếu trang đầu tiên của Admin yêu cầu hiện tìm kiếm thì bật (thông thường thống kê không cần)
        if (role === "ADMIN" && searchContainer) {
          searchContainer.style.display = item.showSearch ? "flex" : "none";
        }
      }

      a.addEventListener("click", function () {
        document
          .querySelectorAll("#dynamicMenu a")
          .forEach((nav) => nav.classList.remove("active"));
        this.classList.add("active");

        // 🔄 XÓA CHỮ TRONG Ô SEARCH KHI CHUYỂN TAB ĐỂ TRÁNH BỊ LỖI LỌC SAI TRANG MỚI
        const topbarSearch = document.getElementById("topbarSearch");
        if (topbarSearch) topbarSearch.value = "";

        // 🔍 ĐIỀU PHÂN QUYỀN REAL-TIME THANH TÌM KIẾM CHO ADMIN
        if (role === "ADMIN" && searchContainer) {
          if (item.showSearch) {
            searchContainer.style.display = "flex"; // Chỉ hiện ở Quản lý nhân viên & Quản lý khách hàng
          } else {
            searchContainer.style.display = "none"; // Các trang Admin khác ẩn đi
          }
        }
      });

      li.appendChild(a);
      menuContainer.appendChild(li);
    });
  }

  // 🌟 5. LẮNG NGHE THANH TÌM KIẾM CHUNG - BẮN TỪ KHÓA XUYÊN IFRAME REAL-TIME
  const topbarSearch = document.getElementById("topbarSearch");
  if (topbarSearch) {
    topbarSearch.addEventListener("input", function () {
      const keyword = this.value.trim(); // Giữ nguyên chữ hoa/thường để backend xử lý linh hoạt
      const iframe = document.getElementById("mainFrame");

      if (iframe && iframe.contentWindow) {
        // Bắn gói tin message sang iframe
        iframe.contentWindow.postMessage(
          { action: "SEARCH_CUSTOMER", keyword: keyword },
          "*",
        );
      }
    });
  }

  // ⚡ 6. KÍCH HOẠT ĐÓNG/MỞ DROPDOWN CHUÔNG THÔNG BÁO TẠI GIAO DIỆN KHÁCH HÀNG
  const notiBellBtn = document.getElementById("notiBellBtn");
  const notiDropdown = document.getElementById("notiDropdown");

  if (notiBellBtn && notiDropdown) {
    notiBellBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      notiDropdown.classList.toggle("open");
    });

    // Click chuột ra ngoài khoảng không tự động thu gọn chuông
    document.addEventListener("click", function () {
      notiDropdown.classList.remove("open");
    });
  }

  // 7. XỬ LÝ SỰ KIỆN NÚT ĐĂNG XUẤT 🚪
  const btnLogout = document.getElementById("btnLogout");
  if (btnLogout) {
    btnLogout.addEventListener("click", function (event) {
      event.preventDefault();
      localStorage.removeItem("petcare_token");
      localStorage.removeItem("petcare_username");
      localStorage.removeItem("petcare_role");
      localStorage.removeItem("petcare_avatar");
      localStorage.removeItem("petcare_current_user");
      window.location.href = "/index.html";
    });
  }
});

// =============================================================================
// 📥 HÀM THÔNG BÁO CHUÔNG - DỰA VÀO NHẬT KÝ HÀNH TRÌNH LOGS THỰC TẾ DƯỚI DB
// =============================================================================
async function initCustomerNotifications(token) {
  const notiCount = document.getElementById("notiCount");
  const notiBodyList = document.getElementById("notiBodyList");
  if (!notiBodyList) return;

  try {
    // 1. Lấy danh sách lịch hẹn của Customer
    const response = await fetch(`${BASE_URL}/bookings/my-bookings`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) throw new Error("Không thể lấy danh sách lịch hẹn");
    const bookings = await response.json();

    if (!bookings || bookings.length === 0) {
      notiBodyList.innerHTML = `<div class="noti-empty-state">Chưa có thông báo lịch hẹn nào.</div>`;
      if (notiCount) notiCount.style.display = "none";
      return;
    }

    let allLogsArray = [];

    // 2. Chạy vòng lặp lấy toàn bộ Logs chi tiết của TỪNG lịch hẹn (Giống hàm renderBookingTimelineUI)
    for (const bk of bookings) {
      try {
        const resLogs = await fetch(
          `${BASE_URL}/bookings/${bk.bookingId}/logs`,
          {
            headers: { Authorization: `Bearer ${token}` },
          },
        );

        if (resLogs.ok) {
          const logs = await resLogs.json();

          // Khớp dữ liệu log đi kèm với thông tin code và trạng thái của đơn
          logs.forEach((log) => {
            allLogsArray.push({
              bookingCode: bk.bookingCode,
              status: bk.status,
              logTime: log.logTime ? log.logTime.substring(0, 5) : "--:--",
              logText: log.logText,
              // Tạo mốc định danh duy nhất để sắp xếp (nếu có id log thì dùng logId, không thì trộn chuỗi)
              sortKey: `${bk.bookingDate}T${log.logTime}`,
            });
          });
        }
      } catch (err) {
        console.error(`Lỗi nạp log của mã đơn ${bk.bookingCode}:`, err);
      }
    }

    // 3. Sắp xếp thông báo mới nhất lên trên đầu (Dựa theo mốc thời gian diễn ra log)
    allLogsArray.sort((a, b) => b.sortKey.localeCompare(a.sortKey));

    if (allLogsArray.length === 0) {
      notiBodyList.innerHTML = `<div class="noti-empty-state">Chưa có cập nhật nhật ký dịch vụ nào.</div>`;
      if (notiCount) notiCount.style.display = "none";
      return;
    }

    // 4. Hiển thị số lượng thông báo log lên badge chuông
    if (notiCount) {
      notiCount.innerText =
        allLogsArray.length > 9 ? "9+" : allLogsArray.length;
      notiCount.style.display = "flex";
    }

    // 5. Render dữ liệu ra giao diện chuông thông báo
    let html = "";
    allLogsArray.forEach((item) => {
      // Chọn icon thông minh đại diện dựa vào từ khóa hoặc trạng thái của đơn
      let statusIcon = "📅";
      const text = item.logText.toLowerCase();

      if (text.includes("duyệt") || text.includes("phê duyệt"))
        statusIcon = "✅";
      else if (text.includes("hủy")) statusIcon = "❌";
      else if (text.includes("vi phạm") || text.includes("xem xét"))
        statusIcon = "⚠️";
      else if (text.includes("từ chối")) statusIcon = "⛔";
      else if (text.includes("tiến hành") || text.includes("chăm sóc"))
        statusIcon = "✂️";
      else if (text.includes("hoàn thành")) statusIcon = "🎉";

      html += `
        <div class="noti-item" style="border-bottom: 1px solid #f1f5f9; padding: 12px; cursor: pointer;">
          <div style="display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; margin-bottom: 4px;">
            <span>${statusIcon} Mã lịch: <b>${item.bookingCode}</b></span>
            <span>⏱️ ${item.logTime}</span>
          </div>
          <p class="noti-text" style="margin: 0; font-size: 13px; color: #334155; line-height: 1.4;">${item.logText}</p>
        </div>
      `;
    });

    notiBodyList.innerHTML = html;
  } catch (error) {
    console.error("Lỗi nạp thông báo chuông từ hệ thống logs:", error);
    notiBodyList.innerHTML = `<div class="noti-empty-state" style="color:#ff7a45;">Mất kết nối dữ liệu thông báo.</div>`;
  }
}

/**
 * 📥 Hàm bốc dữ liệu Avatar từ Server dựa vào Role
 */
async function loadUserAvatar(token) {
  try {
    const role = localStorage.getItem("petcare_role");
    let targetUrl =
      role === "STAFF" || role === "ADMIN"
        ? `${BASE_URL}/staffs/me`
        : `${BASE_URL}/customers/profile`;

    const response = await fetch(targetUrl, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) return;

    const userData = await response.json();
    const avatarEl = document.getElementById("userAvatar");
    if (!avatarEl) return;

    const avatarPath = userData.avatar || userData.imagePath;

    if (avatarPath) {
      avatarEl.src = getImageUrl(avatarPath);
    } else {
      avatarEl.src = DEFAULT_SVG_AVATAR;
    }
  } catch (error) {
    console.error("Lỗi tải avatar:", error);
  }
}

/**
 * 🛠️ Hàm chuẩn hóa đường dẫn ảnh thông minh
 */
function getImageUrl(path) {
  if (!path || path.trim() === "") return DEFAULT_SVG_AVATAR;

  if (
    path.startsWith("http://") ||
    path.startsWith("https://") ||
    path.startsWith("data:image/")
  ) {
    return path;
  }

  return SERVER_HOST + path;
}
