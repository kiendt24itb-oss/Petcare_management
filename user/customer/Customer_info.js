/**
 * 👤 FILE: Customer_info.js
 * Xử lý thông tin cá nhân khách hàng (Bản chuẩn hóa bảo mật & Lưu cấu trúc lồng Entity)
 */

// 🌐 Cấu hình địa chỉ Server tập trung - Sửa 1 nơi, áp dụng toàn bộ file
const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// Bộ nhớ tạm lưu trữ cấu trúc gốc từ Back-end để bảo toàn dữ liệu khi cập nhật
let currentCustomerRawId = null;
let originalAccountData = null;
let uploadedAvatarPath = null; // Lưu đường dẫn tương đối (Ví dụ: /uploads/avatar1.jpg)

document.addEventListener("DOMContentLoaded", function () {
  // 🔑 Nhặt token trực tiếp từ thanh địa chỉ URL (Query Parameter)
  const urlParams = new URLSearchParams(window.location.search);
  let token = urlParams.get("token");

  if (!token) {
    token = localStorage.getItem("petcare_token");
  }
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  // 🧹 DỌN RÁC TOKEN
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
  loadCustomerProfile(token);

  // 📸 XỬ LÝ CHỌN ẢNH ĐẠI DIỆN TRỰC QUAN (PREVIEW & UPLOAD)
  const inputAvatar = document.getElementById("inputAvatar");
  const customerAvatar = document.getElementById("customerAvatar");

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
        customerAvatar.src = e.target.result;
      };
      reader.readAsDataURL(file);

      // 🔥 ĐẨY FILE LÊN SERVER
      await uploadAvatar(file, token);

      // 🔄 ĐÃ SỬA: Giải phóng value của ô input để nếu người dùng có bấm chọn lại đúng file ảnh này lần nữa thì sự kiện 'change' vẫn được kích hoạt mượt mà
      inputAvatar.value = "";
    });
  }

  // 💾 LẮNG NGHE SỰ KIỆN LƯU THAY ĐỔI
  const formUpdate = document.getElementById("formUpdateCustomer");
  if (formUpdate) {
    formUpdate.addEventListener("submit", function (event) {
      submitUpdateCustomer(event, token);
    });
  }

  // 🔄 LẮNG NGHE SỰ KIỆN BẤM NÚT "ĐẶT LẠI" (RESET FORM)
  const btnReset = document.getElementById("btnReset");
  if (btnReset) {
    btnReset.addEventListener("click", function () {
      loadCustomerProfile(token);
      showLocalToast("Đã khôi phục lại dữ liệu ban đầu!", "success");
    });
  }
});

/**
 * 📥 Hàm gọi API lấy thông tin Profile Khách Hàng đổ vào các ô dữ liệu
 */
/**
 * 📥 Hàm gọi API lấy thông tin Profile Khách Hàng đổ vào các ô dữ liệu
 */
async function loadCustomerProfile(token) {
  try {
    const response = await fetch(`${BASE_URL}/customers/profile`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const customer = await response.json();
      console.log("📊 Cấu trúc dữ liệu thực tế nhận từ Server:", customer);

      // 💾 KÉT SẮT LƯU TRỮ THÔ
      currentCustomerRawId = customer.id || customer.customerId || null;
      originalAccountData = customer.account || null;

      // 🔥 Tham khảo Staff: Khóa và giữ lại path cũ từ DB lên biến tạm ngay từ đầu để tránh rỗng DB khi user bấm update luôn
      if (customer.avatar) {
        uploadedAvatarPath = customer.avatar;
        document.getElementById("customerAvatar").src = getImageUrl(
          customer.avatar,
        );
      } else {
        document.getElementById("customerAvatar").src = getImageUrl(null);
      }

      // 🎯 Đổ dữ liệu vào các ô Input HTML
      document.getElementById("customerId").value = customer.customerCode || "";
      document.getElementById("fullName").value = customer.fullName || "";

      const profileNameEl = document.getElementById("profileName");
      if (profileNameEl) {
        profileNameEl.innerText = customer.fullName || "Khách hàng";
      }

      document.getElementById("age").value = customer.age || "";
      document.getElementById("phone").value = customer.phone || "";
      document.getElementById("gender").value = customer.gender || "";
      document.getElementById("address").value = customer.address || "";
      document.getElementById("email").value = customer.email || "";

      // 🌟 ĐỒNG BỘ HOÁ TÊN (FULLNAME) VÀO LOCALSTORAGE & DASHBOARD CHA NGAY KHI VÀO TRANG
      if (customer.fullName && customer.fullName.trim() !== "") {
        localStorage.setItem("petcare_username", customer.fullName);
        if (window.parent && window.parent.document) {
          const parentNameEl =
            window.parent.document.getElementById("userProfileName");
          if (parentNameEl) parentNameEl.innerText = customer.fullName;
        }
      }
    } else {
      showLocalToast(
        `Lỗi lấy thông tin from Server: ${response.status}`,
        "error",
      );
    }
  } catch (error) {
    console.error("Lỗi kết nối endpoint profile:", error);
    showLocalToast("Không thể kết nối đến máy chủ dữ liệu!", "error");
  }
}

/**
 * 📤 Hàm xử lý đóng gói dữ liệu bắn API PUT gửi lên Back-End để lưu lại
 */
async function submitUpdateCustomer(event, token) {
  event.preventDefault();

  const updateData = {
    fullName: document.getElementById("fullName").value.trim(),
    age: parseInt(document.getElementById("age").value) || null,
    gender: document.getElementById("gender").value || null,
    email: document.getElementById("email").value.trim(),
    phone: document.getElementById("phone").value.trim(),
    address: document.getElementById("address").value.trim(),
    avatar: uploadedAvatarPath || null,
  };

  try {
    console.log("===== ĐANG GỬI UPDATE CHUẨN DTO =====", updateData);
    const response = await fetch(`${BASE_URL}/customers/profile`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(updateData),
    });

    const resultText = await response.text();

    if (response.ok) {
      showLocalToast("🎉 Lưu thay đổi thông tin thành công!", "success");

      const resData = JSON.parse(resultText);
      document.getElementById("profileName").innerText = resData.fullName;

      if (window.parent) {
        localStorage.setItem("petcare_username", resData.fullName);
        const parentNameEl =
          window.parent.document.getElementById("userProfileName");
        if (parentNameEl) parentNameEl.innerText = resData.fullName;
      }

      loadCustomerProfile(token);
    } else {
      try {
        const errorObj = JSON.parse(resultText);
        showLocalToast(errorObj.message || "Cập nhật thất bại!", "error");
      } catch (e) {
        showLocalToast(
          `Thất bại: Email đã tồn tại hoặc dữ liệu không hợp lệ!`,
          "error",
        );
      }
    }
  } catch (error) {
    console.error("Lỗi cập nhật profile:", error);
    showLocalToast("Mất kết nối đường truyền tới máy chủ!", "error");
  }
}

/**
 * 🍿 Hàm bắn Toast thông báo
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

/**
 * 📸 Hàm upload avatar lên Server
 * 🔥 ĐÃ SỬA CHUẨN HÓA: Đóng gói FormData độc lập và bắt khối text trả về từ catch của Back-End
 */
async function uploadAvatar(file, token) {
  const formData = new FormData();
  formData.append("file", file); // Key trùng khớp 100% với @RequestParam("file") bên Java

  try {
    console.log(
      "🚀 Chuẩn bị gửi File:",
      file.name,
      "Dung lượng:",
      file.size,
      "bytes",
    );

    const response = await fetch(`${BASE_URL}/upload/avatar`, {
      method: "POST",
      headers: {
        // Tuyệt đối không thêm Content-Type thủ công ở đây để tránh làm mất boundary của FormData sạch
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (response.ok) {
      uploadedAvatarPath = await response.text();
      console.log(
        "📸 Upload thành công, đường dẫn tương đối lưu vào bộ nhớ tạm:",
        uploadedAvatarPath,
      );

      const fullUrl = getImageUrl(uploadedAvatarPath);

      // 1. Cập nhật UI hiển thị ảnh trên trang cá nhân hiện tại
      document.getElementById("customerAvatar").src = fullUrl;

      // 2. Đồng bộ bắn thẳng sang hiển thị trên ảnh đại diện nhỏ ngoài Dashboard cha ngay lập tức giống Staff
      if (window.parent) {
        const parentAvatar =
          window.parent.document.getElementById("userAvatar");
        if (parentAvatar) {
          parentAvatar.src = fullUrl;
        }
      }
      showLocalToast("Tải ảnh đại diện lên thành công!", "success");
    } else {
      // Bốc xuất log lỗi text mà khối catch của Java ném ra ngoài để theo dõi
      const backendErrorMsg = await response.text();
      console.error("❌ Back-End phản hồi lỗi thực tế:", backendErrorMsg);
      showLocalToast(`Upload ảnh thất bại: ${backendErrorMsg}`, "error");
    }
  } catch (error) {
    console.error("Lỗi upload avatar:", error);
    showLocalToast("Không thể upload ảnh lên máy chủ vị trí!", "error");
  }
}

/**
 * 🛠️ Hàm chuẩn hóa đường dẫn ảnh thông minh
 */
function getImageUrl(path) {
  if (!path || path.trim() === "") {
    return "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0b3AtY29sb3I9IiNmZjlhOWUiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZWNmZWYiLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI1MCIgZmlsbD0idXJsKCNnKSIvPldjdXN0b21lcjxjaXJjbGUgY3g9IjUwIiBjeT0iNDIiIHI9IjE4IiBmaWxsPSIjZmZmIi8+PHBhdGggZD0iTTI1LDc1IEMyNSw1OCA3NSw1OCA3NSw3NSIgZmlsbD0iI2ZmZiIvPjwvc3ZnPg==";
  }
  if (
    path.startsWith("http://") ||
    path.startsWith("https://") ||
    path.startsWith("data:image/")
  ) {
    return path;
  }
  return SERVER_HOST + path;
}
