package com.example.petcare_management.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 🌐 CẤU HÌNH CORS: Thông quan toàn diện cho Front-End, cân luôn cả request OPTIONS preflight
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // =====================================================
                        // 🔑 1. AUTHENTICATION & PUBLIC ENDPOINTS
                        // =====================================================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/accounts/login", "/api/accounts/register").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Thả xích xem ảnh đại diện, file upload không cần token

                        // =====================================================
                        // ⏱️ 2. HỆ THỐNG CHẤM CÔNG (ATTENDANCE) - ĐƯA LÊN TRƯỚC TRÁNH ĐÈ PATH
                        // =====================================================
                        .requestMatchers("/api/attendance/admin/**").hasRole("ADMIN") // Chỉ Admin được vào dashboard tổng / quét bù
                        .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "STAFF") // Staff & Admin tự chấm công, tự xem lịch sử

                        // =====================================================
                        // 🏨 3. PHÂN QUYỀN HỆ THỐNG PHÒNG ỐC (ROOMS)
                        // =====================================================
                        .requestMatchers(HttpMethod.POST, "/api/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/**").authenticated()

                        // =====================================================
                        // 🛠️ 4. PHÂN QUYỀN HỆ THỐNG DỊCH VỤ (SERVICES)
                        // =====================================================
                        .requestMatchers("/api/services/create").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/services/update/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/services/toggle/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/services/all").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/services/search").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/services/active").authenticated()

                        // =====================================================
                        // 📊 5. THỐNG KÊ & KPI DASHBOARD
                        // =====================================================
                        .requestMatchers("/api/v1/dashboard/operation-summary").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/admin/dashboard/**", "/api/admin/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/dashboard/**", "/api/dashboard/**").hasAnyRole("ADMIN", "STAFF")

                        // =====================================================
                        // 👥 6. PHÂN QUYỀN KHÁCH HÀNG (CUSTOMERS)
                        // =====================================================
                        .requestMatchers("/api/customers/profile").authenticated()
                        .requestMatchers("/api/customers/booking-info").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/customers").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/customers/search").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "STAFF")

                        // =====================================================
                        // 👔 7. PHÂN QUYỀN NHÂN VIÊN (STAFFS)
                        // =====================================================
                        .requestMatchers("/api/staffs/me").authenticated()
                        .requestMatchers("/api/staffs/search").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/staffs/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/staffs/active").authenticated()
                        .requestMatchers("/api/staffs/**").hasRole("ADMIN") // Quản lý nhân viên toàn quyền cho Admin

                        // =====================================================================
                        // 📅 8. CÁC HỆ THỐNG CÒN LẠI (PETS, BOOKINGS)
                        // =====================================================================
                        // 🐾 Thú cưng (Pets)
                        .requestMatchers("/api/pets/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/pets/**").authenticated()

                        // 📅 Hệ thống Đặt lịch (Bookings) - ĐÃ PHÂN QUYỀN RẠCH RÒI CHUẨN CHỈ
                        .requestMatchers("/api/bookings/admin/**").hasRole("ADMIN")   // 🔒 Chỉ ADMIN mới được sờ vào đầu /admin/**
                        .requestMatchers("/api/bookings/staff/**").hasRole("STAFF")   // 🔓 Chỉ STAFF mới được gọi vào đầu /staff/**
                        .requestMatchers("/api/bookings/*/logs").authenticated()
                        .requestMatchers("/api/bookings", "/api/bookings/**").authenticated()
                        // 🔐 Chốt chặn cuối cùng bảo mật toàn hệ thống
                        .anyRequest().authenticated()
                );

        // Đóng dấu filter JWT vào trước thềm xác thực của Spring Security
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}