/**
 * 📊 FILE: Overview.js
 * Xử lý số liệu Dashboard - Kết nối API Thật danh sách chờ duyệt, Doanh thu, Biểu đồ, Lịch hẹn & Trạng thái phòng
 * ĐÃ CẬP NHẬT: Xóa hoàn toàn dữ liệu ảo của Phòng, liên thông dữ liệu thời gian thực từ Database.
 */

const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// 🔄 Dữ liệu ảo dự phòng (Chỉ dùng khi mất kết nối API Thống kê)
const MOCK_KPI = {
  totalRevenue: 0,
  todayBookingsCount: 0,
};

const MOCK_CHART_DATA = {
  labels: ["Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6"],
  datasets: [0, 0, 0, 0, 0, 0],
};

let myChartInstance = null;

const getAuthHeaders = (token) => {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
};

document.addEventListener("DOMContentLoaded", function () {
  let token =
    localStorage.getItem("petcare_token") ||
    window.parent?.localStorage.getItem("petcare_token");

  // Khởi chạy đồng bộ toàn bộ hệ thống từ dữ liệu thực tế
  loadKPIMetricsAndCharts(token);
  loadRoomStatus(token);
  loadPendingBookings(token);

  document
    .getElementById("btnViewAllBookings")
    ?.addEventListener("click", function () {
      if (window.parent) window.parent.location.hash = "#/bookings";
    });
});

function initRevenueChart(labels, datasets) {
  const ctx = document.getElementById("revenueChart");
  if (!ctx) return;
  if (myChartInstance) myChartInstance.destroy();

  const context = ctx.getContext("2d");
  const gradient = context.createLinearGradient(0, 0, 0, 250);
  gradient.addColorStop(0, "rgba(255, 107, 0, 0.35)");
  gradient.addColorStop(1, "rgba(255, 107, 0, 0.00)");

  myChartInstance = new Chart(ctx, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Doanh thu (VNĐ)",
          data: datasets,
          borderColor: "#ff6b00",
          backgroundColor: gradient,
          borderWidth: 4,
          pointBackgroundColor: "#ff6b00",
          pointHoverBackgroundColor: "#fff",
          pointHoverBorderWidth: 3,
          pointRadius: 4,
          tension: 0.4,
          fill: true,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { grid: { color: "#f3ece6" } },
        x: { grid: { display: false } },
      },
    },
  });
}

// 👑 GOM NHÓM & LIÊN THÔNG API DOANH THU + SỐ ĐƠN HÔM NAY + NHÂN SỰ
async function loadKPIMetricsAndCharts(token) {
  let finalRevenue = 0;
  let todayBookings = 0;
  let chartLabels = MOCK_CHART_DATA.labels;
  let chartData = MOCK_CHART_DATA.datasets;

  // 1. Gọi API lấy Thống kê doanh thu và số lượng đơn hôm nay từ Backend
  try {
    const revResponse = await fetch(
      `${BASE_URL}/bookings/admin/dashboard-stats`,
      {
        method: "GET",
        headers: getAuthHeaders(token),
      },
    );
    if (revResponse.ok) {
      const stats = await revResponse.json();
      finalRevenue = stats.currentMonthRevenue;
      todayBookings = stats.todayBookingsCount || 0;

      // Chuyển mảng 6 tháng gần nhất sang cấu trúc biểu đồ Chart.js
      if (stats.sixMonthsRevenue && stats.sixMonthsRevenue.length > 0) {
        chartLabels = stats.sixMonthsRevenue.map((item) => item.label);
        chartData = stats.sixMonthsRevenue.map((item) => item.revenue);
      }
    }
  } catch (error) {
    console.error("🚨 Lỗi nạp dữ liệu thống kê thật:", error);
    finalRevenue = MOCK_KPI.totalRevenue;
    todayBookings = MOCK_KPI.todayBookingsCount;
  }

  // Vẽ hoặc làm mới biểu đồ doanh thu
  initRevenueChart(chartLabels, chartData);

  // 2. Gọi API lấy số lượng điểm danh của Nhân viên
  let presentStaff = 0;
  let totalStaff = 0;
  try {
    const staffResponse = await fetch(
      `${BASE_URL}/attendance/admin/widgets-summary`,
      {
        method: "GET",
        headers: getAuthHeaders(token),
      },
    );
    if (staffResponse.ok) {
      const staffData = await staffResponse.json();
      presentStaff = staffData.presentToday || 0;
      totalStaff = staffData.totalStaff || 0;
    }
  } catch (error) {
    console.error("🚨 Không thể kết nối API điểm danh nhân viên.");
  }

  // 3. Render thông tin trực quan lên các Widget KPI Card
  document.getElementById("kpiRevenue").innerText = new Intl.NumberFormat(
    "vi-VN",
    { style: "currency", currency: "VND" },
  ).format(finalRevenue);

  document.getElementById("kpiActiveStaff").innerText =
    `${presentStaff}/${totalStaff}`;

  document.getElementById("kpiTodayBookings").innerText = todayBookings;
}

// 📋 API: TẢI DANH SÁCH PHÒNG THẬT TỪ DATABASE (ĐỒNG BỘ THEO ROOMMANAGEMENT)
async function loadRoomStatus(token) {
  try {
    const response = await fetch(`${SERVER_HOST}/api/rooms/all`, {
      method: "GET",
      headers: getAuthHeaders(token),
    });

    if (response.ok) {
      const actualRooms = await response.json();

      // Sắp xếp thứ tự phòng theo Mã Phòng tăng dần (chuẩn logic giao diện)
      actualRooms.sort((a, b) => {
        if (!a.roomCode) return 1;
        if (!b.roomCode) return -1;
        return a.roomCode.localeCompare(b.roomCode, undefined, {
          numeric: true,
          sensitivity: "base",
        });
      });

      renderRooms(actualRooms);
    } else {
      throw new Error("Không phản hồi dữ liệu phòng");
    }
  } catch (error) {
    console.error("🚨 Lỗi nạp dữ liệu không gian thực tế:", error);
    const roomGrid = document.getElementById("roomLiveStatus");
    if (roomGrid) {
      roomGrid.innerHTML = `
        <div style="grid-column: 1/-1; text-align:center; color: #dc3545; padding:20px; font-weight:600;">
            ⚠️ Không thể đồng bộ trạng thái phòng từ Database!
        </div>`;
    }
  }
}

// 🎨 HIỂN THỊ CÁC Ô TRẠNG THÁI PHÒNG REAL-TIME LÊN MÀN HÌNH CHỦ
function renderRooms(rooms) {
  const roomGrid = document.getElementById("roomLiveStatus");
  if (!roomGrid) return;
  roomGrid.innerHTML = "";

  if (!rooms || rooms.length === 0) {
    roomGrid.innerHTML = `
      <div style="grid-column: 1/-1; text-align:center; color: #999; padding:20px; font-weight:600;">
          📭 Hệ thống chưa có cấu hình phòng vận hành nào.
      </div>`;
    return;
  }

  rooms.forEach((room) => {
    const roomCard = document.createElement("div");

    let statusClass = "status-available";
    let statusText = '<i class="ri-checkbox-circle-fill"></i> Trống';

    if (room.status === "BUSY") {
      statusClass = "status-busy";
      statusText = '<i class="ri-heart-pulse-fill"></i> Có Pet';
    } else if (room.status === "BOOKED") {
      statusClass = "status-booked";
      statusText = '<i class="ri-calendar-check-fill"></i> Đã Đặt';
    } else if (room.status === "MAINTENANCE") {
      statusClass = "status-maintenance";
      statusText = '<i class="ri-tools-fill"></i> Bảo trì';
    }

    roomCard.className = `cstm-room-card ${statusClass}`;
    roomCard.innerHTML = `
      <div class="room-name" title="${room.roomName}">[${room.roomCode || "N/A"}] ${room.roomName}</div>
      <div class="room-type-badge">${room.roomCategory} - ${room.roomType || "CHƯA PHÂN LOẠI"}</div>
      <span class="room-status-text">${statusText}</span>
    `;
    roomGrid.appendChild(roomCard);
  });
}

// =============================================================================
// ⚡ LOAD DANH SÁCH CHỜ DUYỆT THẬT
// =============================================================================
async function loadPendingBookings(token) {
  try {
    const response = await fetch(`${BASE_URL}/bookings/admin/pending-list`, {
      method: "GET",
      headers: getAuthHeaders(token),
    });

    if (response.ok) {
      const actualBookings = await response.json();
      renderPendingBookings(actualBookings);
    } else {
      const textErr = await response.text();
      throw new Error(textErr || `Lỗi hệ thống (${response.status})`);
    }
  } catch (error) {
    console.error("🚨 Chi tiết lỗi load đơn:", error.message);
    renderPendingBookings([]);
  }
}

function renderPendingBookings(bookings) {
  const tbody = document.getElementById("tablePendingBookings");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!bookings || bookings.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center" style="padding: 30px; font-weight: bold; color: var(--cstm-text-muted);">🎉 Đã xử lý xong toàn bộ lịch hẹn chờ duyệt!</td></tr>`;
    return;
  }

  bookings.forEach((bk) => {
    const tr = document.createElement("tr");
    const pillClass =
      bk.paymentStatus === "PAID" ? "pay-pill-paid" : "pay-pill-unpaid";
    const pillText = bk.paymentStatus === "PAID" ? "Đã xong" : "Chờ thu";

    let petDisplay = "N/A";
    let serviceDisplay = "Dịch vụ liên quan";
    if (bk.details && bk.details.length > 0) {
      petDisplay = bk.details.map((d) => d.petName).join(", ");
      serviceDisplay = bk.details.map((d) => d.serviceName).join(", ");
    }

    tr.innerHTML = `
      <td><span class="cstm-customer-badge">${bk.bookingCode || "Mã đơn"}</span></td>
      <td><span class="cstm-pet-tag"><i class="ri-paw-print-fill"></i> ${petDisplay}</span></td>
      <td><span class="badge-service" title="${serviceDisplay}">${serviceDisplay}</span></td>
      <td>
        <div class="time-block-vertical">
          <span class="time-hour"><i class="ri-time-line"></i> ${bk.bookingTime}</span>
          <span class="time-date">${bk.bookingDate}</span>
        </div>
      </td>
      <td><span class="pay-status-pill ${pillClass}">${pillText}</span></td>
      <td>
        <div class="cstm-action-group">
          <button class="cstm-action-btn btn-view" onclick="viewBookingDetails(${bk.bookingId})">Xem</button>
          <button class="cstm-action-btn btn-approve" onclick="handleProcessBooking(${bk.bookingId}, 'APPROVE')">Duyệt</button>
          <button class="cstm-action-btn btn-cancel" onclick="handleProcessBooking(${bk.bookingId}, 'REJECTED')">Hủy</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function viewBookingDetails(bookingId) {
  showLocalToast(`Đang hiển thị hồ sơ lịch hẹn #${bookingId}`, "info");
}

async function handleProcessBooking(bookingId, action) {
  let token =
    localStorage.getItem("petcare_token") ||
    window.parent?.localStorage.getItem("petcare_token");

  let actionText = action === "APPROVE" ? "PHÊ DUYỆT" : "TỪ CHỐI";
  if (!confirm(`Bạn có chắc chắn muốn ${actionText} lịch hẹn này không?`))
    return;

  let targetUrl =
    action === "APPROVE"
      ? `${BASE_URL}/bookings/admin/approve-booking/${bookingId}`
      : `${BASE_URL}/bookings/admin/reject-booking/${bookingId}`;

  try {
    const response = await fetch(targetUrl, {
      method: "PUT",
      headers: getAuthHeaders(token),
    });

    const msg = await response.text();

    if (response.ok) {
      showLocalToast(msg || `Đã xử lý thành công!`, "success");
      loadKPIMetricsAndCharts(token);
      loadPendingBookings(token);
    } else {
      throw new Error(msg || "Thao tác thất bại từ hệ thống.");
    }
  } catch (error) {
    showLocalToast(`${error.message}`, "error");
  }
}

function showLocalToast(message, type = "success") {
  let container =
    window.parent?.document.getElementById("pet-toast-container") ||
    document.getElementById("pet-toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "pet-toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `pet-toast ${type}`;

  toast.style.cssText =
    "background: #ffffff !important; padding: 14px 24px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15); font-weight: 700; font-family: sans-serif; font-size: 14px; display: flex; align-items: center; gap: 12px; min-width: 280px; max-width: 400px; color: #111111 !important;";

  if (type === "success") {
    toast.style.borderLeft = "6px solid #2ecc71";
  } else if (type === "error") {
    toast.style.borderLeft = "6px solid #e74c3c";
  } else {
    toast.style.borderLeft = "6px solid #ff6b00";
  }

  let icon =
    type === "success"
      ? '<i class="ri-checkbox-circle-fill" style="color:#2ecc71; font-size: 18px;"></i>'
      : type === "info"
        ? '<i class="ri-information-fill" style="color:#ff6b00; font-size: 18px;"></i>'
        : '<i class="ri-error-warning-fill" style="color:#e74c3c; font-size: 18px;"></i>';

  toast.innerHTML = `${icon} <span style="color: #111111 !important; font-weight: 700;">${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = "toastFadeOut 0.3s ease forwards";
    setTimeout(() => toast.remove(), 300);
  }, 3200);
}
