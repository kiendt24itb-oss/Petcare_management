/**
 * 🐾 FILE: Pet_list.js
 * Quản lý danh sách thú cưng & Tích hợp Modal thông minh
 * (Bản chuẩn hóa đồng bộ 100% theo các Endpoint của Backend Spring Boot)
 */

// 🌐 Cấu hình địa chỉ Server tập trung
const SERVER_HOST = "http://localhost:8080";
const BASE_URL = `${SERVER_HOST}/api`;

// 🎯 Chuỗi ảnh mặc định dạng SVG vẽ khung tròn siêu mượt không lỗi hiển thị
const DEFAULT_PET_SVG =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100%' height='100%' fill='%23fdfaf7'/><path d='M50,35 C40,35 35,42 35,50 C35,60 45,68 50,68 C55,68 65,60 65,50 C65,42 60,35 50,35 Z' fill='%23ff7a45'/><circle cx='43' cy='48' r='3' fill='%23ffffff'/><circle cx='57' cy='48' r='3' fill='%23ffffff'/><path d='M47,56 Q50,59 53,56' stroke='%23ffffff' stroke-width='2' fill='none'/></svg>";

// Bộ nhớ tạm để quản lý trạng thái Modal
let isEditMode = false;
let currentEditingPetId = null;
let uploadedModalAvatarPath = null;
let originalPetData = null;

document.addEventListener("DOMContentLoaded", function () {
  let token = localStorage.getItem("petcare_token");
  if (!token && window.parent && window.parent.localStorage) {
    token = window.parent.localStorage.getItem("petcare_token");
  }

  if (
    !token ||
    token === "null" ||
    token === "undefined" ||
    token.trim() === ""
  ) {
    showPetToast("Phiên làm việc hết hạn. Vui lòng đăng nhập lại!", "error");
    return;
  }

  // Tải danh sách thú cưng ban đầu
  fetchMyPets(token);

  // 📸 Lắng nghe sự kiện chọn ảnh đại diện cho thú cưng
  const inputModalAvatar = document.getElementById("inputModalAvatar");
  if (inputModalAvatar) {
    inputModalAvatar.addEventListener("change", async function (event) {
      const file = event.target.files[0];
      if (!file) return;

      if (!file.type.startsWith("image/")) {
        showPetToast("Vui lòng chọn tệp hình ảnh hợp lệ!", "error");
        return;
      }

      // Đọc và Preview ảnh ngay lập tức bằng Base64 của trình duyệt
      const reader = new FileReader();
      reader.onload = (e) => {
        document.getElementById("modalPetAvatar").src = e.target.result;
      };
      reader.readAsDataURL(file);

      // Bắn tệp lên server lưu trữ (Mở cmt upload controller rồi chạy tẹt ga ní ơi)
      await uploadModalAvatar(file, token);
    });
  }

  // 💾 Lắng nghe sự kiện submit form (Hợp nhất Thêm mới & Cập nhật)
  const formAddPet = document.getElementById("formAddPet");
  if (formAddPet) {
    formAddPet.addEventListener("submit", function (event) {
      event.preventDefault();
      event.stopPropagation();

      if (isEditMode) {
        submitUpdatePet(token);
      } else {
        submitAddPet(token);
      }
    });
  }

  // Khởi tạo các sự kiện đóng/hủy/reset form
  initModalActions(token);
});

/**
 * 📥 Tải danh sách thú cưng của tôi
 * 🎯 ĐÃ VÁ: Sửa từ /pets/my về đúng endpoint gốc /pets
 */
async function fetchMyPets(token) {
  try {
    const response = await fetch(`${BASE_URL}/pets`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (response.ok) {
      const pets = await response.json();
      renderPetGrid(pets, token);
    } else {
      showPetToast("Không thể tải danh sách thú cưng!", "error");
    }
  } catch (error) {
    console.error(error);
    showPetToast("Lỗi kết nối máy chủ danh sách!", "error");
  }
}

/**
 * 🎨 Dựng giao diện danh sách thú cưng ra dạng lưới Card
 */
function renderPetGrid(pets, token) {
  const petGrid = document.querySelector(".pet-grid");
  if (!petGrid) return;

  petGrid.innerHTML = "";

  // Duyệt mảng đổ dữ liệu thú cưng hiện có
  pets.forEach((pet) => {
    const petCard = document.createElement("div");
    petCard.className = "pet-card";

    petCard.addEventListener("click", () => {
      openModalForEdit(pet);
    });

    const avatarUrl = pet.avatar ? getImageUrl(pet.avatar) : DEFAULT_PET_SVG;
    const readableSpecies =
      pet.species === "DOG" ? "Chó" : pet.species === "CAT" ? "Mèo" : "Khác";

    petCard.innerHTML = `
      <div class="avatar-wrapper">
        <img src="${avatarUrl}" alt="${pet.petName || "Thú cưng"}" class="pet-avatar-img" 
             onerror="this.src='${DEFAULT_PET_SVG}'" />
      </div>
      <h3 class="pet-name ${!pet.petName ? "text-muted" : ""}">${pet.petName || "Chưa đặt tên"}</h3>
      <div class="pet-info-row">
        <div class="info-item">
          <span class="info-label">Loài</span>
          <span class="info-val">${readableSpecies}</span>
        </div>
        <div class="info-item">
          <span class="info-label">Mã</span>
          <span class="info-val">${pet.petCode || "---"}</span>
        </div>
      </div>
    `;
    petGrid.appendChild(petCard);
  });

  // Nút bấm cố định ở cuối danh sách: "Thêm Bé Cưng"
  const addCard = document.createElement("div");
  addCard.className = "pet-card add-pet-trigger";
  addCard.id = "openModalBtn";
  addCard.innerHTML = `
    <div class="add-icon-circle"><i class="fa-solid fa-plus"></i></div>
    <span class="add-text">Thêm Bé Cưng</span>
  `;

  addCard.addEventListener("click", () => {
    openModalForAdd(token);
  });

  petGrid.appendChild(addCard);
}

/**
 * 🔓 CHẾ ĐỘ 1: Mở Modal để THÊM MỚI
 */
async function openModalForAdd(token) {
  isEditMode = false;
  currentEditingPetId = null;
  uploadedModalAvatarPath = null;
  originalPetData = null;

  document.querySelector(".modal-header h3").innerText = "Thêm Bé Cưng Mới";

  const btnDelete = document.getElementById("btnModalDelete");
  if (btnDelete) btnDelete.style.display = "none";

  document.getElementById("formAddPet").reset();
  document.getElementById("modalPetAvatar").src = DEFAULT_PET_SVG;

  // TỰ ĐỘNG LẤY MÃ XEM TRƯỚC TỪ ENDPOINT MỚI
  document.getElementById("modalPetCode").value = "Đang lấy mã...";
  try {
    const res = await fetch(`${BASE_URL}/pets/next-code`, {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      const nextCode = await res.text();
      document.getElementById("modalPetCode").value = nextCode;
    } else {
      document.getElementById("modalPetCode").value = "TC-A001";
    }
  } catch (err) {
    document.getElementById("modalPetCode").value = "TC-A001";
  }

  document.getElementById("petModal").classList.add("active");
}

/**
 * ✏️ CHẾ ĐỘ 2: Mở Modal để CẬP NHẬT DỮ LIỆU
 */
function openModalForEdit(pet) {
  isEditMode = true;
  currentEditingPetId = pet.petId;
  uploadedModalAvatarPath = pet.avatar;

  originalPetData = JSON.parse(JSON.stringify(pet));

  document.querySelector(".modal-header h3").innerText =
    `Cập nhật: ${pet.petName || "Bé Cưng"}`;

  fillPetForm(pet);
  renderDeleteButton();

  document.getElementById("petModal").classList.add("active");
}

function fillPetForm(pet) {
  document.getElementById("modalPetCode").value = pet.petCode || "Chưa cấp mã";
  document.getElementById("modalPetName").value = pet.petName || "";
  document.getElementById("modalSpecies").value = pet.species || "";
  document.getElementById("modalBreed").value = pet.breed || "";
  document.getElementById("modalPetAge").value =
    pet.age !== null ? pet.age : "";
  document.getElementById("modalWeight").value =
    pet.weight !== null ? pet.weight : "";
  document.getElementById("modalPetGender").value = pet.gender || "";
  document.getElementById("modalHealthNote").value = pet.healthNote || "";
  document.getElementById("modalPetAvatar").src = pet.avatar
    ? getImageUrl(pet.avatar)
    : DEFAULT_PET_SVG;
}

function renderDeleteButton() {
  let btnDelete = document.getElementById("btnModalDelete");
  if (!btnDelete) {
    btnDelete = document.createElement("button");
    btnDelete.type = "button";
    btnDelete.id = "btnModalDelete";
    btnDelete.className = "modal-btn-danger";
    btnDelete.innerText = "Xóa Bé Cưng";

    const modalActions = document.querySelector(".modal-actions");
    if (modalActions) {
      modalActions.insertBefore(btnDelete, modalActions.firstChild);
    }
  }
  btnDelete.style.display = "block";
}

/**
 * Gửi yêu cầu POST thêm mới
 */
async function submitAddPet(token) {
  const payload = getFormPayload();
  try {
    const response = await fetch(`${BASE_URL}/pets`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      showPetToast("🎉 Thêm bé cưng mới thành công!", "success");
      closeAndReloadModal(token);
    } else {
      const resultText = await response.text();
      handleApiError(resultText, "Không thể thêm bé cưng!");
    }
  } catch (error) {
    showPetToast("Mất kết nối máy chủ thêm mới!", "error");
  }
}

/**
 * Gửi yêu cầu PUT cập nhật dữ liệu
 */
async function submitUpdatePet(token) {
  const payload = getFormPayload();
  try {
    const response = await fetch(`${BASE_URL}/pets/${currentEditingPetId}`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      showPetToast("🎉 Cập nhật thông tin thành công!", "success");
      closeAndReloadModal(token);
    } else {
      const resultText = await response.text();
      handleApiError(resultText, "Không thể cập nhật thông tin!");
    }
  } catch (error) {
    showPetToast("Mất kết nối máy chủ cập nhật!", "error");
  }
}

/**
 * Gửi yêu cầu DELETE xóa dữ liệu khỏi hệ thống
 */
async function submitDeletePet(token) {
  if (!currentEditingPetId) return;

  if (
    !confirm(
      `Bạn có chắc chắn muốn xóa thông tin của bé này không? Thao tác này không thể hoàn tác!`,
    )
  ) {
    return;
  }

  try {
    const response = await fetch(`${BASE_URL}/pets/${currentEditingPetId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });

    if (response.ok) {
      showPetToast("🗑️ Đã xóa thông tin bé cưng khỏi hệ thống!", "success");
      closeAndReloadModal(token);
    } else {
      showPetToast("Không thể xóa thú cưng vào lúc này!", "error");
    }
  } catch (error) {
    console.error(error);
    showPetToast("Lỗi kết nối máy chủ xóa dữ liệu!", "error");
  }
}

/**
 * Thu thập và đóng gói dữ liệu từ giao diện
 */
function getFormPayload() {
  return {
    petName: document.getElementById("modalPetName").value.trim(),
    species: document.getElementById("modalSpecies").value,
    breed: document.getElementById("modalBreed").value.trim(),
    age: parseInt(document.getElementById("modalPetAge").value) || 0,
    weight: parseFloat(document.getElementById("modalWeight").value) || 0.0,
    gender: document.getElementById("modalPetGender").value,
    avatar: uploadedModalAvatarPath || null,
    healthNote: document.getElementById("modalHealthNote").value.trim(),
  };
}

/**
 * Gửi ảnh tải lên máy chủ xử lý file nhị phân
 */
async function uploadModalAvatar(file, token) {
  const formData = new FormData();
  formData.append("file", file);
  try {
    const response = await fetch(`${BASE_URL}/upload/avatar`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    });
    if (response.ok) {
      uploadedModalAvatarPath = await response.text();
      document.getElementById("modalPetAvatar").src = getImageUrl(
        uploadedModalAvatarPath,
      );
      showPetToast("Đã tải ảnh thú cưng lên hệ thống!", "success");
    }
  } catch (error) {
    console.error(error);
    showPetToast("Lỗi upload ảnh thú cưng!", "error");
  }
}

function closeAndReloadModal(token) {
  document.getElementById("petModal").classList.remove("active");
  fetchMyPets(token);
}

function handleApiError(resultText, defaultMessage) {
  try {
    const errorObj = JSON.parse(resultText);
    showPetToast(errorObj.message || defaultMessage, "error");
  } catch (e) {
    showPetToast(resultText || defaultMessage, "error");
  }
}

function initModalActions(token) {
  const modal = document.getElementById("petModal");
  const closeBtn = document.getElementById("closeModalBtn");
  const resetBtn = document.getElementById("btnModalReset");

  if (closeBtn && modal) {
    closeBtn.addEventListener("click", () => modal.classList.remove("active"));
    modal.addEventListener("click", (e) => {
      if (e.target === modal) modal.classList.remove("active");
    });
  }

  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      if (isEditMode && originalPetData) {
        fillPetForm(originalPetData);
        uploadedModalAvatarPath = originalPetData.avatar;
        showPetToast("Đã khôi phục dữ liệu ban đầu của bé!", "success");
      } else {
        document.getElementById("formAddPet").reset();
        document.getElementById("modalPetAvatar").src = DEFAULT_PET_SVG;
        uploadedModalAvatarPath = null;
        openModalForAdd(token);
        showPetToast("Đã làm trống Form nhập liệu!", "success");
      }
    });
  }

  const modalActions = document.querySelector(".modal-actions");
  if (modalActions) {
    modalActions.addEventListener("click", function (e) {
      if (e.target && e.target.id === "btnModalDelete") {
        submitDeletePet(token);
      }
    });
  }
}

function getImageUrl(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return SERVER_HOST + path;
}

function showPetToast(message, type = "success") {
  let container = document.getElementById("pet-toast-container");
  if (!container && window.parent && window.parent.document) {
    container = window.parent.document.getElementById("pet-toast-container");
  }

  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `pet-toast ${type}`;

  const icon = type === "success" ? "🎉" : "❌";
  toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;

  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3000);
}
