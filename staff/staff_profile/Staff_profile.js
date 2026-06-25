/**
 * 👤 FILE: Staff_profile.js
 * Xử lý thông tin cá nhân nhân viên & bảng lương 3 ô (Bản chuẩn hóa bảo mật & Liên thông Token)
 */

// 🌐 Cấu hình địa chỉ Server tập trung - Sửa một nơi, áp dụng toàn bộ file
const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// Bộ nhớ tạm lưu trữ cấu trúc gốc từ Back-end để bảo toàn dữ liệu khi cập nhật
let currentStaffRawId = null;
let currentPositionId = null;
let uploadedAvatarPath = null;

document.addEventListener("DOMContentLoaded", function () {
  // 🔑 Nhặt token trực tiếp từ thanh địa chỉ URL hoặc Kho lưu trữ localStorage
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");

  if (!token) {
    token = localStorage.getItem("petcare_token");
  }
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  // 🧹 DỌN RÁC & KIỂM TRA TOKEN CHẶN TỪ VÒNG GỬI XE
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

  console.log("🔒 Token thông quan thành công:", token);

  // Tiến hành lấy thông tin nhân viên đổ lên Form
  loadStaffProfile(token);

  // 📸 XỬ LÝ CHỌN ẢNH ĐẠI DIỆN TRỰC QUAN (PREVIEW & UPLOAD)
  const inputAvatar = document.getElementById("inputAvatar");
  const staffAvatar = document.getElementById("staffAvatar");

  if (inputAvatar) {
    inputAvatar.addEventListener("change", async function (event) {
      const file = event.target.files[0];

      if (!file) return;

      if (!file.type.startsWith("image/")) {
        showLocalToast(
          "Vui lòng chọn tệp hình ảnh hợp lệ (png, jpg, webp)!",
          "error",
        );
        inputAvatar.value = "";
        return;
      }

      // Preview ảnh tạm thời bằng Base64 của trình duyệt cho nhanh
      const reader = new FileReader();
      reader.onload = function (e) {
        staffAvatar.src = e.target.result;
      };
      reader.readAsDataURL(file);

      // Upload chính thức lên server lưu trữ
      await uploadAvatar(file, token);
    });
  }

  // 💾 LẮNG NGHE SỰ KIỆN LƯU THAY ĐỔI PROFILE
  const formUpdate = document.getElementById("formUpdateStaff");
  if (formUpdate) {
    formUpdate.addEventListener("submit", function (event) {
      submitUpdateStaff(event, token);
    });
  }

  // 🔄 LẮNG NGHE SỰ KIỆN BẤM NÚT "ĐẶT LẠI" (RESET FORM)
  const btnReset = document.getElementById("btnReset");
  if (btnReset) {
    btnReset.addEventListener("click", function () {
      loadStaffProfile(token);
      showLocalToast("Đã khôi phục lại dữ liệu ban đầu!", "success");
    });
  }
});

/**
 * 📥 Hàm gọi API lấy thông tin Profile Nhân Viên đổ vào các ô dữ liệu
 */
async function loadStaffProfile(token) {
  try {
    // Gọi thẳng cổng /me dành riêng cho tài khoản đang login (Hết lỗi 403 Forbidden)
    const response = await fetch(`${BASE_URL}/staffs/me`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const staff = await response.json(); // Nhận trực tiếp duy nhất 1 cục object của Staff đang đăng nhập
      console.log("📊 Dữ liệu hồ sơ Staff nhận từ Server:", staff);

      // 💾 KÉT SẮT LƯU TRỮ THÔ ID VÀ POSITION PHỤC VỤ CHO UPDATE
      currentStaffRawId = staff.staffId;
      currentPositionId = staff.positionId || null; // Giữ lại ID chức vụ để tránh bị rỗng DB khi update

      document.getElementById("rawStaffId").value = currentStaffRawId;
      document.getElementById("positionId").value = currentPositionId;

      // 🎯 Đổ dữ liệu vào các ô Input HTML của Staff (Mã nhân viên tự gen nhảy chuẩn bài)
      document.getElementById("staffCode").value = staff.staffCode || "";
      document.getElementById("fullName").value = staff.fullName || "";
      document.getElementById("cccd").value = staff.cccd || ""; // Tự động điền số CCCD cũ nếu có
      document.getElementById("phone").value = staff.phone || "";
      document.getElementById("gender").value = staff.gender || "MALE";
      document.getElementById("address").value = staff.address || "";
      document.getElementById("email").value = staff.email || ""; // 🌟 ĐÃ SỬA: Cho phép hiển thị và sửa bình thường

      // 🌟 ĐÃ SỬA: Đổ dữ liệu chức vụ vào ô input disabled hiển thị trên form
      const positionInput = document.getElementById("positionName");
      if (positionInput) {
        positionInput.value =
          staff.positionName || "Chưa cấu hình vị trí (Chờ Admin gán)";
      }

      // Cập nhật Header Profile bên cột trái (Sidebar)
      const profileNameEl = document.getElementById("profileName");
      if (profileNameEl) {
        profileNameEl.innerText = staff.fullName || "Nhân viên";
      }
      const profileSidebarPosEl = document.getElementById("profileSidebarPos");
      if (profileSidebarPosEl) {
        profileSidebarPosEl.innerText =
          staff.positionName || "Chưa cấu hình vị trí";
      }

      // 📊 ĐỔ DỮ LIỆU BẢNG TÍNH LƯƠNG & THƯỞNG 3 Ô GỌN GÀNG (Định dạng tiền tệ VNĐ)
      document.getElementById("sal-base").innerText = formatVND(staff.salary);
      document.getElementById("sal-bonus").innerText = formatVND(staff.bonus);
      document.getElementById("sal-total").innerText = formatVND(
        staff.totalSalary,
      );

      // 📸 HIỂN THỊ ẢNH ĐẠI DIỆN ĐƯỜNG DẪN TƯƠNG ĐỐI TỪ SERVER
      if (staff.avatar) {
        uploadedAvatarPath = staff.avatar;
        document.getElementById("staffAvatar").src = getImageUrl(staff.avatar);
      }

      // 🌟 ĐỒNG BỘ HOÁ TÊN (FULLNAME) VÀO LOCALSTORAGE & DASHBOARD CHA NGAY KHI VÀO TRANG
      if (staff.fullName && staff.fullName.trim() !== "") {
        localStorage.setItem("petcare_username", staff.fullName);
        if (window.parent && window.parent.document) {
          const parentNameEl =
            window.parent.document.getElementById("userProfileName");
          if (parentNameEl) parentNameEl.innerText = staff.fullName;
        }
      }
    } else {
      showLocalToast(`Lỗi lấy thông tin: ${response.status}`, "error");
    }
  } catch (error) {
    console.error("Lỗi kết nối endpoint staff profile:", error);
    showLocalToast("Không thể kết nối đến máy chủ dữ liệu!", "error");
  }
}

/**
 * 📤 Hàm xử lý đóng gói dữ liệu bắn API PUT gửi lên Back-End để cập nhật
 */
async function submitUpdateStaff(event, token) {
  event.preventDefault();

  // Đóng gói đúng tệp Request DTO cấu trúc của StaffRequest ở Back-end
  const updateData = {
    fullName: document.getElementById("fullName").value.trim(),
    gender: document.getElementById("gender").value,
    email: document.getElementById("email").value.trim(), // Cho phép tự cập nhật lại email liên hệ
    cccd: document.getElementById("cccd").value.trim(), // Cho phép tự cập nhật số CCCD cá nhân
    phone: document.getElementById("phone").value.trim(),
    avatar: uploadedAvatarPath || null, // Lưu đường dẫn ảnh tương đối
    address: document.getElementById("address").value.trim(),
    positionId: parseInt(document.getElementById("positionId").value) || null,
    // Vì Staff tự sửa nên lương thưởng gửi lên gì cũng được, BE đã bọc logic tự động khóa giữ nguyên lương cũ
    salary: 0,
    bonus: 0,
    username: "",
    password: "",
  };

  try {
    console.log("===== ĐANG GỬI CẬP NHẬT HỒ SƠ STAFF =====", updateData);

    const response = await fetch(`${BASE_URL}/staffs/${currentStaffRawId}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(updateData),
    });

    const resultText = await response.text();

    if (response.ok) {
      showLocalToast("🎉 Cập nhật hồ sơ cá nhân thành công!", "success");

      // Đồng bộ tên mới lên các Header giao diện
      document.getElementById("profileName").innerText = updateData.fullName;

      if (window.parent) {
        localStorage.setItem("petcare_username", updateData.fullName);
        const parentNameEl =
          window.parent.document.getElementById("userProfileName");
        if (parentNameEl) parentNameEl.innerText = updateData.fullName;
      }

      // Tải lại dữ liệu mới nhất từ DB để đồng bộ hoàn toàn
      loadStaffProfile(token);
    } else {
      try {
        const errorObj = JSON.parse(resultText);
        showLocalToast(errorObj.message || "Cập nhật hồ sơ thất bại!", "error");
      } catch (e) {
        showLocalToast(
          resultText || `Lỗi ${response.status}: Máy chủ từ chối xử lý!`,
          "error",
        );
      }
    }
  } catch (error) {
    console.error("Lỗi cập nhật profile staff:", error);
    showLocalToast("Mất kết nối đường truyền tới máy chủ!", "error");
  }
}

/**
 * 📸 Hàm upload avatar lên Server (Copy nguyên bản chuẩn từ customer của ní)
 */
async function uploadAvatar(file, token) {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch(`${BASE_URL}/upload/avatar`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (response.ok) {
      uploadedAvatarPath = await response.text();
      const fullUrl = getImageUrl(uploadedAvatarPath);

      // Cập nhật ảnh đại diện tức thì trên trang hiện tại
      document.getElementById("staffAvatar").src = fullUrl;

      // Cập nhật lên cả thanh menu lớn của Dashboard cha (nếu đang nằm trong iframe)
      if (window.parent) {
        const parentAvatar =
          window.parent.document.getElementById("userAvatar");
        if (parentAvatar) {
          parentAvatar.src = fullUrl;
        }
      }
      showLocalToast("Tải ảnh đại diện mới thành công!", "success");
    } else {
      showLocalToast("Upload ảnh thất bại!", "error");
    }
  } catch (error) {
    console.error("Lỗi upload avatar:", error);
    showLocalToast("Không thể upload ảnh lên máy chủ vị trí!", "error");
  }
}

/**
 * 🛠️ CÁC HÀM TIỆN ÍCH DÙNG CHUNG (BẢN CHUẨN ĐÃ ĐỒNG BỘ)
 */
function getImageUrl(path) {
  if (!path)
    return "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0b3AtY29sb3I9IiNmZjlhOWUiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZWNmZWYiLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0idXJsKCNnKSIvPldjdXN0b21lcjxjaXJjbGUgY3g9IjUwIiBjeT0iNDIiIHIpxIjE4IiBmaWxsPSIjZmZmIi8+PHBhdGggZD0iTTI1LDc1IEMyNSw1OCA3NSw1OCA3NSw3NSIgZmlsbD0iI2ZmZiIvPjwvc3ZnPg==";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return SERVER_HOST + path;
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
