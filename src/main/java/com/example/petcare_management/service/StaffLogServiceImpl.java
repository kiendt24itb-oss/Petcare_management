package com.example.petcare_management.service;

import com.example.petcare_management.dto.StaffLogResponse;
import com.example.petcare_management.entity.Staff;
import com.example.petcare_management.entity.StaffLog;
import com.example.petcare_management.repository.StaffLogRepository;
import com.example.petcare_management.repository.StaffRepository;
import com.example.petcare_management.service.StaffLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Annotation đặt ở class Impl này để Spring nhận diện Bean
public class StaffLogServiceImpl implements StaffLogService {

    @Autowired
    private StaffLogRepository staffLogRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Override
    public void addLog(Integer staffId, String text) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên mang ID: " + staffId));

        StaffLog log = StaffLog.builder()
                .staff(staff)
                .logText(text)
                .logTime(LocalDateTime.now())
                .build();

        staffLogRepository.save(log);
    }

    @Override
    public List<StaffLogResponse> getTodayLogs() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<StaffLog> entityList = staffLogRepository.findLogsByDateRange(startOfDay, endOfDay);

        return entityList.stream().map(log -> StaffLogResponse.builder()
                .logId(log.getLogId())
                .staffId(log.getStaff().getStaffId())
                .staffCode(log.getStaff().getStaffCode())
                .staffName(log.getStaff().getFullName())
                .logTime(log.getLogTime())
                .logText(log.getLogText())
                .build()
        ).collect(Collectors.toList());
    }
}