/**
 * 🏨 TÊN TỆP: RoomManagement.js
 * Quản lý danh sách phòng, tự động đồng bộ mã số,
 * và kết nối API Spring Boot bảo mật bằng cơ chế petcare_token.
 * 🌟 ĐÃ ĐỒNG BỘ CHUẨN: Cho phép bấm bảo trì đột xuất gỡ dịch vụ liên kết!
 */

const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api/rooms`;

let ALL_ROOMS_DATA = [];
let SEARCH_ROOM_KEYWORD = "";

document.addEventListener("DOMContentLoaded", () => {
  const token = getValidToken();

  if (!token) {
    console.error(
      "❌ Chặn Front-End: Không tìm thấy Token hợp lệ (petcare_token)!",
    );
    showLocalToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  loadRoomsList(token);
  initDropdownFilter(token);
  initModalEvents(token);
});

/**
 * 🌟 HÀM TOÀN CỤC: Nhận từ khóa tìm kiếm từ thanh Tìm kiếm của Dashboard cha
 */
window.handleGlobalSearch = (keyword) => {
  SEARCH_ROOM_KEYWORD = keyword.trim().toLowerCase();
  filterAndRenderRooms(getValidToken());
};

function filterAndRenderRooms(token) {
  const filteredRooms = ALL_ROOMS_DATA.filter((rm) => {
    // Tìm kiếm bao gồm cả các thuộc tính dạng text và tiếng Việt đã map từ Backend
    const textTarget =
      `${rm.roomCode || ""} ${rm.roomName || ""} ${rm.categoryVn || ""} ${rm.roomTypeVn || ""}`.toLowerCase();
    return textTarget.includes(SEARCH_ROOM_KEYWORD);
  });
  renderCardGrid(filteredRooms, token);
}

/**
 * 🎭 XỬ LÝ ĐỔI LỰA CHỌN DROPDOWN ĐỘNG THEO DANH MỤC KHỐI CHỌN
 */
window.handleCategoryChange = (category) => {
  const typeLabel = document.getElementById("lblRoomType");
  const typeSelect = document.getElementById("roomType");
  if (!typeLabel || !typeSelect) return;

  const roomTypeMap = {
    HOTEL: [
      { value: "STANDARD", text: "Standard (Chuồng Thường)" },
      { value: "DELUXE", text: "Deluxe (Căn Hộ Rộng)" },
      { value: "VIP", text: "VIP (Biệt Thự Luxury)" },
    ],
    SPA: [
      { value: "SPA_TABLE", text: "Bàn Cắt Tỉa / Tạo Kiểu" },
      { value: "SPA_TUB", text: "Bồn Tắm / Sấy Spa" },
    ],
    HEALTH: [
      { value: "CLINIC_ROOM", text: "Phòng Khám Đa Khoa" },
      { value: "SURGERY_ROOM", text: "Phẫu Thuật Vô Trùng" },
    ],
  };

  if (category === "HOTEL") {
    typeLabel.innerText = "Hạng Phòng Lưu Trú (HOTEL)";
  } else if (category === "SPA") {
    typeLabel.innerText = "Kiểu Không Gian (SPA)";
  } else {
    typeLabel.innerText = "Kiểu Phòng Bệnh (HEALTH)";
  }

  typeSelect.innerHTML = "";
  const availableTypes = roomTypeMap[category] || roomTypeMap["HEALTH"];

  availableTypes.forEach((type) => {
    const opt = document.createElement("option");
    opt.value = type.value;
    opt.innerText = type.text;
    typeSelect.appendChild(opt);
  });
};

/**
 * 📋 1. API: TẢI DANH SÁCH TOÀN BỘ PHÒNG TỪ CƠ SỞ DỮ LIỆU
 */
async function loadRoomsList(token) {
  try {
    const response = await fetch(`${BASE_URL}/all`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      ALL_ROOMS_DATA = await response.json();

      ALL_ROOMS_DATA.sort((a, b) => {
        if (!a.roomCode) return 1;
        if (!b.roomCode) return -1;
        return a.roomCode.localeCompare(b.roomCode, undefined, {
          numeric: true,
          sensitivity: "base",
        });
      });

      filterAndRenderRooms(token);
    } else {
      showLocalToast("Không thể tải danh sách phòng từ hệ thống!", "error");
    }
  } catch (error) {
    console.error("❌ Lỗi kết nối API lấy danh sách phòng:", error);
  }
}

function renderCardGrid(roomsList, token) {
  const gridContainer = document.getElementById("roomGridBody");
  if (!gridContainer) return;

  gridContainer.innerHTML = "";

  if (!roomsList || roomsList.length === 0) {
    gridContainer.innerHTML = `
        <div style="grid-column: 1/-1; text-align:center; color: #999; padding:50px; font-weight:600;">
            📭 Không tìm thấy phòng vận hành nào phù hợp!
        </div>`;
    return;
  }

  roomsList.forEach((room) => {
    let statusClass = "state-available";
    let statusBadge = `<span class="pill-room-status avail">🟢 Còn Trống</span>`;
    let btnTitle = "Bấm để kích hoạt chế độ sửa chữa hệ thống";

    // 🌟 ĐỒNG BỘ FIX: Dùng thuộc tính status trực tiếp, kèm pill trạng thái từ Backend (statusVn)
    if (room.status === "BUSY") {
      statusClass = "state-busy";
      statusBadge = `<span class="pill-room-status busy">${room.statusVn || "🔴 Đang Có Khách Ở"}</span>`;
      btnTitle =
        "⚠️ Cảnh báo: Bảo trì đột xuất sẽ gỡ phòng khỏi lịch khách hiện tại!";
    } else if (room.status === "BOOKED") {
      statusClass = "state-booked";
      statusBadge = `<span class="pill-room-status booked" style="background-color: #ff9f43; color: white;">${room.statusVn || "🟠 Đã Được Gán"}</span>`;
      btnTitle = "⚠️ Gỡ liên kết phòng khỏi gói dịch vụ hiện tại để đi bảo trì";
    } else if (room.status === "MAINTENANCE") {
      statusClass = "state-maintenance";
      statusBadge = `<span class="pill-room-status maint">${room.statusVn || "🛠️ Bảo Trì"}</span>`;
      btnTitle = "Mở lại trạng thái Sẵn Sàng hoạt động";
    }

    const card = document.createElement("div");
    card.className = `room-luxury-card ${statusClass}`;
    card.innerHTML = `
        <div class="card-top-info">
            <span class="room-badge-code">${room.roomCode}</span>
            ${statusBadge}
        </div>
        
        <h4 class="room-main-title">${room.roomName}</h4>
        
        <div class="room-sub-tags">
            <span class="tag-category">📦 Khối: ${room.categoryVn || room.roomCategory}</span>
            <span class="tag-type">📐 Kiểu: ${room.roomTypeVn || "Chưa phân loại"}</span>
        </div>
        
        <p class="room-desc-note" title="${room.note || ""}">${room.note || "Chưa có cấu hình ghi chú cho phòng này..."}</p>
        
        <div class="room-card-footer">
            <button class="btn-quick-toggle" title="${btnTitle}" id="toggle-btn-${room.roomId}">
                ${room.status === "MAINTENANCE" ? "🔓 Mở Hoạt Động" : "🛠️ Khóa Bảo Trì"}
            </button>
            
            <div class="footer-right-actions">
                <button class="btn-circle-action edit" id="edit-btn-${room.roomId}" title="Xem & Sửa">👁️</button>
            </div>
        </div>
    `;

    gridContainer.appendChild(card);

    const toggleBtn = card.querySelector(`#toggle-btn-${room.roomId}`);
    if (toggleBtn) {
      toggleBtn.onclick = () => toggleQuickMaintain(room, token);
    }

    const editBtn = card.querySelector(`#edit-btn-${room.roomId}`);
    if (editBtn) {
      editBtn.onclick = () => openEditRoomModal(room);
    }
  });
}

/**
 * 🛠️ 2. API: CẬP NHẬT TRẠNG THÁI NHANH (Bảo trì <=> Sẵn sàng)
 */
async function toggleQuickMaintain(room, token) {
  const nextStatus =
    room.status === "MAINTENANCE" ? "AVAILABLE" : "MAINTENANCE";

  let validRoomType = room.roomType;
  if (!validRoomType || validRoomType.trim() === "") {
    if (room.roomCategory === "HOTEL") validRoomType = "STANDARD";
    else if (room.roomCategory === "SPA") validRoomType = "SPA_TABLE";
    else validRoomType = "CLINIC_ROOM";
  }

  const updateRequest = {
    roomName: room.roomName,
    roomCategory: room.roomCategory,
    roomType: validRoomType,
    status: nextStatus,
    note: room.note || "",
  };

  try {
    const response = await fetch(`${BASE_URL}/${room.roomId}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(updateRequest),
    });

    if (response.ok) {
      showLocalToast(
        `🔄 Phòng ${room.roomCode} đã chuyển sang trạng thái ${nextStatus === "MAINTENANCE" ? "BẢO TRÌ (Đã dọn dịch vụ cũ)" : "SẴN SÀNG"}!`,
        "success",
      );
      loadRoomsList(token);
    } else {
      const errText = await response.text();
      console.error("❌ Backend từ chối cập nhật bảo trì nhanh:", errText);
      showLocalToast("Không thể cập nhật trạng thái phòng này!", "error");
    }
  } catch (error) {
    console.error("❌ Lỗi kết nối mạng khi cập nhật bảo trì:", error);
    showLocalToast("Lỗi kết nối hệ thống!", "error");
  }
}

function initModalEvents(token) {
  const modal = document.getElementById("roomModal");
  const btnOpenAdd = document.getElementById("btnOpenAddModal");
  const btnCloseX = document.getElementById("btnCloseModal");
  const btnCancel = document.getElementById("btnCancelModal");
  const form = document.getElementById("formRoom");

  if (!modal || !form) return;

  if (btnOpenAdd) {
    btnOpenAdd.addEventListener("click", () => {
      document.getElementById("modalTitle").innerHTML =
        `<i class="ri-add-box-line"></i> Khởi Tạo Không Gian Mới`;
      form.reset();

      document.getElementById("roomId").value = "";
      document.getElementById("viewCodeGroup").style.display = "none";

      document.getElementById("roomCategory").value = "HEALTH";
      handleCategoryChange("HEALTH");

      if (document.getElementById("optBusyDisabled"))
        document.getElementById("optBusyDisabled").disabled = true;
      if (document.getElementById("optBookedDisabled"))
        document.getElementById("optBookedDisabled").disabled = true;

      document.getElementById("roomStatus").value = "AVAILABLE";
      modal.style.display = "flex";
    });
  }

  const closeModal = () => {
    modal.style.display = "none";
  };
  if (btnCloseX) btnCloseX.addEventListener("click", closeModal);
  if (btnCancel) btnCancel.addEventListener("click", closeModal);

  form.addEventListener("submit", async function (event) {
    event.preventDefault();

    const id = document.getElementById("roomId").value;
    const isUpdate = id !== "";
    const category = document.getElementById("roomCategory").value;

    const roomData = {
      roomName: document.getElementById("roomName").value.trim(),
      roomCategory: category,
      roomType: document.getElementById("roomType").value,
      status: document.getElementById("roomStatus").value,
      note: document.getElementById("roomNote").value.trim(),
    };

    const url = isUpdate ? `${BASE_URL}/${id}` : BASE_URL;
    const method = isUpdate ? "PUT" : "POST";

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(roomData),
      });

      if (response.ok) {
        showLocalToast(
          isUpdate
            ? "🎉 Cập nhật chi tiết phòng thành công!"
            : "🎉 Khởi tạo phòng mới thành công!",
          "success",
        );
        modal.style.display = "none";
        loadRoomsList(token);
      } else {
        showLocalToast(
          "Xử lý thất bại, vui lòng kiểm tra lại dữ liệu!",
          "error",
        );
      }
    } catch (error) {
      console.error("❌ Lỗi gửi submit form phòng:", error);
    }
  });
}

function openEditRoomModal(room) {
  const modal = document.getElementById("roomModal");
  if (!modal || !room) return;

  document.getElementById("modalTitle").innerHTML =
    `<i class="ri-edit-box-line"></i> Cấu Hình Phòng`;

  document.getElementById("roomId").value = room.roomId;
  document.getElementById("viewCodeGroup").style.display = "flex";
  document.getElementById("roomCodeDisplay").value = room.roomCode || "";
  document.getElementById("roomName").value = room.roomName || "";
  document.getElementById("roomCategory").value = room.roomCategory;

  handleCategoryChange(room.roomCategory);

  document.getElementById("roomType").value = room.roomType;
  document.getElementById("roomNote").value = room.note || "";

  if (document.getElementById("optBusyDisabled")) {
    document.getElementById("optBusyDisabled").disabled =
      room.status !== "BUSY";
  }
  if (document.getElementById("optBookedDisabled")) {
    document.getElementById("optBookedDisabled").disabled =
      room.status !== "BOOKED";
  }

  document.getElementById("roomStatus").value = room.status;
  modal.style.display = "flex";
}

function initDropdownFilter(token) {
  const dropdown = document.getElementById("dropdownFilterCategory");
  const selectedText = document.getElementById("dropdownSelectedText");
  const options = document.querySelectorAll("#dropdownListOptions li");

  if (!dropdown || !selectedText) return;

  selectedText.addEventListener("click", (e) => {
    e.stopPropagation();
    dropdown.classList.toggle("open");
  });

  document.addEventListener("click", () => dropdown.classList.remove("open"));

  options.forEach((opt) => {
    opt.addEventListener("click", function () {
      options.forEach((li) => li.classList.remove("active"));
      this.classList.add("active");

      const filterValue = this.getAttribute("data-value");
      selectedText.innerHTML = `${this.innerText} <i class="ri-arrow-down-s-line dropdown-icon-arrow"></i>`;

      if (filterValue === "ALL") {
        SEARCH_ROOM_KEYWORD = "";
        filterAndRenderRooms(token);
      } else {
        const filtered = ALL_ROOMS_DATA.filter(
          (r) => r.roomCategory === filterValue,
        );
        renderCardGrid(filtered, token);
      }
    });
  });
}

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
