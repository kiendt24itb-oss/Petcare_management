package com.example.petcare_management.util;

public class CodeGenerator {

    /**
     * Thuật toán sinh mã nhảy tự động: Prefix-A001 -> Prefix-A099 -> Prefix-B001
     * @param maxCode Mã lớn nhất hiện tại lấy từ DB (Ví dụ: "KH-A099")
     * @param prefix Tiền tố của bảng (Ví dụ: "KH", "TC", "NV")
     * @return Mã mới tự động tăng kế tiếp
     */
    public static String generateNextCode(String maxCode, String prefix) {
        // 1. Nếu DB trống hoặc format không đúng (không chứa dấu "-"), trả về mã đầu tiên
        if (maxCode == null || maxCode.isEmpty() || !maxCode.contains("-")) {
            return prefix + "-A001";
        }

        try {
            // 2. Tách chuỗi bằng dấu "-"
            String[] parts = maxCode.split("-");
            String codePart = parts[1]; // "A099"

            // Validate phòng trường hợp độ dài phần code không chuẩn 4 ký tự (Ví dụ: A01)
            if (codePart.length() < 4) {
                return prefix + "-A001";
            }

            char currentLetter = codePart.charAt(0); // Lấy chữ 'A'
            int currentNumber = Integer.parseInt(codePart.substring(1)); // Lấy số 99

            // 3. Logic nhảy mã: Nếu chạm mốc 99 thì tăng chữ cái (A lên B) và reset số về 1
            if (currentNumber >= 99) {
                // Nếu vượt quá chữ 'Z', bạn có thể cân nhắc reset hoặc xử lý thêm, tạm thời tăng tiếp theo bảng ASCII
                currentLetter++;
                currentNumber = 1;
            } else {
                currentNumber++;
            }

            // 4. Format lại số thành 3 chữ số (Ví dụ: 1 -> "001")
            String formattedNumber = String.format("%03d", currentNumber);

            // 5. Trả về mã hoàn chỉnh
            return prefix + "-" + currentLetter + formattedNumber;

        } catch (Exception e) {
            // Nếu có bất kỳ lỗi parse nào do dữ liệu DB sai format, fallback về mã mặc định thay vì sập app
            return prefix + "-A001";
        }
    }
}