/**
 * ⚙️ FILE: CustomerManagement.js
 */

const SERVER_HOST = "http://localhost:8080";
const API_BASE_URL = `${SERVER_HOST}/api/customers`;
const API_ALL_PETS_URL = `${SERVER_HOST}/api/pets/all-list`;
let customerDataLocal = [];
let allPetsLocal = [];

const getAuthHeaders = () => {
  let token = localStorage.getItem("petcare_token");
  if (!token || token === "null" || token === "undefined") {
    token = "";
  }
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
};

document.addEventListener("DOMContentLoaded", () => {
  fetchCustomerAndPetData();
  initDropdownFilter();
  initModalEvents();
  listenFromDashboardParent();
});

async function fetchCustomerAndPetData() {
  try {
    const resCust = await fetch(API_BASE_URL, {
      method: "GET",
      headers: getAuthHeaders(),
    });
    if (!resCust.ok) throw new Error("Không thể tải danh sách khách hàng!");
    customerDataLocal = await resCust.json();

    try {
      const resPets = await fetch(API_ALL_PETS_URL, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      if (resPets.ok) allPetsLocal = await resPets.json();
    } catch (e) {
      console.warn("⚠️ Không thể tải trước danh sách pets:", e);
    }

    renderTable(customerDataLocal);
  } catch (error) {
    showToast(`Lỗi: ${error.message}`, false);
  }
}

function renderTable(dataList) {
  const tbody = document.getElementById("tableCustomerBody");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!dataList || dataList.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center" style="color: var(--cstm-text-muted); padding: 30px;">📭 Không tìm thấy khách hàng nào.</td></tr>`;
    return;
  }

  dataList.forEach((cust) => {
    const approvalMode = cust.approvalMode;
    let statusBadge = "";
    if (approvalMode === "LOCKED") {
      statusBadge = `<span class="pill-status status-banned">🔒 Đã khóa TK</span>`;
    } else if (approvalMode === "MANUAL") {
      statusBadge = `<span class="pill-status status-manual">⚠️ Chờ Duyệt Lịch</span>`;
    } else {
      statusBadge = `<span class="pill-status status-auto">⚡ Tự Động Duyệt</span>`;
    }

    const matchedPets = allPetsLocal.filter(
      (p) => Number(p.customerId) === Number(cust.customerId),
    );
    let petBadges =
      matchedPets.length > 0
        ? matchedPets
            .map(
              (p) =>
                `<span style="background:#fff3eb; color:#ff6b00; padding:2px 8px; border-radius:8px; font-size:12px; border:1px solid #ffd6bd; font-weight:700; display:inline-block;">🐾 ${p.petName || p.name || "Bé cưng"}</span>`,
            )
            .join("")
        : `<span style="color:var(--cstm-text-muted); font-size:12px; font-style:italic;">Chưa đăng ký bé cưng</span>`;

    const tr = document.createElement("tr");
    tr.innerHTML = `
            <td><span class="cstm-customer-badge">${cust.customerCode || "N/A"}</span></td>
            <td><div style="font-weight: 700; color: var(--cstm-text-dark);">${cust.fullName || "Chưa cập nhật"}</div></td>
            <td><span style="font-size:13px; font-weight:500;">${cust.email}</span></td>
            <td>${cust.phone || "---"}</td>
            <td><div style="display:flex; flex-wrap:wrap; gap:4px;">${petBadges}</div></td>
            <td class="text-center" style="font-weight: 800; color: ${cust.violationCount >= 3 ? "var(--cstm-status-banned)" : "inherit"}">${cust.violationCount || 0}</td>
            <td>${statusBadge}</td>
            <td class="text-center">
                <div class="cstm-action-group">
                    <button class="cstm-action-btn btn-view" id="btn-view-${cust.customerId}">
                        <i class="ri-eye-line"></i> Xem
                    </button>
                </div>
            </td>
        `;
    tbody.appendChild(tr);

    tr.querySelector(`#btn-view-${cust.customerId}`).addEventListener(
      "click",
      () => openViewModal(cust),
    );
  });
}

window.openViewModal = function (cust) {
  const modal = document.getElementById("customerModal");
  if (!modal) return;

  document.getElementById("customerId").value = cust.customerId || "";
  document.getElementById("customerName").value =
    cust.fullName || "Chưa cập nhật";
  document.getElementById("customerEmail").value = cust.email || "";
  document.getElementById("customerPhone").value = cust.phone || "---";
  document.getElementById("customerAddress").value =
    cust.address || "Chưa khai báo";
  document.getElementById("customerViolations").value =
    cust.violationCount || 0;

  // Lưu trạng thái cũ vào thuộc tính data để so sánh lúc submit
  modal.setAttribute("data-old-status", cust.approvalMode);

  const statusSelect = document.getElementById("accountStatus");
  if (statusSelect) {
    statusSelect.value = cust.approvalMode === "LOCKED" ? "LOCKED" : "ACTIVE";
  }

  modal.style.display = "flex";
};

function initModalEvents() {
  const modal = document.getElementById("customerModal");
  const form = document.getElementById("formCustomer");

  if (!modal || !form) return;

  const closeModal = () => {
    modal.style.display = "none";
  };
  document
    .getElementById("btnCloseModal")
    ?.addEventListener("click", closeModal);
  document
    .getElementById("btnCancelModal")
    ?.addEventListener("click", closeModal);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const custId = document.getElementById("customerId").value;
    const selectedStatus =
      document.getElementById("accountStatus")?.value || "ACTIVE";
    const oldStatus = modal.getAttribute("data-old-status");

    // Trường hợp 1: Admin muốn MỞ KHÓA (Từ LOCKED chuyển sang ACTIVE)
    if (selectedStatus === "ACTIVE" && oldStatus === "LOCKED") {
      try {
        const response = await fetch(
          `${SERVER_HOST}/api/bookings/admin/unlock-account/${custId}`,
          {
            method: "PUT",
            headers: getAuthHeaders(),
          },
        );
        if (!response.ok) throw new Error("Mở khóa thất bại!");
        showToast("🔓 Mở khóa thành công! Vi phạm lùi về mốc 3.", true);
        closeModal();
        fetchCustomerAndPetData();
      } catch (error) {
        showToast(`❌ Thất bại: ${error.message}`, false);
      }
    }
    // Trường hợp 2: Admin ghét quá muốn CHỦ ĐỘNG KHÓA (Từ đang bình thường ACTIVE chuyển sang LOCKED)
    else if (selectedStatus === "LOCKED" && oldStatus !== "LOCKED") {
      try {
        const response = await fetch(
          `${SERVER_HOST}/api/bookings/admin/lock-account/${custId}`,
          {
            method: "PUT",
            headers: getAuthHeaders(),
          },
        );
        if (!response.ok) throw new Error("Chủ động khóa tài khoản thất bại!");
        showToast("🔒 Đã khóa tài khoản thành công theo yêu cầu Admin!", true);
        closeModal();
        fetchCustomerAndPetData();
      } catch (error) {
        showToast(`❌ Thất bại: ${error.message}`, false);
      }
    }
    // Không có gì thay đổi
    else {
      showToast("⚠️ Trạng thái tài khoản được giữ nguyên.", true);
      closeModal();
    }
  });
}

// 🌟 FIX LỖI TOAST CHỮ TRẮNG: Sử dụng class success/error đồng bộ với CSS của ní
function showToast(message, isSuccess = true) {
  let container = document.getElementById("pet-toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "pet-toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  // Thêm class .success hoặc .error để ăn màu chuẩn trong file CSS của ông
  toast.className = `pet-toast ${isSuccess ? "success" : "error"}`;
  toast.innerHTML = `<i class="${isSuccess ? "ri-checkbox-circle-line" : "ri-error-warning-line"}"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3000);
}

function initDropdownFilter() {
  const dropdown = document.getElementById("dropdownFilterRank");
  const selectedText = document.getElementById("dropdownSelectedText");
  const options = document.querySelectorAll("#dropdownListOptions li");
  if (!dropdown || !selectedText) return;

  dropdown.addEventListener("click", (e) => {
    e.stopPropagation();
    dropdown.classList.toggle("open");
  });
  document.addEventListener("click", () => dropdown.classList.remove("open"));

  options.forEach((opt) => {
    opt.addEventListener("click", function () {
      options.forEach((li) => li.classList.remove("active"));
      this.classList.add("active");
      const filterValue = this.getAttribute("data-value");
      selectedText.innerHTML = `${this.textContent} <i class="ri-arrow-down-s-line dropdown-icon-arrow"></i>`;

      if (filterValue === "ALL") {
        renderTable(customerDataLocal);
      } else {
        const filtered = customerDataLocal.filter(
          (c) => c.approvalMode === filterValue,
        );
        renderTable(filtered);
      }
    });
  });
}

function listenFromDashboardParent() {
  window.addEventListener("message", async (event) => {
    if (event.data && event.data.action === "SEARCH_CUSTOMER") {
      const keyword = event.data.keyword.trim();

      // 🔄 Nếu Admin xóa trắng ô search, tự động nạp lại bảng dữ liệu ban đầu
      if (keyword === "") {
        renderTable(customerDataLocal);
        return;
      }

      try {
        const response = await fetch(
          `${API_BASE_URL}/search?keyword=${encodeURIComponent(keyword)}`,
          { method: "GET", headers: getAuthHeaders() },
        );
        if (!response.ok) throw new Error("Tìm kiếm thất bại");
        renderTable(await response.json());
      } catch (error) {
        console.error("Lỗi tìm kiếm khách hàng qua Iframe:", error);
      }
    }
  });
}
