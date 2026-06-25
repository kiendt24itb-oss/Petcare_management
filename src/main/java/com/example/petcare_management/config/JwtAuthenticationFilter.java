package com.example.petcare_management.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 🔥 1. BÙA THÔNG QUAN CHO TRÌNH DUYỆT: Chặn và trả về 200 OK ngay lập tức cho request OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Cache-Control");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);
            return; // Dừng luôn tại đây, không cho đi tiếp vào vòng lặp check Token mệt mỏi nữa!
        }

        try {
            // 2. Lấy chuỗi mã Token từ Header người dùng gửi lên
            String jwt = getJwtFromRequest(request);

            // 3. Nếu có token và token này hợp lệ (máy soi báo OK)
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                // 4. Giải mã token để lấy thông tin cá nhân
                String username = tokenProvider.getUsernameFromJWT(jwt);
                String role = tokenProvider.getRoleFromJWT(jwt); // Lấy role (ADMIN/STAFF/CUSTOMER)

                // 5. Ép chuỗi role sang viết HOA toàn bộ (.toUpperCase()) để tránh lệch pha với SecurityConfig
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim());
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(authority);

                // Trong JwtAuthenticationFilter.java - Bước số 6 sửa lại thành:
                org.springframework.security.core.userdetails.UserDetails userDetails =
                        new org.springframework.security.core.userdetails.User(username, "", authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities); // Đút userDetails vào đây thay vì username
                
                // 7. Đút chứng nhận vào hệ thống -> Yêu cầu phân quyền chính thức được thông qua!
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Không thể thiết lập quyền hạn người dùng", ex);
        }

        // Đi tiếp vào các bộ lọc sau hoặc chạy thẳng vào Controller đối với các request GET, POST, PUT, DELETE thật
        filterChain.doFilter(request, response);
    }

    // Hàm phụ tách chuỗi Token ra khỏi chữ "Bearer " bọc ở đầu Header
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // 🔥 CHÈN 2 DÒNG NÀY ĐỂ SOI TẬN GỐC:
        System.out.println("====== KIỂM TRA CHUỖI HEADER NHẬN ĐƯỢC ======");
        System.out.println("Giá trị Header Authorization là: [" + bearerToken + "]");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String tokenRuot = bearerToken.substring(7);
            System.out.println("Ruột Token sau khi cắt chữ Bearer: [" + tokenRuot + "]");
            return tokenRuot;
        }
        return null;
    }
}