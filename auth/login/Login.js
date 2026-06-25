// Thư mục gốc API của Back-End
if (typeof BASE_URL === "undefined") {
  var BASE_URL = "http://localhost:8080/api";
}

// Biến cờ chặn bấm liên tục ở Form Login
if (typeof isLoginSubmitting === "undefined") {
  var isLoginSubmitting = false;
}

/**
 * 🛠️ Hàm tạo Toast thông báo trượt góc phải màn hình (Bản fix không trùng lặp)
 */
if (typeof showPetToast === "undefined") {
  function showPetToast(message, type = "success") {
    const container = document.getElementById("pet-toast-container");
    if (!container) return;

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
}

function togglePwd(inputId) {
  const passwordInput = document.getElementById(inputId);
  if (!passwordInput) return;
  passwordInput.type = passwordInput.type === "password" ? "text" : "password";
}

function closeAuthModal(event) {
  const overlay = document.getElementById("authOverlay");
  if (event.target === overlay) {
    if (typeof removeAuthModal === "function") {
      removeAuthModal();
    } else {
      const container = document.getElementById("authModalContainer");
      if (container) container.innerHTML = "";
    }
  }
}

/**
 * 🔐 Xử lý Đăng Nhập khi Submit Form kết nối API Spring Boot
 */
/**
 * 🔐 Xử lý Đăng Nhập khi Submit Form kết nối API Spring Boot
 */
async function submitLogin(event) {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }

  if (isLoginSubmitting) return;

  const usernameInput = document.getElementById("loginUsername");
  const pwdInput = document.getElementById("loginPwd");

  if (!usernameInput || !pwdInput) return;

  const username = usernameInput.value.trim();
  const password = pwdInput.value;

  if (!username || !password) {
    showPetToast("Vui lòng nhập tài khoản và mật khẩu!", "error");
    return;
  }

  const loginData = { username: username, password: password };

  try {
    isLoginSubmitting = true;

    const response = await fetch(`${BASE_URL}/accounts/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(loginData),
    });

    const resultText = await response.text();

    if (response.ok) {
      const data = JSON.parse(resultText);

      // Lưu thông tin đăng nhập vào localStorage bằng bộ key petcare_role
      localStorage.setItem("petcare_token", data.token);
      localStorage.setItem("petcare_role", data.role);
      localStorage.setItem("petcare_username", data.username);
      localStorage.setItem("petcare_accountId", data.accountId);
      localStorage.setItem("petcare_email", data.email);

      showPetToast(
        `Đăng nhập thành công! Đang chuyển hướng vào Dashboard...`,
        "success",
      );

      // Tắt modal popup ngay lập tức
      const container = document.getElementById("authModalContainer");
      if (container) container.innerHTML = "";

      // Ép thanh Menu Header soi lại trạng thái (nếu cần trước khi chuyển trang)
      if (typeof checkLoginStatus === "function") {
        checkLoginStatus();
      }

      // 🔴 ĐIỀU HƯỚNG TOÀN TRANG: Rời khỏi trang chủ để sang trang Dashboard riêng biệt
      setTimeout(() => {
        // Đi từ vị trí trang chủ lùi vào thư mục dashboard/Dashboard.html
        window.location.assign("dashboard/Dashboard.html");
      }, 1000);
    } else {
      try {
        const errorObj = JSON.parse(resultText);
        showPetToast(
          errorObj.message || "Tài khoản hoặc mật khẩu không đúng!",
          "error",
        );
      } catch (e) {
        showPetToast(
          resultText || "Tài khoản hoặc mật khẩu không đúng!",
          "error",
        );
      }
    }
  } catch (error) {
    console.error("Lỗi kết nối API:", error);
    showPetToast("Không thể kết nối tới máy chủ Back-End!", "error");
  } finally {
    isLoginSubmitting = false;
  }
}
