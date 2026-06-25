/**
 * ⚡ FILE: ServiceManagement.js
 * Quản lý đồng bộ bảng giá dịch vụ và kết nối hiển thị mã phòng/thẻ phòng.
 * ĐÃ CHỈNH SỬA: Đồng bộ 100% logic nhả phòng, khóa bảo trì với Back-End mới.
 */

const SERVER_HOST = "http://localhost:8080";
const API_SERVICES = `${SERVER_HOST}/api/services`;
const API_ROOMS = `${SERVER_HOST}/api/rooms`;

let ALL_SERVICES_DATA = [];
let ALL_ROOMS_DATA = [];
let CURRENT_ACTIVE_TAB = "HEALTH";

document.addEventListener("DOMContentLoaded", function () {
  const token = getValidToken();
  if (!token) {
    showLocalToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  // Tải song song cả hai nguồn dữ liệu sống ban đầu
  Promise.all([loadRoomsData(token), loadServicesData(token)]).then(() => {
    switchServiceTab("HEALTH");
  });

  initFormSubmitEvent(token);
});

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

async function loadRoomsData(token) {
  try {
    const response = await fetch(`${API_ROOMS}/all`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (response.ok) {
      ALL_ROOMS_DATA = await response.json();
    }
  } catch (error) {
    console.error("❌ Lỗi tải danh sách phòng:", error);
  }
}

async function loadServicesData(token) {
  try {
    const response = await fetch(`${API_SERVICES}/all`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (response.ok) {
      ALL_SERVICES_DATA = await response.json();
    }
  } catch (error) {
    console.error("❌ Lỗi tải bảng giá dịch vụ:", error);
  }
}

window.switchServiceTab = function (categoryEnum) {
  CURRENT_ACTIVE_TAB = categoryEnum;

  document
    .querySelectorAll(".tab-btn")
    .forEach((btn) => btn.classList.remove("active"));
  const clickedBtn = document.querySelector(
    `button[onclick="switchServiceTab('${categoryEnum}')"]`,
  );
  if (clickedBtn) clickedBtn.classList.add("active");

  document
    .querySelectorAll(".service-panel")
    .forEach((panel) => panel.classList.remove("active"));
  const activePanel = document.getElementById(`panel-${categoryEnum}`);
  if (activePanel) activePanel.classList.add("active");

  const filteredServices = ALL_SERVICES_DATA.filter(
    (s) => s.category === categoryEnum,
  );

  if (categoryEnum === "HOTEL") {
    renderTableBody("tableHotelBody", filteredServices);
    renderHotelGrid(); // Đổ card danh sách phòng xuống dưới
  } else if (categoryEnum === "SPA") {
    renderTableBody("tableSpaBody", filteredServices);
  } else {
    renderTableBody("tableHealthBody", filteredServices);
  }
};

/**
 * 📋 ĐỔ DỮ LIỆU BẢNG - CHỈ HIỂN THỊ MÃ PHÒNG GỌN GÀNG THEO Ý NÍ
 */
function renderTableBody(tableId, services) {
  const tbody = document.getElementById(tableId);
  if (!tbody) return;
  tbody.innerHTML = "";

  if (services.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center" style="color: #999;">📭 Chưa cấu hình gói dịch vụ nào.</td></tr>`;
    return;
  }

  services.forEach((srv) => {
    let roomCodeDisplay = srv.roomCode || "Chưa gán";

    const tr = document.createElement("tr");
    tr.style.opacity = srv.status ? "1" : "0.6";
    tr.innerHTML = `
      <td><span class="badge-code">${srv.serviceCode}</span></td>
      <td><strong>${srv.serviceName}</strong></td>
      <td><span class="room-badge-indicator">🚪 ${roomCodeDisplay}</span></td>
      <td><span style="font-size:13px; color:#666;">${srv.description || "Chưa có ghi chú..."}</span></td>
      <td><span class="price-text">${srv.price.toLocaleString("vi-VN")} đ</span></td>
      <td class="text-center">
        <div class="cstm-action-group">
          <button class="btn-icon-action" title="Xem Chi Tiết Cụ Thể" onclick="openEditService('${srv.serviceId}')">👁️</button>
          <button class="btn-icon-action delete" title="Bật/Tắt Kinh Doanh" onclick="toggleService('${srv.serviceId}')">
            ${srv.status ? "🛑" : "🟢"}
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

/**
 * 🏨 SƠ ĐỒ THẺ CARD LUXURY CHO PHÒNG LƯU TRÚ (TAB HOTEL)
 * 🌟 ĐÃ ĐỒNG BỘ: Chuẩn hóa nhãn text tiếng Việt đồng bộ hệ thống.
 */
function renderHotelGrid() {
  const grid = document.getElementById("hotelGridBody");
  if (!grid) return;
  grid.innerHTML = "";

  const hotelRooms = ALL_ROOMS_DATA.filter((r) => r.roomCategory === "HOTEL");

  if (hotelRooms.length === 0) {
    grid.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:30px; color:#999;">📭 Không tìm thấy phòng lưu trú nào!</div>`;
    return;
  }

  hotelRooms.forEach((room) => {
    let cardClass = "cstm-empty";
    let statusText = "🟢 Còn Trống";

    if (room.status === "BUSY") {
      cardClass = "cstm-occupied";
      statusText = "🔴 Đang Có Khách Ở";
    } else if (room.status === "BOOKED") {
      cardClass = "cstm-booked";
      statusText = "🟠 Đã Được Gán Dịch Vụ";
    } else if (room.status === "MAINTENANCE") {
      cardClass = "cstm-maintain";
      statusText = "🛠️ Đang Bảo Trì";
    }

    const card = document.createElement("div");
    card.className = `hotel-room-card ${cardClass}`;
    card.innerHTML = `
      <div class="room-icon-wrapper">🏨</div>
      <div class="room-name">${room.roomName}</div>
      <div style="margin-bottom: 10px;"><span class="pill-room-status">${statusText}</span></div>
      <div style="font-size:12px; color:#777; font-weight:700;">Mã phòng: ${room.roomCode}</div>
    `;
    grid.appendChild(card);
  });
}

window.openPriceModal = function (categoryEnum) {
  const modal = document.getElementById("priceModal");
  const form = document.getElementById("formPriceService");
  if (!modal || !form) return;

  form.reset();
  document.getElementById("priceTargetId").value = "";
  document.getElementById("priceServiceType").value = categoryEnum;
  document.getElementById("modalPriceTitle").innerText =
    `Thêm Mới Gói Phục Vụ - ${categoryEnum}`;
  document.getElementById("inputServiceStatus").value = "true";

  buildRoomDropdown(categoryEnum, "");
  modal.style.display = "flex";
};

window.openEditService = function (serviceId) {
  const srv = ALL_SERVICES_DATA.find((s) => s.serviceId == serviceId);
  if (!srv) return;

  const modal = document.getElementById("priceModal");
  if (!modal) return;

  document.getElementById("modalPriceTitle").innerText =
    "Cấu Hình Chi Tiết & Phòng Phục Vụ";
  document.getElementById("priceTargetId").value = srv.serviceId;
  document.getElementById("priceServiceType").value = srv.category;
  document.getElementById("inputServiceName").value = srv.serviceName;
  document.getElementById("inputServicePrice").value = srv.price;
  document.getElementById("inputServiceDuration").value =
    srv.durationMinutes || 30;
  document.getElementById("inputServiceDesc").value = srv.description || "";
  document.getElementById("inputServiceStatus").value = srv.status.toString();

  buildRoomDropdown(srv.category, srv.roomCode);
  modal.style.display = "flex";
};

/**
 * 🛠️ ĐÃ CẬP NHẬT: Ngăn chặn gán phòng bảo trì triệt để từ giao diện Front-End
 */
function buildRoomDropdown(categoryEnum, currentRoomCode) {
  const selectRoom = document.getElementById("inputServiceRoomId");
  if (!selectRoom) return;

  const matchedRooms = ALL_ROOMS_DATA.filter(
    (r) => r.roomCategory === categoryEnum,
  );

  if (matchedRooms.length === 0) {
    selectRoom.innerHTML = `<option value="">-- Dùng chung khu vực ${categoryEnum} mặc định --</option>`;
    return;
  }

  let optionsHtml = `<option value="">-- Chọn phòng vận hành cụ thể --</option>`;

  matchedRooms.forEach((rm) => {
    let statusVi = "Còn Trống";
    if (rm.status === "BUSY") statusVi = "Đang Có Khách Ở";
    else if (rm.status === "BOOKED") statusVi = "Đã Được Gán";
    else if (rm.status === "MAINTENANCE") statusVi = "Đang Bảo Trì 🛠️";

    const isCurrentAssignedRoom =
      currentRoomCode && rm.roomCode === currentRoomCode;

    // 🌟 ĐỒNG BỘ MỚI: Nếu phòng đang sửa chữa (MAINTENANCE) thì BẮT BUỘC KHÓA, không cho phép giữ lại kể cả là phòng cũ
    const shouldDisable =
      (rm.status === "MAINTENANCE" ||
        rm.status === "BOOKED" ||
        rm.status === "BUSY") &&
      !isCurrentAssignedRoom;

    const isSelected = isCurrentAssignedRoom ? "selected" : "";
    const isDisabledAttr = shouldDisable ? "disabled" : "";

    const lockNote = shouldDisable
      ? ` [Không Thể Chọn - ${statusVi}]`
      : ` [${statusVi}]`;

    optionsHtml += `<option value="${rm.roomCode}" ${isSelected} ${isDisabledAttr}>🚪 Phòng ${rm.roomName} (${rm.roomCode})${lockNote}</option>`;
  });

  selectRoom.innerHTML = optionsHtml;
}

window.closePriceModal = function () {
  document.getElementById("priceModal").style.display = "none";
};

window.toggleService = async function (serviceId) {
  const token = getValidToken();
  try {
    const response = await fetch(`${API_SERVICES}/toggle/${serviceId}`, {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (response.ok) {
      const data = await response.json();
      showLocalToast(
        data.message || "Thay đổi trạng thái kinh doanh thành công!",
        "success",
      );

      // Đồng bộ tải lại toàn bộ thông tin để tránh lệch trạng thái
      await Promise.all([loadRoomsData(token), loadServicesData(token)]);
      switchServiceTab(CURRENT_ACTIVE_TAB);
    } else {
      showLocalToast("Không thể thay đổi trạng thái dịch vụ!", "error");
    }
  } catch (error) {
    console.error("❌ Lỗi bật tắt dịch vụ:", error);
    showLocalToast("Có lỗi xảy ra khi kết nối máy chủ!", "error");
  }
};

function initFormSubmitEvent(token) {
  const form = document.getElementById("formPriceService");
  if (!form) return;

  form.addEventListener("submit", async function (e) {
    e.preventDefault();

    const id = document.getElementById("priceTargetId").value;
    const isUpdate = id !== "";
    const category = document.getElementById("priceServiceType").value;

    const servicePayload = {
      serviceName: document.getElementById("inputServiceName").value.trim(),
      category: category,
      roomCode: document.getElementById("inputServiceRoomId").value,
      price: parseFloat(document.getElementById("inputServicePrice").value),
      durationMinutes: parseInt(
        document.getElementById("inputServiceDuration").value,
      ),
      description: document.getElementById("inputServiceDesc").value.trim(),
      status: document.getElementById("inputServiceStatus").value === "true",
    };

    const url = isUpdate
      ? `${API_SERVICES}/update/${id}`
      : `${API_SERVICES}/create`;
    const method = isUpdate ? "PUT" : "POST";

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(servicePayload),
      });

      if (response.ok) {
        showLocalToast(
          isUpdate
            ? "🎉 Lưu cấu hình dịch vụ thành công!"
            : "🎉 Khởi tạo dịch vụ thành công!",
          "success",
        );
        document.getElementById("priceModal").style.display = "none";

        // 🌟 ĐỒNG BỘ QUAN TRỌNG: Gọi song song để cập nhật tức thì màu Grid của cả Phòng và Dịch vụ vừa gán!
        await Promise.all([loadRoomsData(token), loadServicesData(token)]);
        switchServiceTab(category);
      } else {
        showLocalToast(
          "Xử lý thất bại, hãy kiểm tra lại dữ liệu đầu vào hoặc trạng thái phòng!",
          "error",
        );
      }
    } catch (error) {
      console.error("❌ Lỗi gửi form:", error);
    }
  });
}

function showLocalToast(message, type = "success") {
  let container = document.getElementById("pet-toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "pet-toast-container";
    document.body.appendChild(container);
  }
  const toast = document.createElement("div");
  toast.style.cssText =
    "position: fixed; top: 20px; right: 20px; background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.15); padding: 12px 20px; border-radius: 8px; z-index: 9999; display: flex; align-items: center; gap: 10px; border-left: 5px solid " +
    (type === "success" ? "#2ec4b6" : "#dc3545");
  toast.innerHTML = `<span>${type === "success" ? "🎉" : "❌"}</span> <span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}
