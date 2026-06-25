/**
 * ⚙️ FILE: DashboardOperation.js
 * Giám sát luồng điều phối trung tâm và Timeline nhật ký hệ thống gộp.
 * Chế độ: Chỉ theo dõi thông tin đơn thời gian thực, lược bỏ hoàn toàn Popup/Modal.
 */

const SERVER_HOST = "http://localhost:8080";
const DASHBOARD_API_URL = `${SERVER_HOST}/api/v1/dashboard/operation-summary`;

let MEMORY_OPERATION_DETAILS = [];
let CURRENT_FILTER = "ALL";

document.addEventListener("DOMContentLoaded", function () {
  const token = getValidToken();

  if (!token) {
    console.error("❌ Không tìm thấy Token hợp lệ (petcare_token)!");
    showToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  // 🚀 Nạp dữ liệu giám sát thời gian thực
  loadOperationDashboard(CURRENT_FILTER, token);

  // 🕒 Đồng hồ hiển thị thời gian nhảy giây
  setInterval(updateLiveClock, 1000);
  updateLiveClock();

  // 📅 Cập nhật hiển thị Ngày/Tháng/Năm hiện tại
  updateLiveDate();
});

// =============================================================================
// 🕒 HÀM BỔ TRỢ XỬ LÝ THỜI GIAN REAL-TIME TỰ ĐỘNG
// =============================================================================
function updateLiveClock() {
  const clockEl = document.getElementById("liveClock");
  if (!clockEl) return;
  const now = new Date();
  clockEl.innerText = now.toTimeString().split(" ")[0];
}

function updateLiveDate() {
  const dateEl = document.getElementById("liveDate");
  if (!dateEl) return;

  const now = new Date();
  const options = {
    weekday: "long",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  };
  const formattedDate = now.toLocaleDateString("vi-VN", options);

  dateEl.innerText = `${formattedDate} | `;
}

// =============================================================================
// 📋 1. TRUY VẤN DỮ LIỆU ĐỘNG TỪ SERVER
// =============================================================================
async function loadOperationDashboard(statusFilter, token) {
  const tbody = document.getElementById("tableOperationBody");
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center" style="color:var(--cstm-text-muted); padding:30px;">⏳ Đang đồng bộ dữ liệu vận hành từ máy chủ...</td></tr>`;
  }

  try {
    const response = await fetch(
      `${DASHBOARD_API_URL}?filter=${statusFilter}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (response.ok) {
      const data = await response.json();
      MEMORY_OPERATION_DETAILS = data.operationFlows || [];

      updateMetricsCounters(data);
      renderOperationTable();
      renderLogsTimeline(data.operationLogs || []);
    } else {
      console.error("❌ Lỗi phản hồi từ máy chủ vận hành:", response.status);
      showToast("Không thể tải dữ liệu vận hành hôm nay!", "error");
    }
  } catch (error) {
    console.error("❌ Mất kết nối mạng đến cụm API Server:", error);
    showToast("Mất kết nối hệ thống server vận hành!", "error");
  }
}

/**
 * 📊 2. HIỂN THỊ ĐẾM SỐ THẺ CHỈ SỐ
 */
function updateMetricsCounters(data) {
  if (!data) return;

  if (document.getElementById("countTotal"))
    document.getElementById("countTotal").innerText =
      data.totalTicketsToday ?? 0;

  if (document.getElementById("countWaiting"))
    document.getElementById("countWaiting").innerText = data.waitingCount ?? 0;

  if (document.getElementById("countProcessing"))
    document.getElementById("countProcessing").innerText =
      data.processingCount ?? 0;

  if (document.getElementById("countHotel")) {
    const rate =
      data.hotelCapacityRate !== undefined
        ? Math.round(data.hotelCapacityRate * 10) / 10
        : 0;
    document.getElementById("countHotel").innerText = `${rate}%`;

    if (document.getElementById("hotelAvailable")) {
      document.getElementById("hotelAvailable").innerText =
        data.availableRooms !== undefined
          ? `${data.availableRooms} phòng trống`
          : "Đang vận hành";
    }
  }
}

/**
 * 🎛️ 3. XỬ LÝ SỰ KIỆN CHUYỂN ĐỔI BỘ LỌC TAB
 */
window.filterOperation = function (status) {
  const token = getValidToken();
  CURRENT_FILTER = status;

  document
    .querySelectorAll(".filter-tag")
    .forEach((btn) => btn.classList.remove("active"));
  if (window.event?.target) window.event.target.classList.add("active");

  loadOperationDashboard(CURRENT_FILTER, token);
};

/**
 * 🗓️ 4. HIỂN THỊ BẢNG THEO DÕI ĐIỀU PHỐI (LUỒNG CA TRONG NGÀY)
 * Lấy thông tin chi tiết của lịch: Mã đơn, Tên thú cưng / Chủ, Dịch vụ, Trạng thái, Nhân viên phụ trách trực tiếp
 */
function renderOperationTable() {
  const tbody = document.getElementById("tableOperationBody");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (MEMORY_OPERATION_DETAILS.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center" style="color:var(--cstm-text-muted); padding:30px; font-weight:600;">📭 Hiện tại không có ca phục vụ nào trong ngày.</td></tr>`;
    return;
  }

  MEMORY_OPERATION_DETAILS.forEach((item) => {
    let statusClass = "status-waiting";
    let statusText = "⏳ Đang Chờ";

    if (item.status === "PROCESSING") {
      statusClass = "status-processing";
      statusText = "⚙️ Đang Làm";
    } else if (item.status === "COMPLETED") {
      statusClass = "status-completed";
      statusText = "✅ Hoàn Thành";
    }

    let serviceIcon = "🩺";
    const textLower = item.serviceName ? item.serviceName.toLowerCase() : "";
    if (
      textLower.includes("spa") ||
      textLower.includes("tắm") ||
      textLower.includes("tỉa")
    ) {
      serviceIcon = "✂️";
    } else if (
      textLower.includes("hotel") ||
      textLower.includes("lưu trú") ||
      textLower.includes("phòng")
    ) {
      serviceIcon = "🏨";
    }

    // Hiển thị tên nhân viên thực hiện (lấy từ system gán unit)
    const staffNameLabel =
      item.staffInCharge && item.staffInCharge !== "Chưa phân công"
        ? `<i class="ri-user-star-line"></i> ${item.staffInCharge}`
        : "Chưa Chỉ Định";

    tbody.innerHTML += `
      <tr>
        <td><span class="badge-code" style="background:#eee4dc; color:var(--cstm-text-dark);">${item.bookingCode || "N/A"}</span></td>
        <td>
          <div style="font-weight:700; font-size: 13px;">🐾 ${item.petAndOwnerName || "Thú cưng / Khách hàng"}</div>
        </td>
        <td style="font-size:12px; color:#4a3b32; font-weight: 500;">${serviceIcon} ${item.serviceName || "Dịch vụ"}</td>
        <td><span class="pill-op-status ${statusClass}">${statusText}</span></td>
        <td><span style="font-size:12px; font-weight:700;">${staffNameLabel}</span></td>
      </tr>`;
  });
}

/**
 * 📈 5. HIỂN THỊ TIMELINE NHẬT KÝ BIẾN ĐỘNG VẬN HÀNH
 */
function renderLogsTimeline(timelineLogs) {
  const container = document.getElementById("timelineLogs");
  if (!container) return;
  container.innerHTML = "";

  if (!timelineLogs || timelineLogs.length === 0) {
    container.innerHTML = `<div style="text-align: center; color: #999; padding: 30px; font-size:12px;">Chưa có biến động vận hành nào hôm nay.</div>`;
    return;
  }

  timelineLogs.forEach((log) => {
    let logType = "blue";
    const originalText = log.logText || "";
    const textLower = originalText.toLowerCase();

    if (textLower.includes("muộn") || textLower.includes("sớm")) {
      logType = "orange";
    } else if (textLower.includes("vắng mặt") || textLower.includes("nghỉ")) {
      logType = "red";
    } else if (
      textLower.includes("đúng giờ") ||
      textLower.includes("thành công")
    ) {
      logType = "green";
    }

    let cleanTime = "--:--";
    if (log.logTime) {
      if (typeof log.logTime === "string" && log.logTime.includes(":")) {
        cleanTime = log.logTime.substring(0, 5);
      } else if (Array.isArray(log.logTime) && log.logTime.length >= 2) {
        const hh = String(log.logTime[0]).padStart(2, "0");
        const mm = String(log.logTime[1]).padStart(2, "0");
        cleanTime = `${hh}:${mm}`;
      }
    }

    const timeHTML =
      cleanTime !== "--:--"
        ? `<span class="timeline-time">${cleanTime}</span>`
        : "";

    container.innerHTML += `
      <div class="timeline-item">
        <div class="timeline-badge ${logType}"></div>
        <div class="timeline-content">
          ${timeHTML}
          <p class="timeline-text" style="font-weight: 500; color: var(--cstm-text-dark); line-height: 1.4; margin: 0;">${originalText}</p>
        </div>
      </div>`;
  });
}

// =============================================================================
// 🛠️ HỖ TRỢ TIỆN ÍCH (UTILS)
// =============================================================================
function getValidToken() {
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");
  if (!token) token = localStorage.getItem("petcare_token");
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }
  return token && token !== "null" && token !== "undefined"
    ? token.trim()
    : null;
}

function showToast(msg, type = "success") {
  let container = document.getElementById("pet-toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "pet-toast";

  let iconHTML = `<i class="ri-checkbox-circle-fill" style="color:#10b981"></i>`;
  if (type === "error") {
    iconHTML = `<i class="ri-error-warning-fill" style="color:#dc3545"></i>`;
  }

  toast.innerHTML = `${iconHTML} <span>${msg}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = "slideIn 0.3s reverse";
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}
