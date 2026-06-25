package com.example.petcare_management.service;

import com.example.petcare_management.dto.PetRequest;
import com.example.petcare_management.dto.PetResponse;
import com.example.petcare_management.entity.Customer;
import com.example.petcare_management.entity.Pet;
import com.example.petcare_management.repository.CustomerRepository;
import com.example.petcare_management.repository.PetRepository;
import com.example.petcare_management.service.PetService;
import com.example.petcare_management.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public PetResponse addPet(String username, PetRequest request) {
        // 1. Tìm thông tin khách hàng dựa vào username đang đăng nhập
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Bạn cần cập nhật thông tin cá nhân trước khi thêm thú cưng!"));

        // 2. 🌟 SỬ DỤNG THUẬT TOÁN SINH MÃ TỰ ĐỘNG CỦA NÍ 🌟
        String latestCode = petRepository.findLatestPetCode();
        String nextCode = CodeGenerator.generateNextCode(latestCode, "TC"); // Sinh mã dạng TC-Axxx

        // 3. Khởi tạo đối tượng Pet và đổ data từ Form vào
        Pet pet = Pet.builder()
                .petCode(nextCode)
                .customer(customer) // Gắn rễ trực tiếp vào khách hàng sở hữu
                .petName(request.getPetName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .age(request.getAge())
                .weight(request.getWeight())
                .gender(request.getGender())
                .avatar(request.getAvatar())
                .healthNote(request.getHealthNote())
                .build();

        Pet savedPet = petRepository.save(pet);
        return mapToResponse(savedPet);
    }

    @Override
    @Transactional
    public PetResponse updatePet(String username, Integer petId, PetRequest request) {
        // 1. Tìm bé Pet xem có tồn tại trong DB không
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thú cưng này!"));

        // 2. 🛡️ BẢO MẬT CHỦ SỞ HỮU: Kiểm tra xem khứa đang đăng nhập có đúng là chủ của bé Pet này không
        if (!pet.getCustomer().getAccount().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa thú cưng của người khác!");
        }

        // 3. Tiến hành đè dữ liệu mới lên bản ghi cũ
        pet.setPetName(request.getPetName());
        pet.setSpecies(request.getSpecies());
        pet.setBreed(request.getBreed());
        pet.setAge(request.getAge());
        pet.setWeight(request.getWeight());
        pet.setGender(request.getGender());
        pet.setHealthNote(request.getHealthNote());

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            pet.setAvatar(request.getAvatar());
        }

        Pet updatedPet = petRepository.save(pet);
        return mapToResponse(updatedPet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getMyPets(String username) {
        // Trả về danh sách pet sạch đẹp bằng Stream API
        return petRepository.findByCustomer_Account_Username(username)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetDetail(String username, Integer petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thú cưng!"));

        if (!pet.getCustomer().getAccount().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền xem thông tin thú cưng này!");
        }

        return mapToResponse(pet);
    }

    /**
     * 🔄 Hàm Converter thủ công chuyển đổi từ Entity sang DTO sạch
     * 🌟 ĐÃ CẬP NHẬT: Thêm mapping trường customerId phục vụ hiển thị Front-End Admin
     */
    private PetResponse mapToResponse(Pet pet) {
        return PetResponse.builder()
                .petId(pet.getPetId())
                .petCode(pet.getPetCode())
                .customerId(pet.getCustomer() != null ? pet.getCustomer().getCustomerId() : null) // 🐾 Đút ID chủ nuôi vào đây nè ní!
                .petName(pet.getPetName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .age(pet.getAge())
                .weight(pet.getWeight())
                .gender(pet.getGender())
                .avatar(pet.getAvatar())
                .healthNote(pet.getHealthNote())
                .createdAt(pet.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String findLatestPetCode() {
        // Gọi thẳng câu Query xịn của ní trong Repo ra luôn
        return petRepository.findLatestPetCode();
    }

    @Override
    @Transactional
    public void deletePet(String username, Integer petId) {
        // 1. Tìm xem bé Pet có tồn tại không
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thú cưng cần xóa!"));

        // 2. 🛡️ Bảo mật: Kiểm tra xem khứa đang xóa có đúng là chủ nuôi không, tránh trường hợp F12 chế Id xóa bậy
        if (!pet.getCustomer().getAccount().getUsername().equals(username)) {
            throw new RuntimeException("Bạn không có quyền xóa thú cưng của người khác!");
        }

        // 3. Đúng chủ rồi thì múc thôi ní! Hàm delete này có sẵn của JpaRepository rồi nè
        petRepository.delete(pet);
    }

    /**
     * 👑 OVERRIDE DÀNH CHO ADMIN: Bốc sạch toàn bộ danh sách Pet sử dụng hàm mặc định
     * Đã chuyển sang dùng findAll() gốc để đồng bộ với Repository nguyên bản của ní.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PetResponse> getAllPetsForAdmin() {
        // 🌟 SỬA DÒNG NÀY: Đổi từ findAllWithCustomer() thành findAll() chuẩn của JPA
        return petRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}