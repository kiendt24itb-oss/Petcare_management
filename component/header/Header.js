/**
 * =========================================================================
 * THIẾT LẬP BAN ĐẦU: NẠP HEADER VÀO TRANG CHÍNH INDEX
 * =========================================================================
 */
document.addEventListener("DOMContentLoaded", function () {
  const headerPlaceholder = document.getElementById("header-placeholder");

  if (headerPlaceholder) {
    // Tải giao diện Header vào trục chính index.html
    fetch("component/header/Header.html")
      .then((response) => {
        if (!response.ok) throw new Error("Không thể tải file Header.html");
        return response.text();
      })
      .then((headerHtml) => {
        headerPlaceholder.innerHTML = headerHtml;

        // Sau khi nạp Header xong xuôi, kích hoạt kiểm tra đăng nhập và menu
        checkLoginStatus();
        initNavigation();
      })
      .catch((err) => console.error("Lỗi nạp hệ thống giao diện Header:", err));
  }
});

/**
 * =========================================================================
 * QUẢN LÝ TRẠNG THÁI ĐĂNG NHẬP & MODAL XÁC THỰC
 * =========================================================================
 */
function checkLoginStatus() {
  const authGroup = document.getElementById("navAuthGroup");
  const userGroup = document.getElementById("navUserGroup");

  // 🔽 SỬA THEO YÊU CẦU: Kiểm tra sự tồn tại của petcare_token 🔽
  const token = localStorage.getItem("petcare_token");
  const role = localStorage.getItem("petcare_role"); // Đọc quyền ADMIN/STAFF/CUSTOMER

  if (token) {
    if (authGroup) authGroup.style.display = "none";
    if (userGroup) userGroup.style.display = "flex";

    // Mẹo nhỏ: Bạn có thể ẩn/hiện nút Dashboard tùy theo Role tại đây nếu cần
    const btnDashboard = document.querySelector(".btn-dashboard");
    if (btnDashboard && role === "ADMIN") {
      btnDashboard.innerText = "Trang Quản Trị (Admin)";
    } else if (btnDashboard) {
      btnDashboard.innerText = "Vào không gian của bạn";
    }
  } else {
    if (authGroup) authGroup.style.display = "flex";
    if (userGroup) userGroup.style.display = "none";
  }
}

function loadAuthComponent(type) {
  const container = document.getElementById("authModalContainer");
  let fileName =
    type === "login" ? "auth/login/Login.html" : "auth/register/Register.html";

  fetch(fileName)
    .then((response) => {
      if (!response.ok) throw new Error("Không tìm thấy file: " + fileName);
      return response.text();
    })
    .then((htmlContent) => {
      container.innerHTML = htmlContent;
      const oldScript = container.querySelector("script");
      if (oldScript) {
        const existingScript = document.getElementById("loadedAuthScript");
        if (existingScript) existingScript.remove();

        const newScript = document.createElement("script");
        newScript.id = "loadedAuthScript";
        newScript.src = oldScript.getAttribute("src");
        document.body.appendChild(newScript);
      }
    })
    .catch((error) => console.error("Lỗi mở modal xác thực:", error));
}

function closeAuthModal(event) {
  if (event.target.id === "authOverlay") {
    document.getElementById("authModalContainer").innerHTML = "";
  }
}

function handleLogout() {
  // 🔽 SỬA THEO YÊU CẦU: Dọn dẹp sạch sẽ các key petcare_ 🔽
  localStorage.removeItem("petcare_token");
  localStorage.removeItem("petcare_role");
  localStorage.removeItem("petcare_username");
  localStorage.removeItem("petcare_accountId");
  localStorage.removeItem("petcare_email");

  // Xóa thêm các key cũ phòng hờ dẫm chân nhau
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("role");

  alert("Bạn đã đăng xuất khỏi hệ thống PetCare Premium!");
  window.location.reload(); // Reload để làm sạch toàn bộ trạng thái web
}

/**
 * =========================================================================
 * HỆ THỐNG ĐIỀU HƯỚNG SINGLE PAGE (ĐỨNG IM HEADER - CHỈ ĐỔI RUỘT CHÍNH)
 * =========================================================================
 */
function initNavigation() {
  document.body.addEventListener("click", function (event) {
    const targetLink = event.target.closest("[data-page]");
    if (targetLink) {
      event.preventDefault();
      const targetPage = targetLink.getAttribute("data-page");

      const menuLinks = document.querySelectorAll(".nav-links a");
      menuLinks.forEach((item) => {
        if (item.getAttribute("data-page") === targetPage) {
          item.classList.add("active");
        } else {
          item.classList.remove("active");
        }
      });

      // Thực hiện bốc trang con về
      loadPageContent(targetPage);
    }
  });

  // MẶC ĐỊNH: Tự động tải trang chủ mặc định khi mở web
  loadPageContent("Home.html");
}

function loadPageContent(pageName) {
  const mainContent = document.getElementById("main-content");
  const headerPlaceholder = document.getElementById("header-placeholder");
  const footerPlaceholder = document.getElementById("footer-placeholder");

  if (!mainContent) return;

  if (typeof stopAutoPlay === "function") {
    stopAutoPlay();
  }

  // Quyết định đường dẫn file
  let folderPath =
    pageName === "Dashboard.html" ? pageName : "component/" + pageName;

  // XỬ LÝ ẨN/HIỆN GIAO DIỆN CHÍNH
  if (pageName === "Dashboard.html") {
    if (headerPlaceholder) headerPlaceholder.style.display = "none";
    if (footerPlaceholder) footerPlaceholder.style.display = "none";
    mainContent.style.width = "100%"; // Cho dashboard tràn màn hình nếu cần
  } else {
    if (headerPlaceholder) headerPlaceholder.style.display = "block";
    if (footerPlaceholder) footerPlaceholder.style.display = "block";
  }

  fetch(folderPath)
    .then((response) => {
      if (!response.ok) throw new Error("Không thể tải trang: " + folderPath);
      return response.text();
    })
    .then((htmlData) => {
      mainContent.innerHTML = htmlData;
      window.scrollTo(0, 0);

      // Chạy script của trang con (giữ nguyên đoạn code cũ của bạn...)
      const dynamicScripts = mainContent.querySelectorAll("script");
      dynamicScripts.forEach((oldScript) => {
        const newScript = document.createElement("script");
        if (oldScript.src) {
          newScript.src = oldScript.src;
        } else {
          newScript.textContent = oldScript.textContent;
        }
        oldScript.parentNode.replaceChild(newScript, oldScript);
      });
    })
    .catch((error) => {
      console.error("Lỗi điều hướng:", error);
      mainContent.innerHTML = `<div style="padding: 50px; text-align: center; color: red;"><h2>Lỗi nạp nội dung</h2></div>`;
    });
}
