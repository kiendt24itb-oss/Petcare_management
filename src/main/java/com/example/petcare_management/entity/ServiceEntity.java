package com.example.petcare_management.entity;

import com.example.petcare_management.entity.enums.ServiceCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "service_code", nullable = false, unique = true, length = 10)
    private String serviceCode; // Mã tự sinh (DV-H001, DV-S001, DV-O001...)

    @Column(name = "service_name", nullable = false, length = 150)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ServiceCategory category; // HEALTH, SPA, HOTEL

    @Column(name = "price", nullable = false)
    private BigDecimal price; // Giá tiền dịch vụ

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Thời gian ước tính thực hiện (phút)

    /**
     * 🚪 MỐI QUAN HỆ KẾT NỐI VỚI CLASS ROOM (ĐÃ ĐỒNG BỘ)
     * Dùng @JsonIgnore để chặn vòng lặp Jackson lỗi API
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id", nullable = true)
    @JsonIgnore
    private Room room;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    @Builder.Default
    private Boolean status = true; // TRUE: Đang kinh doanh, FALSE: Ngừng phục vụ

    /**
     * 💡 HÀM PHẲNG HÓA DỮ LIỆU JSON CHO FRONT-END ĐỌC
     * Spring Boot tự động render ra thuộc tính "roomCode" mà JS đang cần quét
     */
    @Transient
    public String getRoomCode() {
        return (this.room != null) ? this.room.getRoomCode() : "Chưa gán";
    }
}