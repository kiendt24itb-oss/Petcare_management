// =============================================================================
// 👤 CONFIG ĐỊA CHỈ SERVER TẬP TRUNG & BIẾN TOÀN CỤC (Đồng bộ hệ thống)
// =============================================================================
const SERVER_HOST = "http://localhost:8080";

if (typeof BASE_URL === "undefined") {
  var BASE_URL = `${SERVER_HOST}/api`;
}

let myPetsList = [];
let fullBookingsCache = [];
let availableServicesCache = {}; // 🌟 Bộ nhớ đệm lưu thông tin dịch vụ (bao gồm durationMinutes) bóc từ API

// =============================================================================
// 🛠️ HÀM TẠO TOAST THÔNG BÁO TRƯỢT GÓC PHẢI MÀN HÌNH (Đồng bộ từ Auth)
// =============================================================================
function showPetToast(message, type = "success") {
  const container = document.getElementById("pet-toast-container");
  if (!container) {
    alert(message);
    return;
  }

  const existingToasts = container.querySelectorAll(".pet-toast");
  for (let t of existingToasts) {
    if (t.innerText.includes(message)) return;
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

// =============================================================================
// 🔑 BỘ CÂN BẰNG PHIÊN LÀM VIỆC (Fix lỗi không đọc được Token trong Iframe)
// =============================================================================
function getValidToken() {
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");

  if (!token) token = localStorage.getItem("petcare_token");
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  if (
    token === "null" ||
    token === "undefined" ||
    !token ||
    token.trim() === ""
  ) {
    return null;
  }
  return token;
}

// =============================================================================
// 🔄 SỰ KIỆN KHỞI CHẠY TRANG (DOM CONTENT LOADED)
// =============================================================================
document.addEventListener("DOMContentLoaded", function () {
  const token = getValidToken();
  if (!token) {
    showPetToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  // Khởi tạo chặn ngày quá khứ cho ô Date đầu vào
  const dateInput = document.getElementById("bookingDate");
  if (dateInput) {
    const now = new Date();
    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
    dateInput.min = todayStr;

    dateInput.addEventListener("change", () => {
      init24hTimeSelector();
    });
  }

  // Khởi tạo nạp danh sách Slot Giờ
  init24hTimeSelector();
  const hourSelect = document.getElementById("bookingHour");
  if (hourSelect) {
    hourSelect.addEventListener("change", () => {
      init24hTimeSelector();
    });
  }
  const minuteSelect = document.getElementById("bookingMinute");
  if (minuteSelect) {
    minuteSelect.addEventListener("change", () => {
      init24hTimeSelector();
    });
  }

  // Nạp danh sách lịch hẹn và dữ liệu cấu hình ban đầu
  fetchAndRenderGridBookings(token);
  fetchBookingDataInfo(token);

  // Lắng nghe các nút hành động Form động
  const addRowBtn = document.getElementById("add-pet-service-btn");
  if (addRowBtn) addRowBtn.addEventListener("click", () => createBookingRow());

  const openBkModalBtn = document.getElementById("openBkModalBtn");
  if (openBkModalBtn)
    openBkModalBtn.addEventListener("click", () => openModalForCreate());

  // Lắng nghe sự kiện đóng Modal
  const closeBkModalBtn = document.getElementById("closeBkModalBtn");
  const btnBookingReset = document.getElementById("btnBookingReset");
  if (closeBkModalBtn) closeBkModalBtn.addEventListener("click", closeModal);
  if (btnBookingReset) btnBookingReset.addEventListener("click", closeModal);

  const bookingModal = document.getElementById("bookingModal");
  if (bookingModal) {
    bookingModal.addEventListener("click", (e) => {
      if (e.target === bookingModal) closeModal();
    });
  }

  // Submit form chính và Hủy đơn
  const formUpdateBooking = document.getElementById("formUpdateBooking");
  if (formUpdateBooking) {
    formUpdateBooking.addEventListener("submit", (e) =>
      handleFormSubmit(e, token),
    );
  }

  const btnCancelBooking = document.getElementById("btnCancelBooking");
  if (btnCancelBooking) {
    btnCancelBooking.addEventListener("click", () =>
      handleCancelBooking(token),
    );
  }
});

// ⏳ HÀM TÍNH TỔNG THỜI GIAN THỰC HIỆN CỦA CÁC PHÂN ĐOẠN ĐANG ĐƯỢC CHỌN TRÊN FORM (LẤY TỪ DATA CỦA CÁC Ô CHỌN)
function calculateCurrentTotalDuration() {
  let totalMinutes = 0;
  const detailSelectors = document.querySelectorAll(".cstm-detail-select");
  detailSelectors.forEach((select) => {
    const serviceId = select.value;
    if (serviceId && availableServicesCache[serviceId]) {
      // 🎯 LẤY TỪ DATA: Lấy chính xác trường durationMinutes từ ServiceEntity trả về, nếu không có mới fallback về 45 làm mốc an toàn
      const duration = availableServicesCache[serviceId].durationMinutes || 45;
      totalMinutes += parseInt(duration);
    }
  });
  return totalMinutes > 0 ? totalMinutes : 45;
}

// =============================================================================
// ⏱️ HÀM TỰ ĐỘNG CHẶN GIỜ QUÁ KHỨ & GIỜ TỐI MUỘN THEO ĐÚNG KHUNG (8H - 22H)
// =============================================================================
function init24hTimeSelector() {
  const hourSelect = document.getElementById("bookingHour");
  const dateInput = document.getElementById("bookingDate");
  const minuteSelect = document.getElementById("bookingMinute");
  if (!hourSelect || !dateInput || !minuteSelect) return;

  const now = new Date();
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  const selectedHour = hourSelect.value;
  const selectedDate = dateInput.value;
  const selectedMinute = parseInt(minuteSelect.value) || 0;

  // Lấy tổng thời gian thực hiện động từ database của các dịch vụ đang được tick chọn
  const totalServiceDuration = calculateCurrentTotalDuration();

  // Thu thập ID nhân viên bận phục vụ tính toán trùng lịch
  const selectedStaffIds = Array.from(
    document.querySelectorAll(".cstm-staff-select"),
  )
    .map((sel) => sel.value)
    .filter((val) => val !== "");

  let optionsHTML = `<option value="">-- Giờ --</option>`;

  // Kiểm tra nếu chọn ngày hôm nay và mốc thời gian hiện tại cộng với dịch vụ đã lấn qua 22h tối
  const maxAllowHour = 22 - Math.ceil(totalServiceDuration / 60);
  if (selectedDate === todayStr && currentHour > maxAllowHour) {
    showPetToast(
      `Hệ thống ngừng tiếp nhận đơn do tổng thời gian thực hiện (${totalServiceDuration} phút) vượt quá 22h đêm! Vui lòng dời sang ngày mai.`,
      "error",
    );
  }

  // 🔄 Chạy vòng lặp từ ca 8h sáng đến mốc giới hạn 22h tối
  for (let hour = 8; hour <= 22; hour++) {
    const strHour = hour < 10 ? "0" + hour : String(hour);
    let isDisabled = false;
    let suffix = "Giờ";

    // 1. CHẶN TRÊN: Nếu mốc kết thúc vượt quá 22h00 thì vô hiệu hóa
    if (hour >= 22) {
      isDisabled = true;
      suffix = "Giờ (Hết ca làm việc ❌)";
    } else {
      // Thuật toán tính xem: Mốc giờ đang xét + số phút chọn + tổng thời gian dịch vụ từ DB có lấn qua 22h (1320 phút) không
      const targetEndTime = hour * 60 + selectedMinute + totalServiceDuration;
      if (targetEndTime > 22 * 60) {
        isDisabled = true;
        suffix = `Giờ (Quá 22h do dịch vụ dài ❌)`;
      }
    }

    // 2. Chặn giờ quá khứ trong ngày hôm nay
    if (!isDisabled && selectedDate === todayStr) {
      if (hour < currentHour) {
        isDisabled = true;
        suffix = "Giờ (Quá khứ ❌)";
      } else if (hour === currentHour) {
        if (currentMinute > 30) {
          isDisabled = true;
          suffix = "Giờ (Hết slot ❌)";
        }
      }
    }

    // 3. Kiểm tra bận ca trùng lịch của nhân viên dựa vào cache thời gian thực tế
    if (!isDisabled && selectedDate && selectedStaffIds.length > 0) {
      const isBusy = checkStaffConflictInCache(
        selectedDate,
        `${strHour}:${selectedMinute < 10 ? "0" + selectedMinute : selectedMinute}`,
        selectedStaffIds,
        totalServiceDuration,
      );
      if (isBusy) {
        isDisabled = true;
        suffix = "Giờ (Nhân viên bận ca ❌)";
      }
    }

    optionsHTML += `<option value="${strHour}" ${isDisabled ? 'disabled style="color: #999; background-color: #f5f5f5;"' : ""}>${strHour} ${suffix}</option>`;
  }

  hourSelect.innerHTML = optionsHTML;

  if (selectedHour) {
    const targetOption = hourSelect.querySelector(
      `option[value="${selectedHour}"]`,
    );
    if (targetOption && !targetOption.disabled) {
      hourSelect.value = selectedHour;
    } else {
      hourSelect.value = "";
    }
  }

  // 🔴 LOCK Ô PHÚT ĐỘNG THỜI GIAN THỰC: Khóa mốc :30 nếu tổng thời gian dịch vụ từ data tràn quá 22h00
  const currentSelectedHour = parseInt(hourSelect.value);
  if (!isNaN(currentSelectedHour)) {
    const checkTargetMinutes =
      currentSelectedHour * 60 + 30 + totalServiceDuration;
    if (checkTargetMinutes > 22 * 60) {
      if (minuteSelect.options[1]) minuteSelect.options[1].disabled = true;
      if (minuteSelect.value === "30") minuteSelect.value = "00";
    } else {
      if (minuteSelect.options[1]) minuteSelect.options[1].disabled = false;
    }

    // Xử lý chặn phút quá khứ nếu trùng giờ hiện tại
    if (selectedDate === todayStr && currentSelectedHour === currentHour) {
      if (currentMinute > 0 && currentMinute <= 30) {
        if (minuteSelect.options[0]) minuteSelect.options[0].disabled = true;
        if (minuteSelect.value === "00") minuteSelect.value = "30";
      }
    } else {
      if (minuteSelect.options[0]) minuteSelect.options[0].disabled = false;
    }
  }
}

// 🧮 HÀM KIỂM TRA XUNG ĐỘT LỊCH NHÂN VIÊN THEO THỜI GIAN ĐỘNG CỦA GÓI DỊCH VỤ CỦA DATA LỊCH CŨ
function checkStaffConflictInCache(
  dateStr,
  timeStr,
  staffIds,
  durationMinutes,
) {
  if (!fullBookingsCache || fullBookingsCache.length === 0) return false;

  const currentBookingId = document.getElementById("editingBookingId").value;
  const targetStartTime = new Date(`${dateStr}T${timeStr}`);
  const targetEndTime = new Date(
    targetStartTime.getTime() + durationMinutes * 60 * 1000,
  );

  for (const bk of fullBookingsCache) {
    if (
      bk.bookingDate !== dateStr ||
      bk.status === "CANCELLED" ||
      String(bk.bookingId) === String(currentBookingId)
    ) {
      continue;
    }

    // Tính toán thời gian của lịch hẹn cũ dựa vào mảng con thảy về từ DB
    const bkStartTime = new Date(`${bk.bookingDate}T${bk.bookingTime}`);
    let bkDuration = 45;

    const detailsArr = bk.bookingDetails || bk.details;
    if (detailsArr && detailsArr.length > 0) {
      // 🎯 LẤY TỪ DATA: Quét mảng tính tổng durationMinutes từ cơ sở dữ liệu của lịch hẹn đó
      bkDuration = detailsArr.reduce(
        (acc, curr) => acc + (curr.durationMinutes || 45),
        0,
      );
    }
    const bkEndTime = new Date(bkStartTime.getTime() + bkDuration * 60 * 1000);

    // Kiểm tra Overlapping (giao nhau thời gian làm việc)
    if (targetStartTime < bkEndTime && targetEndTime > bkStartTime) {
      if (detailsArr && detailsArr.length > 0) {
        for (const detail of detailsArr) {
          if (detail.staffId && staffIds.includes(String(detail.staffId))) {
            return true;
          }
        }
      }
    }
  }
  return false;
}

// =============================================================================
// 📥 HÀM FETCH DỮ LIỆU VÀ RENDER GRID BOOKING VÀO GIAO DIỆN CHÍNH
// =============================================================================
async function fetchAndRenderGridBookings(token) {
  const grid = document.getElementById("booking-list-grid");
  const triggerCard = document.getElementById("openBkModalBtn");
  if (!grid) return;

  try {
    const response = await fetch(`${BASE_URL}/bookings/my-bookings`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const bookings = await response.json();
      fullBookingsCache = bookings;

      grid.innerHTML = "";

      bookings.forEach((bk) => {
        const card = document.createElement("div");
        let statusClass = "border-waiting";
        let statusText = "Đang chờ";

        if (bk.status === "PROCESSING") {
          statusClass = "border-processing";
          statusText = "Đang tiến hành";
        } else if (bk.status === "COMPLETED") {
          statusClass = "border-completed";
          statusText = "Đã hoàn thành";
        } else if (bk.status === "CANCELLED") {
          statusClass = "border-cancelled";
          statusText = "Đã hủy bỏ";
        } else if (bk.status === "PENDING_APPROVAL") {
          statusClass = "border-pending-approval";
          statusText = "Chờ Admin duyệt (Vi phạm ⚠️)";
        } else if (bk.status === "REJECTED") {
          statusClass = "border-rejected";
          statusText = "Từ chối tiếp nhận ⛔";
        }

        card.className = `bk-card ${statusClass}`;

        const detailsArr = bk.bookingDetails || bk.details;
        const firstDetail =
          detailsArr && detailsArr.length > 0 ? detailsArr[0] : null;
        const petName = firstDetail ? firstDetail.petName : "Chưa có pet";
        const serviceName = firstDetail
          ? firstDetail.serviceName
          : "Chưa chọn gói";
        const totalItemsCount = detailsArr ? detailsArr.length : 0;
        const formattedTotal = bk.totalPrice
          ? bk.totalPrice.toLocaleString("vi-VN") + "đ"
          : "0đ";

        let paymentText = "Tiền mặt tại quầy";
        if (bk.paymentMethod === "TRANSFER")
          paymentText = "Chuyển khoản Banking";

        const displayTime = bk.bookingTime
          ? bk.bookingTime.substring(0, 5)
          : "--:--";

        let formattedDate = bk.bookingDate || "Chưa rõ";
        if (
          bk.bookingDate &&
          typeof bk.bookingDate === "string" &&
          bk.bookingDate.includes("-")
        ) {
          const parts = bk.bookingDate.split("-");
          if (parts.length === 3) {
            formattedDate = `${parts[2]}/${parts[1]}/${parts[0]}`;
          }
        }

        card.innerHTML = `
            <div class="bk-status-group">
                <span class="bk-status-badge ${bk.status ? bk.status.toLowerCase() : "waiting"}">
                    ${statusText} (${bk.bookingCode || "N/A"})
                </span>
                <span class="bk-payment-badge ${bk.paymentStatus === "PAID" ? "pay-success" : "pay-pending"}">
                    <i class="fa-solid ${bk.paymentStatus === "PAID" ? "fa-circle-check" : "fa-money-bill-wave"}"></i> 
                    ${bk.paymentStatus === "PAID" ? "Đã thu tiền" : paymentText}
                </span>
            </div>
            <div class="bk-service-icon icon-spa">
                <i class="fa-solid fa-file-invoice-dollar"></i>
            </div>
            <div class="bk-pet-name">${petName} ${totalItemsCount > 1 ? `(+${totalItemsCount - 1} bé khác)` : ""}</div>
            <div class="bk-service-type">${serviceName}</div>
            <div class="bk-service-type" style="color: var(--color-spa); font-weight: bold; margin-top: 4px;">Tổng: ${formattedTotal}</div>
            <div class="bk-info-timeline">
                <div class="timeline-item"><i class="fa-regular fa-calendar"></i><span>Ngày: <b>${formattedDate}</b></span></div>
                <div class="timeline-item"><i class="fa-regular fa-clock"></i><span>Giờ: <b>${displayTime}</b></span></div>
            </div>
        `;

        card.addEventListener("click", () =>
          openModalForEdit(bk.bookingId, token),
        );
        grid.appendChild(card);
      });
    }

    if (triggerCard) grid.appendChild(triggerCard);
  } catch (error) {
    console.error("Lỗi lấy danh sách lịch hẹn ngoài Grid:", error);
    if (triggerCard && !grid.contains(triggerCard))
      grid.appendChild(triggerCard);
  }
}

async function fetchBookingDataInfo(token) {
  try {
    const response = await fetch(`${BASE_URL}/customers/booking-info`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const data = await response.json();
      if (data.fullName) {
        document.getElementById("user-avatar-initial").innerText = data.fullName
          .trim()
          .charAt(0)
          .toUpperCase();
        document.getElementById("current-user-name").innerText = data.fullName;
      }
      document.getElementById("current-user-code").innerText =
        "Mã khách hàng: " + (data.customerCode || "Chưa có");

      myPetsList = data.pets || [];
      refreshAllPetSelectors();
    }
  } catch (error) {
    console.error("Lỗi fetch thông tin khách hàng:", error);
  }
}

// =============================================================================
// 🐾 QUẢN LÝ CÁC DÒNG CHỌN THÚ CƯNG VÀ DỊCH VỤ ĐỘNG TRÊN FORM MODAL
// =============================================================================
async function createBookingRow(initialData = null) {
  const dynamicContainer = document.getElementById("dynamic-booking-container");
  if (!dynamicContainer) return;

  const rowId = "row-" + Date.now() + Math.floor(Math.random() * 100);
  const rowDiv = document.createElement("div");
  rowDiv.className = "booking-dynamic-row";
  rowDiv.id = rowId;

  let petOptions = buildPetOptionsHTML();

  rowDiv.innerHTML = `
        <div class="row-main-inputs">
            <div class="input-cell">
                <select class="modal-select cstm-pet-select" required>${petOptions}</select>
            </div>
            <div class="category-tabs-group">
                <button type="button" class="cate-tab-btn active" data-cate="SPA"><i class="fa-solid fa-scissors"></i> Spa</button>
                <button type="button" class="cate-tab-btn" data-cate="HEALTH"><i class="fa-solid fa-stethoscope"></i> Thú Y</button>
                <button type="button" class="cate-tab-btn" data-cate="HOTEL"><i class="fa-solid fa-hotel"></i> Khách Sạn</button>
            </div>
            <div class="input-cell">
                <select class="modal-select cstm-detail-select" required><option value="">Đang tải...</option></select>
            </div>
            <div class="input-cell">
                <select class="modal-select cstm-staff-select"><option value="">Đang tải...</option></select>
            </div>
        </div>
        <button type="button" class="btn-remove-row-item">&times;</button>
    `;

  rowDiv.querySelector(".btn-remove-row-item").addEventListener("click", () => {
    if (dynamicContainer.children.length > 1) {
      rowDiv.remove();
      init24hTimeSelector();
    } else {
      showPetToast("Yêu cầu lịch hẹn phải chọn ít nhất một bé!", "error");
    }
  });

  const tabs = rowDiv.querySelectorAll(".cate-tab-btn");
  tabs.forEach((tab) => {
    tab.addEventListener("click", async (e) => {
      if (tab.style.pointerEvents === "none") return;
      tabs.forEach((t) => t.classList.remove("active"));
      const clickedTab = e.currentTarget;
      clickedTab.classList.add("active");
      await updateCascadeSelectorsFromAPI(
        rowDiv,
        clickedTab.getAttribute("data-cate"),
      );
    });
  });

  const detailSelect = rowDiv.querySelector(".cstm-detail-select");
  if (detailSelect) {
    detailSelect.addEventListener("change", () => {
      // 🔄 KHI ĐỔI GÓI DỊCH VỤ: Lập tức gọi hàm quét thời gian động để render lại cấu trúc chặn ca ô Giờ
      init24hTimeSelector();
    });
  }

  const staffSelect = rowDiv.querySelector(".cstm-staff-select");
  if (staffSelect) {
    staffSelect.addEventListener("change", () => {
      init24hTimeSelector();
    });
  }

  dynamicContainer.appendChild(rowDiv);

  if (initialData) {
    rowDiv.querySelector(".cstm-pet-select").value = initialData.petId || "";
    tabs.forEach((t) => {
      if (t.getAttribute("data-cate") === initialData.categoryType) {
        tabs.forEach((tabBtn) => tabBtn.classList.remove("active"));
        t.classList.add("active");
      }
    });
    await updateCascadeSelectorsFromAPI(
      rowDiv,
      initialData.categoryType || "SPA",
      initialData,
    );
  } else {
    await updateCascadeSelectorsFromAPI(rowDiv, "SPA");
  }
}

// =============================================================================
// 🛠️ NÂNG CẤP UI DROPDOWN: CHÈN THÊM Badge THỜI GIAN THỰC HIỆN BỐC TỪ DATA
// =============================================================================
async function updateCascadeSelectorsFromAPI(
  rowElement,
  category,
  initialData = null,
) {
  const token = getValidToken();
  const detailSelect = rowElement.querySelector(".cstm-detail-select");
  const staffSelect = rowElement.querySelector(".cstm-staff-select");

  if (!detailSelect || !staffSelect) return;
  detailSelect.disabled = true;
  staffSelect.disabled = true;

  try {
    let detailOptions = `<option value="">-- Chọn gói dịch vụ --</option>`;
    const responseService = await fetch(
      `${BASE_URL}/services/active?category=${category}`,
      {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    if (responseService.ok) {
      const services = await responseService.json();

      const completedServices = services.filter((s) => {
        const directCode = s.roomCode ? String(s.roomCode).trim() : "";
        const nestedCode =
          s.room && s.room.roomCode ? String(s.room.roomCode).trim() : "";
        const isDirectValid =
          directCode !== "" && directCode.toLowerCase() !== "chưa gán";
        const isNestedValid =
          nestedCode !== "" && nestedCode.toLowerCase() !== "chưa gán";
        return isDirectValid || isNestedValid;
      });

      completedServices.forEach((s) => {
        // Lưu nguyên Object data trả về vào cache toàn cục để FE bốc đầu tính toán
        availableServicesCache[s.serviceId] = s;

        let roomDisplay = s.roomCode || (s.room && s.room.roomCode) || "N/A";

        // 🎯 LẤY TỪ DATA: Bốc chuẩn trường durationMinutes từ ServiceEntity dưới Java
        const serviceDuration = s.durationMinutes || 60;

        // Cơ cấu lại UI Dropdown cực đẹp, phân tách thông tin rành mạch cho khách hàng dễ đọc
        detailOptions += `<option value="${s.serviceId}">
          ${s.serviceName} | 💰 ${s.price.toLocaleString("vi-VN")}đ | ⏱️ ${serviceDuration} phút [P: ${roomDisplay}]
        </option>`;
      });
    }
    detailSelect.innerHTML = detailOptions;
    detailSelect.disabled = false;
    if (initialData && initialData.serviceId)
      detailSelect.value = initialData.serviceId;
  } catch (err) {
    console.error("Lỗi nạp dịch vụ kèm mã phòng:", err);
  }

  try {
    let staffOptions = `<option value="">-- Tự động điều phối nhân sự --</option>`;
    const responseStaff = await fetch(
      `${BASE_URL}/staffs/active?department=${category}`,
      {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    if (responseStaff.ok) {
      const staffs = await responseStaff.json();
      staffs.forEach((st) => {
        staffOptions += `<option value="${st.staffId}">${st.fullName} (${st.positionName})</option>`;
      });
    }
    staffSelect.innerHTML = staffOptions;
    staffSelect.disabled = false;
    if (initialData && initialData.staffId)
      staffSelect.value = initialData.staffId;

    init24hTimeSelector();
  } catch (err) {
    console.error("Lỗi nạp nhân viên theo phòng ban:", err);
  }
}

// =============================================================================
// 🔑 ĐIỀU KHIỂN ĐÓNG MỞ VÀ RESET TRẠNG THÁI MODAL FORM
// =============================================================================
function openModalForCreate() {
  document.getElementById("editingBookingId").value = "";
  document.getElementById("modal-title").innerText =
    "✨ Đăng Ký Lịch Hẹn Đặt Chỗ";

  document.getElementById("btnBookingSubmit").innerText = "Đặt Lịch Ngay";
  document.getElementById("btnBookingSubmit").style.display = "inline-block";
  document.getElementById("btnCancelBooking").style.display = "none";
  document.getElementById("add-pet-service-btn").style.display = "inline-block";

  document.getElementById("bookingDate").disabled = false;
  document.getElementById("bookingHour").disabled = false;
  document.getElementById("bookingMinute").disabled = false;
  document.getElementById("note").disabled = false;
  if (document.getElementById("paymentMethod"))
    document.getElementById("paymentMethod").disabled = false;

  document.getElementById("formUpdateBooking").reset();
  document.getElementById("dynamic-booking-container").innerHTML = "";

  const oldTimeline = document.getElementById("booking-timeline-box");
  if (oldTimeline) oldTimeline.remove();

  if (document.getElementById("bookingHour"))
    document.getElementById("bookingHour").value = "";
  if (document.getElementById("bookingMinute"))
    document.getElementById("bookingMinute").value = "00";

  init24hTimeSelector();
  createBookingRow();
  openModal();
}

async function openModalForEdit(bookingId, token) {
  const bk = fullBookingsCache.find((b) => b.bookingId === bookingId);
  if (!bk) return;

  document.getElementById("editingBookingId").value = bk.bookingId;
  document.getElementById("modal-title").innerText =
    `🛠️ Lịch Hẹn: ${bk.bookingCode}`;
  document.getElementById("btnBookingSubmit").innerText = "Cập Nhật Lịch Hẹn";

  document.getElementById("bookingDate").value = bk.bookingDate;

  if (bk.bookingTime) {
    const timeParts = bk.bookingTime.split(":");
    if (timeParts.length >= 2) {
      document.getElementById("bookingHour").value = timeParts[0];
      document.getElementById("bookingMinute").value = timeParts[1];
    }
  }

  if (document.getElementById("paymentMethod")) {
    document.getElementById("paymentMethod").value = bk.paymentMethod || "CASH";
  }
  document.getElementById("note").value = bk.note || "";

  await renderBookingTimelineUI(bookingId, token);

  const dynamicContainer = document.getElementById("dynamic-booking-container");
  dynamicContainer.innerHTML = "";

  const detailsArr = bk.bookingDetails || bk.details;
  if (detailsArr && detailsArr.length > 0) {
    for (const item of detailsArr) {
      if (item.serviceId) {
        // Nạp trước thông tin vào cache cho chế độ xem lịch cũ
        availableServicesCache[item.serviceId] = {
          serviceId: item.serviceId,
          serviceName: item.serviceName,
          durationMinutes: item.durationMinutes || 45,
        };
      }
      await createBookingRow({
        petId: item.petId,
        serviceId: item.serviceId,
        staffId: item.staffId,
        categoryType: item.categoryType || "SPA",
      });
    }
  } else {
    await createBookingRow();
  }

  if (
    bk.status === "PROCESSING" ||
    bk.status === "COMPLETED" ||
    bk.status === "CANCELLED" ||
    bk.status === "REJECTED"
  ) {
    document.getElementById("btnBookingSubmit").style.display = "none";
    document.getElementById("btnCancelBooking").style.display = "none";
    document.getElementById("add-pet-service-btn").style.display = "none";

    let label = "ĐỂ XEM 👁️";
    if (bk.status === "PROCESSING") label = "ĐANG LÀM ✂️";
    else if (bk.status === "COMPLETED") label = "HOÀN THÀNH ✅";
    else if (bk.status === "CANCELLED") label = "ĐÃ HỦY ❌";
    else if (bk.status === "REJECTED") label = "BỊ TỪ CHỐI ⛔";

    document.getElementById("modal-title").innerText =
      `👁️ Lịch Hẹn: ${bk.bookingCode} [Trạng thái: ${label} - CHẾ ĐỘ CHỈ XEM]`;

    document.getElementById("bookingDate").disabled = true;
    document.getElementById("bookingHour").disabled = true;
    document.getElementById("bookingMinute").disabled = true;
    document.getElementById("note").disabled = true;
    if (document.getElementById("paymentMethod"))
      document.getElementById("paymentMethod").disabled = true;

    setTimeout(() => {
      document
        .querySelectorAll(".booking-dynamic-row select")
        .forEach((sel) => (sel.disabled = true));
      document
        .querySelectorAll(".cate-tab-btn")
        .forEach((tab) => (tab.style.pointerEvents = "none"));
      document
        .querySelectorAll(".btn-remove-row-item")
        .forEach((btn) => (btn.style.display = "none"));
    }, 300);
  } else {
    document.getElementById("btnBookingSubmit").style.display = "inline-block";
    document.getElementById("btnCancelBooking").style.display = "inline-block";
    document.getElementById("add-pet-service-btn").style.display =
      "inline-block";

    document.getElementById("bookingDate").disabled = false;
    document.getElementById("bookingHour").disabled = false;
    document.getElementById("bookingMinute").disabled = false;
    document.getElementById("note").disabled = false;
    if (document.getElementById("paymentMethod"))
      document.getElementById("paymentMethod").disabled = false;
  }

  init24hTimeSelector();
  openModal();
}

async function renderBookingTimelineUI(bookingId, token) {
  const form = document.getElementById("formUpdateBooking");
  let timelineBox = document.getElementById("booking-timeline-box");

  if (timelineBox) timelineBox.innerHTML = "";
  else {
    timelineBox = document.createElement("div");
    timelineBox.id = "booking-timeline-box";
    timelineBox.style =
      "margin: 16px 0; padding: 16px; background: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0;";
    form.insertBefore(
      timelineBox,
      form.querySelector(".modal-field-row").nextSibling,
    );
  }

  try {
    const res = await fetch(`${BASE_URL}/bookings/${bookingId}/logs`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (res.ok) {
      const logs = await res.json();
      let html = `<h4 style="margin: 0 0 12px 0; color: #334155; font-size: 14px;"><i class="fa-solid fa-timeline"></i> Nhật ký hành trình dịch vụ</h4>`;

      if (logs.length === 0) {
        html += `<p style="color: #94a3b8; font-size: 13px; margin: 0;">Chưa có biến động nào được ghi nhận.</p>`;
      } else {
        html += `<div style="position: relative; padding-left: 20px; border-left: 2px dashed #cbd5e1; margin-left: 8px;">`;
        logs.forEach((log) => {
          html += `
            <div style="position: relative; margin-bottom: 12px;">
              <span style="position: absolute; left: -26px; top: 3px; width: 10px; height: 10px; background: var(--color-spa, #ff6600); border-radius: 50%; border: 2px solid #fff;"></span>
              <span style="font-weight: bold; font-size: 12px; color: #64748b; display: block;">${log.logTime.substring(0, 5)}</span>
              <p style="margin: 2px 0 0 0; font-size: 13px; color: #1e293b;">${log.logText}</p>
            </div>`;
        });
        html += `</div>`;
      }
      timelineBox.innerHTML = html;
    }
  } catch (err) {
    console.error("Lỗi vẽ timeline:", err);
  }
}

function openModal() {
  document.getElementById("bookingModal").classList.add("active");
}
function closeModal() {
  document.getElementById("bookingModal").classList.remove("active");
}

function buildPetOptionsHTML() {
  let petOptions = `<option value="">-- Chọn bé thú cưng --</option>`;
  if (myPetsList.length === 0) {
    petOptions = `<option value="">Chưa có thú cưng nào!</option>`;
  } else {
    myPetsList.forEach((p) => {
      petOptions += `<option value="${p.petId}">${p.petName}</option>`;
    });
  }
  return petOptions;
}

function refreshAllPetSelectors() {
  const petSelectors = document.querySelectorAll(".cstm-pet-select");
  const optionsHTML = buildPetOptionsHTML();
  petSelectors.forEach((select) => {
    const currentValue = select.value;
    select.innerHTML = optionsHTML;
    if (currentValue) select.value = currentValue;
  });
}

// =============================================================================
// 🚀 XỬ LÝ SUBMIT FORM
// =============================================================================
async function handleFormSubmit(e, token) {
  e.preventDefault();

  const bookingId = document.getElementById("editingBookingId").value;
  const bookingDate = document.getElementById("bookingDate").value;
  const hourVal = document.getElementById("bookingHour").value;
  const minVal = document.getElementById("bookingMinute").value;

  if (!hourVal) {
    showPetToast("Vui lòng chọn Giờ hẹn cụ thể!", "error");
    return;
  }
  const bookingTime = `${hourVal}:${minVal}`;

  // Kiểm tra thời gian kết thúc dựa vào data thực tế của DB xem có tràn ca (quá 22h) hay không
  const totalDuration = calculateCurrentTotalDuration();
  const selectedMinutesTotal =
    parseInt(hourVal) * 60 + parseInt(minVal) + totalDuration;
  if (selectedMinutesTotal > 22 * 60) {
    showPetToast(
      `Tổng thời gian thực hiện của các dịch vụ (${totalDuration} phút) vượt quá 22h00 tối. Vui lòng chọn giờ sớm hơn!`,
      "error",
    );
    return;
  }

  const nowCheck = new Date();
  const selectedDateObj = new Date(bookingDate + "T" + bookingTime);
  if (selectedDateObj < nowCheck) {
    showPetToast(
      "Thời gian đặt lịch không được nằm trong quá khứ! Vui lòng chọn giờ khác.",
      "error",
    );
    return;
  }

  const staffIds = Array.from(document.querySelectorAll(".cstm-staff-select"))
    .map((sel) => sel.value)
    .filter((val) => val !== "");
  if (
    checkStaffConflictInCache(bookingDate, bookingTime, staffIds, totalDuration)
  ) {
    showPetToast(
      "Khung giờ này nhân viên được chọn đã bận ca khác. Vui lòng đổi giờ!",
      "error",
    );
    return;
  }

  const note = document.getElementById("note").value;
  const paymentMethod = document.getElementById("paymentMethod")
    ? document.getElementById("paymentMethod").value
    : "CASH";

  const rowElements = document.querySelectorAll(".booking-dynamic-row");
  const details = [];

  rowElements.forEach((row) => {
    const petId = row.querySelector(".cstm-pet-select").value;
    const serviceId = row.querySelector(".cstm-detail-select").value;
    const staffId = row.querySelector(".cstm-staff-select").value;

    if (petId && serviceId) {
      details.push({
        petId: parseInt(petId),
        serviceId: parseInt(serviceId),
        staffId: staffId ? parseInt(staffId) : null,
      });
    }
  });

  if (details.length === 0) {
    showPetToast("Vui lòng chọn ít nhất 1 thú cưng và gói dịch vụ!", "error");
    return;
  }

  const payload = {
    bookingDate,
    bookingTime,
    note,
    paymentMethod,
    items: details,
    bookingDetails: details,
  };

  try {
    let url = `${BASE_URL}/bookings/create`;
    let method = "POST";
    if (bookingId) {
      url = `${BASE_URL}/bookings/update/${bookingId}`;
      method = "PUT";
    }

    const response = await fetch(url, {
      method: method,
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      showPetToast(
        bookingId
          ? "🎉 Cập nhật lịch hẹn thành công!"
          : "🎉 Đặt lịch hẹn thành công!",
        "success",
      );
      closeModal();
      fetchAndRenderGridBookings(token);
    } else {
      const errorMsg = await response.text();
      showPetToast(
        errorMsg || "Thao tác thất bại, vui lòng kiểm tra lại!",
        "error",
      );
    }
  } catch (error) {
    console.error(error);
  }
}

// =============================================================================
// ❌ XỬ LÝ HỦY LỊCH HẸN TRỰC TIẾP
// =============================================================================
async function handleCancelBooking(token) {
  const bookingId = document.getElementById("editingBookingId").value;
  if (!bookingId) return;

  if (!confirm("Ní có chắc chắn muốn hủy bỏ lịch hẹn đặt chỗ này không?"))
    return;

  try {
    const response = await fetch(`${BASE_URL}/bookings/cancel/${bookingId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      showPetToast("🎉 Đã hủy bỏ lịch hẹn đặt chỗ thành công!", "success");
      closeModal();
      fetchAndRenderGridBookings(token);
    } else {
      const errorMsg = await response.text();
      showPetToast(errorMsg || "Không thể hủy đơn lịch hẹn này!", "error");
    }
  } catch (error) {
    console.error("Lỗi khi xử lý hủy đơn:", error);
  }
}
