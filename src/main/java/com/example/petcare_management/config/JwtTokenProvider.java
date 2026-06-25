package com.example.petcare_management.config;

import com.example.petcare_management.entity.Account;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Chuỗi mã khóa bí mật (Secret Key) để đóng dấu Token - dài trên 32 ký tự
    private final String JWT_SECRET = "cGFzY3VhbC1zZWNyZXQta2V5LWZvci1wZXRjYXJlLW1hbmFnZW1lbnQtMjAyNg==";

    // Thời gian hết hạn của token: 24 tiếng (tính bằng mili-giây)
    private final long JWT_EXPIRATION = 86400000L;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    // [HOÀN CHỈNH]: Tạo token - Ép luôn Role vào trong gói dữ liệu (Claim)
    public String generateToken(Account account) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(account.getUsername()) // Lưu username
                .claim("role", account.getRole().name()) // LƯU ROLE VÀO ĐÂY (ADMIN/STAFF/CUSTOMER)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Móc username ra từ Token
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // [THÊM MỚI HOÀN CHỈNH]: Móc quyền (Role) ra từ Token
    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class); // Trả về chuỗi "ADMIN", "STAFF" hoặc "CUSTOMER"
    }

    // Kiểm tra tính hợp lệ của Token (Sai dấu, Hết hạn, Fake...)
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            System.out.println("Token không đúng định dạng!");
        } catch (ExpiredJwtException ex) {
            System.out.println("Token đã hết hạn sử dụng!");
        } catch (UnsupportedJwtException ex) {
            System.out.println("Token không được hỗ trợ!");
        } catch (IllegalArgumentException ex) {
            System.out.println("Chuỗi Token bị rỗng!");
        }
        return false;
    }
}