/**
 * ⏱️ FILE: Attendance.js
 * Xử lý hệ thống chấm công Real-time đa ca (Ca Ngày / Ca Tối) của Nhân viên
 * ĐÃ KHẮC PHỤC: Loại bỏ hoàn toàn hardcode tên ca cũ, tối ưu UX tự động khóa/ẩn nút khi quá giờ.
 */

// 🌐 Cấu hình địa chỉ Server tập trung - Đồng bộ với hệ thống
const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

document.addEventListener("DOMContentLoaded", function () {
  // 🔑 Nhặt tham số thông minh từ URL bao gồm cả token và viewStaffId
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");
  const viewStaffId = urlParams.get("viewStaffId"); // 🔍 NHẶT MÃ NHÂN VIÊN ĐƯỢC ADMIN TRUYỀN SANG

  if (!token) {
    token = localStorage.getItem("petcare_token");
  }
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  // 🧹 KIỂM TRA & CHẶN TOKEN TỪ VÒNG GỬI XE
  if (
    token === "null" ||
    token === "undefined" ||
    !token ||
    token.trim() === ""
  ) {
    console.error("❌ Front-End chặn đứng: Không tìm thấy Token hợp lệ!");
    showLocalToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    setTimeout(() => {
      if (window.parent) window.parent.location.href = "index.html";
      else window.location.href = "index.html";
    }, 2000);
    return;
  }

  console.log("🔒 Token chấm công thông quan thành công:", token);

  // ⏱️ KÍCH HOẠT ĐỒNG HỒ REAL-TIME
  startDigitalClock();

  // 👑 KIỂM TRA QUYỀN TRUY CẬP: ADMIN XEM HỘ HAY NHÂN VIÊN TỰ XEM
  if (viewStaffId) {
    console.log(
      `👑 Chế độ Admin: Đang xem lịch sử chấm công của Staff ID [${viewStaffId}]`,
    );

    // 1. Khóa vùng bấm nút Chấm công vì Admin không thể Check-in hộ
    const attendanceActionBox = document.querySelector(".action-box");
    if (attendanceActionBox) {
      attendanceActionBox.style.opacity = "0.5";
      attendanceActionBox.style.pointerEvents = "none";
    }
    const statusBanner = document.getElementById("status-banner");
    if (statusBanner) {
      statusBanner.innerHTML = `ℹ️ Đang xem dữ liệu chấm công với tư cách Quản trị viên`;
    }

    // 2. Render bảng lịch sử theo ID của nhân viên được chọn
    renderAttendanceTable(token, viewStaffId);
  } else {
    // 👤 Chế độ Nhân viên: Tự chấm công và tự xem lịch sử bản thân
    initAttendanceStatus(token);
    renderAttendanceTable(token, null);

    // 🟢 LẮNG NGHE SỰ KIỆN NÚT CHECK-IN
    const btnCheckIn = document.getElementById("btn-checkin");
    if (btnCheckIn) {
      btnCheckIn.addEventListener("click", function () {
        submitCheckIn(token);
      });
    }

    // 🔴 LẮNG NGHE SỰ KIỆN NÚT CHECK-OUT
    const btnCheckOut = document.getElementById("btn-checkout");
    if (btnCheckOut) {
      btnCheckOut.addEventListener("click", function () {
        submitCheckOut(token);
      });
    }
  }
});

/**
 * ⏱️ HỆ THỐNG ĐỒNG HỒ REAL-TIME CHẠY GIÂY
 */
function startDigitalClock() {
  const clockEl = document.getElementById("digital-clock");
  const dateEl = document.getElementById("current-date");
  if (!clockEl) return;

  setInterval(() => {
    const now = new Date();
    let h = String(now.getHours()).padStart(2, "0");
    let m = String(now.getMinutes()).padStart(2, "0");
    let s = String(now.getSeconds()).padStart(2, "0");
    clockEl.innerText = `${h}:${m}:${s}`;

    const options = {
      weekday: "long",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    };
    dateEl.innerText = now.toLocaleDateString("vi-VN", options);
  }, 1000);
}

/**
 * 🕒 ĐỒNG BỘ TRẠNG THÁI NÚT CHẤM CÔNG VÀ BANNER TỪ BACK-END HÔM NAY
 * 🔥 ĐÃ CẬP NHẬT UX: Tự động khóa cứng và làm mờ nút nếu quá giờ làm việc (isExpired)
 */
async function initAttendanceStatus(token) {
  const btnCheckIn = document.getElementById("btn-checkin");
  const btnCheckOut = document.getElementById("btn-checkout");
  const statusBanner = document.getElementById("status-banner");

  if (!btnCheckIn || !btnCheckOut || !statusBanner) return;

  try {
    const res = await fetch(`${BASE_URL}/attendance/today`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (res.ok) {
      const data = await res.json();

      // 🛑 TRƯỜNG HỢP 1: Hệ thống báo quá giờ ca làm (isExpired == true) -> Khóa sạch nút khỏi bấm lung tung
      if (data.isExpired) {
        btnCheckIn.disabled = true;
        btnCheckOut.disabled = true;
        // Thêm class css mờ hẳn nút đi cho đẹp mắt
        btnCheckIn.style.opacity = "0.4";
        btnCheckOut.style.opacity = "0.4";
      }
      // ⛔ TRƯỜNG HỢP 2: Ngoài khung giờ làm việc, không tìm thấy ca nào
      else if (!data.shiftName || data.shiftName === "") {
        btnCheckIn.disabled = true;
        btnCheckOut.disabled = true;
        btnCheckIn.style.opacity = "0.4";
        btnCheckOut.style.opacity = "0.4";
      }
      // 🟢 TRƯỜNG HỢP 3: Trong khung giờ ca làm hợp lệ -> Bật tắt nút theo tiến độ thực tế
      else {
        btnCheckIn.style.opacity = "1";
        btnCheckOut.style.opacity = "1";

        btnCheckIn.disabled = data.hasCheckIn; // Đã check-in thì khóa nút check-in
        btnCheckOut.disabled = !data.hasCheckIn || data.hasCheckOut; // Chưa check-in HOẶC đã check-out thì khóa nút check-out
      }

      // Đổ dòng thông báo HTML động từ Backend ra Banner
      statusBanner.innerHTML = data.statusBannerMessage;
    }
  } catch (err) {
    console.error("🚨 Lỗi lấy trạng thái chấm công hôm nay:", err);
  }
}

/**
 * 📜 RENDER BẢNG LỊCH SỬ CHẤM CÔNG TRONG THÁNG TỪ DATABASE
 * 🔥 ĐÃ SỬA: Loại bỏ gán cứng CA_SANG/CA_CHIEU, lấy trực tiếp tên ca Tiếng Việt từ DB
 */
async function renderAttendanceTable(token, staffId = null) {
  const tbody = document.getElementById("attendance-history-rows");
  if (!tbody) return;

  try {
    const targetUrl = staffId
      ? `${BASE_URL}/attendance/admin/history/${parseInt(staffId, 10)}`
      : `${BASE_URL}/attendance/history`;

    const res = await fetch(targetUrl, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) throw new Error("Không lấy được lịch sử");

    const historyData = await res.json();
    let html = "";

    if (historyData.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">Chưa có dữ liệu chấm công trong tháng</td></tr>`;
      return;
    }

    historyData.forEach((item) => {
      let statusClass = "on-time";
      let label = "Đúng giờ ✅";

      if (item.status === "LATE") {
        statusClass = "late";
        label = "Đi muộn ⏱️";
      } else if (item.status === "EARLY") {
        statusClass = "early";
        label = "Về sớm ⚠️";
      } else if (item.status === "ABSENT") {
        statusClass = "absent";
        label = "Vắng mặt ❌";
      }

      const [year, month, day] = item.attendanceDate.split("-");
      const formattedDate = `${day}/${month}/${year}`;

      const checkInTime = item.checkInTime
        ? item.checkInTime.substring(0, 5)
        : "--:--";
      const checkOutTime = item.checkOutTime
        ? item.checkOutTime.substring(0, 5)
        : "--:--";
      const hoursDisplay = item.totalHours
        ? parseFloat(item.totalHours).toFixed(1)
        : "0";

      // 🔄 ĐÃ THÁO XÍCH CỨNG: Lấy chuẩn chuỗi Tiếng Việt (Ca Ngày / Ca Tối) từ API đổ ra luôn
      let shiftDisplay = "Ca Làm";
      if (item.shiftName) {
        shiftDisplay = item.shiftName;
      } else if (item.workShift && item.workShift.shiftName) {
        shiftDisplay = item.workShift.shiftName;
      }

      html += `
        <tr>
            <td><b>${formattedDate}</b> <br><small style="color: var(--text-muted); font-weight: 500;">(${shiftDisplay})</small></td>
            <td><i class="fa-regular fa-clock" style="color:var(--success-color)"></i> ${checkInTime}</td>
            <td><i class="fa-regular fa-clock" style="color:var(--danger-color)"></i> ${checkOutTime}</td>
            <td><b>${hoursDisplay}h</b></td>
            <td><span class="badge-status ${statusClass}">${label}</span></td>
        </tr>
      `;
    });
    tbody.innerHTML = html;
  } catch (err) {
    console.error("🚨 Lỗi render lịch sử chấm công:", err);
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--danger-color)">🚨 Lỗi không thể tải dữ liệu bảng!</td></tr>`;
  }
}

/**
 * 🟢 GỬI REQUEST CHECK-IN LÊN SERVER
 */
async function submitCheckIn(token) {
  const btnCheckIn = document.getElementById("btn-checkin");
  btnCheckIn.disabled = true;

  try {
    const res = await fetch(`${BASE_URL}/attendance/check-in`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) {
      const errorText = await res.text();
      showLocalToast(errorText || "Check-in thất bại!", "error");
      btnCheckIn.disabled = false;
      return;
    }

    showLocalToast("🎉 Ghi nhận vào ca thành công!", "success");
    await initAttendanceStatus(token);
    await renderAttendanceTable(token, null);
  } catch (err) {
    showLocalToast("Lỗi kết nối Server!", "error");
    btnCheckIn.disabled = false;
  }
}

/**
 * 🔴 GỬI REQUEST CHECK-OUT LÊN SERVER
 */
async function submitCheckOut(token) {
  const btnCheckOut = document.getElementById("btn-checkout");
  btnCheckOut.disabled = true;

  try {
    const res = await fetch(`${BASE_URL}/attendance/check-out`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!res.ok) {
      const errorText = await res.text();
      showLocalToast(errorText || "Check-out thất bại!", "error");
      btnCheckOut.disabled = false;
      return;
    }

    showLocalToast("🎉 Ghi nhận kết thúc ca thành công!", "success");
    await initAttendanceStatus(token);
    await renderAttendanceTable(token, null);
  } catch (err) {
    showLocalToast("Lỗi kết nối Server!", "error");
    btnCheckOut.disabled = false;
  }
}

/**
 * 🛠️ TOAST THÔNG BÁO LIÊN THÔNG ĐỒNG BỘ VỚI PROFILE
 */
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
