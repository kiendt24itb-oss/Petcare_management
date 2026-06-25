/**
 * 👥 FILE: StaffManagement.js
 * Quản lý danh sách nhân viên, upload avatar trực quan và đồng bộ chức vụ động từ DB
 * ĐÃ HOÀN THIỆN: Kết nối API Backend real-time đổ dữ liệu cho Top Widgets và Bảng chấm công
 */

const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

const DEFAULT_SVG_AVATAR =
  "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwJSIgeTE9IjOlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0b3AtY29sb3I9IiNmZjlhOWUiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZWNmZWYiLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0idXJsKCNnKSIvPldjdXN0b21lcjxjaXJjbGUgY3g9IjUwIiBjeT0iNDIiIHI9IjE4IiBmaWxsPSIjZmZmIi8+PHBhdGggZD0iTTI1LDc1IEMyNSw1OCA3NSw1OCA3NSw3NSIgZmlsbD0iI2ZmZiIvPjwvc3ZnPg==";

// 🌟 Biến lưu trữ dữ liệu tập trung
let ALL_STAFFS_DATA = [];
let ATTENDANCE_DASHBOARD_DATA = []; // Lưu trữ mảng trạng thái chấm công của toàn tiệm từ API dashboard-status
let SEARCH_STAFF_KEYWORD = "";
let listPositionsGlobal = [];
let uploadedAvatarPath = null;

document.addEventListener("DOMContentLoaded", function () {
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");

  if (!token) token = localStorage.getItem("petcare_token");
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  if (
    !token ||
    token === "null" ||
    token === "undefined" ||
    token.trim() === ""
  ) {
    console.error("❌ Front-End chặn: Không tìm thấy Token hợp lệ!");
    showLocalToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  initPositionsData(token);
  loadStaffsList(token);
  initDropdownFilter(token);

  const staffNameInput = document.getElementById("staffName");
  if (staffNameInput) {
    staffNameInput.addEventListener("input", function () {
      document.getElementById("profileStaffName").innerText =
        this.value.trim() || "Họ và Tên";
    });
  }

  const inputStaffAvatar = document.getElementById("inputStaffAvatar");
  if (inputStaffAvatar) {
    inputStaffAvatar.addEventListener("change", async function (event) {
      const file = event.target.files[0];
      if (!file) return;

      if (!file.type.startsWith("image/")) {
        showLocalToast("Vui lòng chọn tệp hình ảnh hợp lệ!", "error");
        this.value = "";
        return;
      }

      const reader = new FileReader();
      reader.onload = function (e) {
        document.getElementById("staffAvatarPreview").src = e.target.result;
      };
      reader.readAsDataURL(file);

      await uploadStaffAvatarFile(file, token);
    });
  }

  const staffModal = document.getElementById("staffModal");
  const btnOpenAddModal = document.getElementById("btnOpenAddModal");
  const btnCloseModal = document.getElementById("btnCloseModal");
  const btnCancelModal = document.getElementById("btnCancelModal");
  const formStaff = document.getElementById("formStaff");

  if (btnOpenAddModal) {
    btnOpenAddModal.addEventListener("click", () => {
      formStaff.reset();
      uploadedAvatarPath = null;
      document.getElementById("staffId").value = "";
      document.getElementById("staffCodeGroup").style.display = "none";
      document.getElementById("profileStaffName").innerText = "Họ và Tên";
      document.getElementById("profileStaffCode").innerText = "Mã NV: Mới";
      document.getElementById("staffAvatarPreview").src = DEFAULT_SVG_AVATAR;
      document.getElementById("modalTitle").innerHTML =
        "👤 Tuyển Dụng Nhân Viên Mới";
      staffModal.style.display = "flex";
    });
  }

  const closeModalFunc = () => {
    staffModal.style.display = "none";
  };
  if (btnCloseModal) btnCloseModal.addEventListener("click", closeModalFunc);
  if (btnCancelModal) btnCancelModal.addEventListener("click", closeModalFunc);

  if (formStaff) {
    formStaff.addEventListener("submit", function (event) {
      submitStaffForm(event, token);
    });
  }
});

/**
 * 🌟 HÀM TOÀN CỤC: Đón nhận từ khóa từ ô tìm kiếm chung của Dashboard truyền xuống
 */
window.handleGlobalSearch = function (keyword) {
  SEARCH_STAFF_KEYWORD = keyword.trim().toLowerCase();
  filterAndRenderStaff(localStorage.getItem("petcare_token"));
};

/**
 * 🔄 Hàm xử lý: Lọc mảng dữ liệu sống dựa theo từ khóa nhận được từ Dashboard cha
 */
function filterAndRenderStaff(token) {
  const filteredStaffs = ALL_STAFFS_DATA.filter((st) => {
    const textTarget =
      `${st.staffCode} ${st.fullName} ${st.phone || ""}`.toLowerCase();
    return textTarget.includes(SEARCH_STAFF_KEYWORD);
  });
  renderStaffTable(filteredStaffs, token);
}

/**
 * 📊 Gọi API lấy dữ liệu thực đút vào 4 ô Widgets tổng quan
 */
async function loadAttendanceWidgetsData(token) {
  try {
    const response = await fetch(
      `${BASE_URL}/attendance/admin/widgets-summary`,
      {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      },
    );

    if (response.ok) {
      const summary = await response.json();

      if (document.getElementById("widgetTotalStaff")) {
        document.getElementById("widgetTotalStaff").innerText =
          summary.totalStaff;
      }
      if (document.getElementById("widgetPresent")) {
        document.getElementById("widgetPresent").innerText =
          summary.presentToday;
      }
      if (document.getElementById("widgetLateMonth")) {
        document.getElementById("widgetLateMonth").innerText =
          summary.lateThisMonth;
      }
      if (document.getElementById("widgetAbsentToday")) {
        document.getElementById("widgetAbsentToday").innerText =
          summary.absentToday;
      }
    }
  } catch (error) {
    console.error("🚨 Lỗi lấy dữ liệu Widgets chấm công:", error);
  }
}

async function uploadStaffAvatarFile(file, token) {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch(`${BASE_URL}/upload/avatar`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    });

    if (response.ok) {
      uploadedAvatarPath = await response.text();
      document.getElementById("staffAvatarPreview").src =
        getImageUrl(uploadedAvatarPath);
      showLocalToast("Ảnh đại diện đã được tải lên máy chủ!", "success");
    } else {
      showLocalToast("Gặp lỗi khi xử lý tải ảnh!", "error");
    }
  } catch (error) {
    console.error(error);
  }
}

async function initPositionsData(token) {
  try {
    const response = await fetch(`${BASE_URL}/positions/all`, {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      listPositionsGlobal = await response.json();

      const selectRole = document.getElementById("staffRole");
      if (selectRole) {
        selectRole.innerHTML = listPositionsGlobal
          .map(
            (p) =>
              `<option value="${p.positionId}">${p.positionName} (${p.positionCode})</option>`,
          )
          .join("");
      }

      const dropdownListOptions = document.getElementById(
        "dropdownListOptions",
      );
      if (dropdownListOptions) {
        let htmlFilter = `<li data-value="ALL" class="active">Tất cả chức vụ</li>`;
        htmlFilter += listPositionsGlobal
          .map((p) => `<li data-value="${p.positionId}">${p.positionName}</li>`)
          .join("");
        dropdownListOptions.innerHTML = htmlFilter;
        bindDropdownItemsEvent(token);
      }
    }
  } catch (error) {
    console.error(error);
  }
}

/**
 * 🔄 Tải song song cả danh sách nhân viên lẫn trạng thái chấm công chi tiết
 */
async function loadStaffsList(token) {
  try {
    const [staffsRes, attendanceRes] = await Promise.all([
      fetch(`${BASE_URL}/staffs`, {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      }),
      fetch(`${BASE_URL}/attendance/admin/dashboard-status`, {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      }),
    ]);

    if (staffsRes.ok && attendanceRes.ok) {
      ALL_STAFFS_DATA = await staffsRes.json();
      ATTENDANCE_DASHBOARD_DATA = await attendanceRes.json();

      ALL_STAFFS_DATA.sort((a, b) => {
        if (!a.staffCode) return 1;
        if (!b.staffCode) return -1;
        return a.staffCode.localeCompare(b.staffCode, undefined, {
          numeric: true,
          sensitivity: "base",
        });
      });

      // Render dữ liệu ra bảng
      filterAndRenderStaff(token);

      // Đồng bộ cập nhật số liệu lên các ô Widgets
      await loadAttendanceWidgetsData(token);
    }
  } catch (error) {
    console.error("🚨 Lỗi đồng bộ danh sách và chấm công hệ thống:", error);
  }
}

/**
 * 🔄 Khớp nối thông tin nhân viên với dữ liệu chấm công thực tế để render bảng
 */
function renderStaffTable(staffs, token) {
  const tbody = document.getElementById("tableStaffBody");
  if (!tbody) return;

  if (staffs.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="padding: 20px; color: #999;">Không tìm thấy nhân viên nào phù hợp.</td></tr>`;
    return;
  }

  tbody.innerHTML = staffs
    .map((st) => {
      // Tìm kiếm dữ liệu đối chiếu từ mảng ATTENDANCE_DASHBOARD_DATA thông qua staffId
      const attInfo = ATTENDANCE_DASHBOARD_DATA.find(
        (a) => a.staffId === st.staffId,
      ) || {
        todayCheckIn: "--:--",
        todayStatus: "CHUA_CHECKIN",
        totalLateInMonth: 0,
        totalAbsentInMonth: 0,
      };

      // Xác định badge trạng thái Check-in ngày hôm nay
      let badgeHtml = "";
      if (attInfo.todayStatus === "ON_TIME") {
        badgeHtml = `<span class="badge-status on-time" style="background: #e6f7ed; color: #26af61; padding: 4px 8px; border-radius: 4px; border: 1px solid #b7ebc6; font-weight: 600;">Vào: ${attInfo.todayCheckIn} ✅</span>`;
      } else if (attInfo.todayStatus === "LATE") {
        badgeHtml = `<span class="badge-status late" style="background: #fffbe6; color: #faad14; padding: 4px 8px; border-radius: 4px; border: 1px solid #ffe58f; font-weight: 600;">Muộn: ${attInfo.todayCheckIn} ⏱️</span>`;
      } else if (attInfo.todayStatus === "EARLY") {
        badgeHtml = `<span class="badge-status early" style="background: #fff7e6; color: #ff9c6e; padding: 4px 8px; border-radius: 4px; border: 1px solid #ffd591; font-weight: 600;">Về sớm ⚠️</span>`;
      } else if (attInfo.todayStatus === "ABSENT") {
        badgeHtml = `<span class="badge-status absent" style="background: #fff1f0; color: #ff4d4f; padding: 4px 8px; border-radius: 4px; border: 1px solid #ffa39e; font-weight: 600;">Vắng mặt ❌</span>`;
      } else {
        badgeHtml = `<span class="badge-status badge-absent" style="background: #f5f5f5; color: #777; border-color: #ddd; padding: 4px 8px; border-radius: 4px; border: 1px solid #ddd;">Chưa check-in</span>`;
      }

      return `
        <tr>
          <td style="font-weight: bold; color: #c25100">${st.staffCode}</td>
          <td><div style="font-weight: 600;">${st.fullName}</div></td>
          <td><span class="cstm-badge" style="background: #fdf0e6; color: #c25100; padding: 4px 8px; border-radius: 4px;">${st.positionName}</span></td>
          
          <td class="text-center">${badgeHtml}</td>
          
          <td class="text-center">
            <span class="text-late-count" style="font-weight:700; color:#ff4d4f;">${attInfo.totalLateInMonth}</span> muộn / 
            <span class="text-absent-count" style="font-weight:700; color:#777;">${attInfo.totalAbsentInMonth}</span> nghỉ
          </td>
          
          <td>${st.phone || "Chưa có SĐT"}</td>
          <td class="text-center">
            <div class="cstm-action-group">
              <button class="cstm-action-btn btn-history" onclick="openStaffAttendanceHistory(${st.staffId}, '${st.fullName}')" title="Xem lịch sử chấm công">
                <i class="ri-calendar-check-line"></i> Công
              </button>
              <button class="cstm-action-btn btn-edit" onclick="openEditStaffModal(${st.staffId}, '${token}')">
                  <i class="ri-edit-box-line"></i> Sửa
              </button>
              <button class="cstm-action-btn btn-delete" onclick="deleteStaffItem(${st.staffId}, '${st.fullName}', '${token}')">
                  <i class="ri-delete-bin-line"></i> Xóa
              </button>
            </div>
          </td>
        </tr>
      `;
    })
    .join("");
}

/**
 * Điều hướng liên thông sang tab xem công chi tiết của từng nhân sự
 */
function openStaffAttendanceHistory(staffId, fullName) {
  showLocalToast(`Đang mở dữ liệu công của nhân viên: ${fullName}`, "success");

  if (window.parent && typeof window.parent.switchTab === "function") {
    window.parent.location.href = `dashboard.html?tab=attendance&viewStaffId=${staffId}&viewStaffName=${encodeURIComponent(fullName)}`;
  } else {
    window.location.href = `/staff/attendance/Attendance.html?token=${localStorage.getItem("petcare_token")}&viewStaffId=${staffId}`;
  }
}

async function openEditStaffModal(staffId, token) {
  try {
    const response = await fetch(`${BASE_URL}/staffs/${staffId}`, {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      const st = await response.json();

      document.getElementById("staffId").value = st.staffId;
      document.getElementById("staffCode").value = st.staffCode;
      document.getElementById("staffName").value = st.fullName;
      document.getElementById("staffEmail").value = st.email;
      document.getElementById("staffPhone").value = st.phone;
      document.getElementById("staffCccd").value = st.cccd;
      document.getElementById("staffGender").value = st.gender;
      document.getElementById("staffSalary").value = st.salary;
      document.getElementById("staffSalaryBonus").value = st.bonus;
      document.getElementById("staffRole").value = st.positionId;

      document.getElementById("profileStaffName").innerText = st.fullName;
      document.getElementById("profileStaffCode").innerText =
        "Mã NV: " + st.staffCode;

      uploadedAvatarPath = st.avatar;
      document.getElementById("staffAvatarPreview").src = st.avatar
        ? getImageUrl(st.avatar)
        : DEFAULT_SVG_AVATAR;

      document.getElementById("staffCodeGroup").style.display = "block";
      document.getElementById("modalTitle").innerHTML =
        "👤 Cập Nhật Hồ Sơ Nhân Viên";
      document.getElementById("staffModal").style.display = "flex";
    }
  } catch (error) {
    console.error(error);
  }
}

async function deleteStaffItem(staffId, staffName, token) {
  if (
    !confirm(
      `Bạn có chắc chắn muốn xóa nhân viên "${staffName}" ra khỏi hệ thống?`,
    )
  )
    return;

  try {
    const response = await fetch(`${BASE_URL}/staffs/${staffId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      showLocalToast("🎉 Đã xóa nhân viên thành công!", "success");
      loadStaffsList(token);
    } else {
      showLocalToast(
        "Không thể xóa nhân viên này, đang vướng dữ liệu liên quan!",
        "error",
      );
    }
  } catch (error) {
    console.error(error);
  }
}

async function submitStaffForm(event, token) {
  event.preventDefault();

  const staffId = document.getElementById("staffId").value;
  const isUpdate = staffId !== "";
  const email = document.getElementById("staffEmail").value.trim();

  const staffData = {
    fullName: document.getElementById("staffName").value.trim(),
    gender: document.getElementById("staffGender").value,
    email: email,
    cccd: document.getElementById("staffCccd").value.trim(),
    phone: document.getElementById("staffPhone").value.trim(),
    positionId: parseInt(document.getElementById("staffRole").value),
    salary: parseFloat(document.getElementById("staffSalary").value) || 0,
    bonus: parseFloat(document.getElementById("staffSalaryBonus").value) || 0,
    avatar: uploadedAvatarPath,
  };

  if (!isUpdate) {
    const prefixUser = email.split("@")[0];
    staffData.username = prefixUser + Math.floor(100 + Math.random() * 900);
    staffData.password = "Staff@123";
  }

  const url = isUpdate ? `${BASE_URL}/staffs/${staffId}` : `${BASE_URL}/staffs`;
  const method = isUpdate ? "PUT" : "POST";

  try {
    const response = await fetch(url, {
      method: method,
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(staffData),
    });

    if (response.ok) {
      showLocalToast(
        isUpdate ? "🎉 Cập nhật thành công!" : "🎉 Tuyển dụng thành công!",
        "success",
      );
      document.getElementById("staffModal").style.display = "none";
      loadStaffsList(token);
    } else {
      showLocalToast(
        "Giao dịch thất bại, vui lòng check lại dữ liệu trùng lặp!",
        "error",
      );
    }
  } catch (error) {
    console.error(error);
  }
}

function initDropdownFilter(token) {
  const dropdown = document.getElementById("dropdownFilterRole");
  const selectedText = document.getElementById("dropdownSelectedText");
  if (!dropdown || !selectedText) return;
  selectedText.addEventListener("click", (e) => {
    e.stopPropagation();
    dropdown.classList.toggle("open");
  });
  document.addEventListener("click", () => dropdown.classList.remove("open"));
}

function bindDropdownItemsEvent(token) {
  const items = document.querySelectorAll("#dropdownListOptions li");
  const selectedText = document.getElementById("dropdownSelectedText");

  items.forEach((item) => {
    item.addEventListener("click", async function () {
      items.forEach((li) => li.classList.remove("active"));
      this.classList.add("active");
      selectedText.innerHTML = `${this.innerText} <i class="ri-arrow-down-s-line dropdown-icon-arrow"></i>`;

      const posVal = this.getAttribute("data-value");
      let urlSearch = `${BASE_URL}/staffs/search`;
      if (posVal !== "ALL") urlSearch += `?positionId=${posVal}`;

      try {
        const response = await fetch(urlSearch, {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.ok) {
          ALL_STAFFS_DATA = await response.json();

          ALL_STAFFS_DATA.sort((a, b) => {
            if (!a.staffCode) return 1;
            if (!b.staffCode) return -1;
            return a.staffCode.localeCompare(b.staffCode, undefined, {
              numeric: true,
              sensitivity: "base",
            });
          });

          filterAndRenderStaff(token);
        }
      } catch (error) {
        console.error(error);
      }
    });
  });
}

function getImageUrl(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return SERVER_HOST + path;
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
    (type === "success" ? "#28a745" : "#dc3545");
  toast.innerHTML = `<span>${type === "success" ? "🎉" : "❌"}</span> <span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

/**
 * 🌟 7. LẮNG NGHE THÔNG ĐIỆP TÌM KIẾM BẮN XUYÊN IFRAME TỪ DASHBOARD CHA
 */
window.addEventListener("message", function (event) {
  // Kiểm tra nguồn gốc nếu cần thiết, hoặc chỉ lọc khi đúng action SEARCH_STAFF
  if (event.data && event.data.action === "SEARCH_CUSTOMER") {
    // Chuyển hướng từ khóa sang hàm tìm kiếm toàn cục của StaffManagement
    const keyword = event.data.keyword || "";
    if (typeof window.handleGlobalSearch === "function") {
      window.handleGlobalSearch(keyword);
    }
  }
});
