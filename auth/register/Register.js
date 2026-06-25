// Thư mục gốc API của Back-End (Tự động nhận diện nếu chưa định nghĩa)
if (typeof BASE_URL === "undefined") {
  var BASE_URL = "http://localhost:8080/api";
}

// Biến cờ chặn bấm liên tục sinh ra nhiều Toast trùng lặp
if (typeof isRegisterSubmitting === "undefined") {
  var isRegisterSubmitting = false;
}

/**
 * 🛠️ Hàm tạo Toast thông báo trượt góc màn hình (Bản fix không trùng lặp)
 */
if (typeof showPetToast === "undefined") {
  function showPetToast(message, type = "success") {
    const container = document.getElementById("pet-toast-container");
    if (!container) return;

    // Kiểm tra xem thông báo này đã hiển thị trên màn hình chưa, tránh hiện lặp lại
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

/**
 * 👁️ Ẩn/Hiện mật khẩu
 */
function togglePwd(inputId) {
  const passwordInput = document.getElementById(inputId);
  if (!passwordInput) return;
  passwordInput.type = passwordInput.type === "password" ? "text" : "password";
}

/**
 * ✕ Đóng Modal khi bấm ra rìa ngoài overlay
 */
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
 * 🔥 Xử lý Đăng Ký kết nối trực tiếp đến Spring Boot AccountService
 */
async function submitRegister(event) {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }

  // Nếu đang trong quá trình gửi dữ liệu lên BE thì chặn đứng request tiếp theo
  if (isRegisterSubmitting) return;

  const usernameElement = document.getElementById("regUsername");
  const emailElement = document.getElementById("regEmail");
  const passwordElement = document.getElementById("regPwd");
  const confirmPasswordElement = document.getElementById("confirmPwd");

  // Kiểm tra xem các thẻ Input có tồn tại trên giao diện DOM không
  if (!usernameElement || !emailElement || !passwordElement || !confirmPasswordElement) {
    return;
  }

  const username = usernameElement.value.trim();
  const email = emailElement.value.trim();
  const password = passwordElement.value;
  const confirmPassword = confirmPasswordElement.value;

  // --- VALIDATION TẦNG FRONT-END ---
  if (!username || !email || !password || !confirmPassword) {
    showPetToast("Vui lòng điền đầy đủ các thông tin bắt buộc!", "error");
    return;
  }

  if (password.length < 6) {
    showPetToast("Mật khẩu phải có độ dài tối thiểu từ 6 ký tự!", "error");
    return;
  }

  if (password !== confirmPassword) {
    showPetToast("Mật khẩu nhập lại không trùng khớp, kiểm tra lại nha khứa!", "error");
    return;
  }

  // Khối dữ liệu map chuẩn 100% với RegisterRequest DTO bên Backend
  const registerData = {
    username: username,
    email: email,
    password: password
  };

  try {
    isRegisterSubmitting = true; // Khóa form lại để chống spam click

    // Thực hiện bắn request sang endpoint permitAll() của SecurityConfig
    const response = await fetch(`${BASE_URL}/accounts/register`, {
      method: "POST",
      headers: { 
        "Content-Type": "application/json" 
      },
      body: JSON.stringify(registerData),
    });

    const resultText = await response.text(); // Backend quăng về String thô dạng "Đăng ký tài khoản thành công!" hoặc lỗi gõ chữ

    if (response.ok) {
      // Thành công: resultText = "Đăng ký tài khoản thành công!"
      showPetToast(resultText || "Tạo tài khoản thành công! Đang chuyển sang đăng nhập...", "success");
      
      // Chờ hiệu ứng toast chạy xong thì đá sang form Đăng nhập
      setTimeout(() => {
        if (typeof loadAuthComponent === "function") {
          loadAuthComponent("login");
        }
      }, 1500);

    } else {
      // Thất bại: Xử lý ngoại lệ ném ra từ throw new RuntimeException bên Backend
      try {
        // Dự phòng nếu sau này ní đổi Backend trả về JSON Object lỗi
        const errorObj = JSON.parse(resultText);
        showPetToast(errorObj.message || "Tên tài khoản hoặc Email đã tồn tại!", "error");
      } catch (e) {
        // Đọc thông báo trực tiếp từ chữ thô: "Tên đăng nhập này có người xài rồi khứa ơi!" hoặc "Email này đã được đăng ký hệ thống rồi!"
        showPetToast(resultText || "Đăng ký thất bại, dữ liệu không hợp lệ!", "error");
      }
    }
  } catch (error) {
    console.error("Lỗi kết nối API:", error);
    showPetToast("Không thể kết nối đến máy chủ PetCare (Mất mạng hoặc BE sập)!", "error");
  } finally {
    isRegisterSubmitting = false; // Mở khóa form cho người dùng thao tác lại nếu dính lỗi
  }
}