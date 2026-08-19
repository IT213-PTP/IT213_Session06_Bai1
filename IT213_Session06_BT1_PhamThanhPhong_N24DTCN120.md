# Bài tập 1: Thiết Kế DTO & Ràng Buộc Dữ Liệu Giao Dịch Chuyển Khoản

**Thông tin sinh viên:**
- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên / Lớp:** N24DTCN120
- **Môn học:** IT213 (Phát triển ứng dụng AI với Spring Boot)
- **Session:** Session 06 - Bài tập 1
- **Hệ sinh thái:** RIKKEI INTELLIGENT BANKING & ASSISTANT SUITE (RikkeiPay)

---

## 1. Giới thiệu & Bối cảnh Nghiệp vụ

Trong hệ sinh thái tài chính ngân hàng thông minh **RikkeiPay (Rikkei Intelligent Banking & Assistant Suite)**, phân hệ trợ lý ảo AI (**RikkeiPay Assistant**) đóng vai trò kết nối trực tiếp ngôn ngữ tự nhiên của khách hàng với hệ thống Core Banking doanh nghiệp. 

Khi người dùng đưa ra câu lệnh như: *"Chuyển 5,000,000 VND từ tài khoản của tôi cho số tài khoản 0987654321 ngân hàng Vietcombank với nội dung trả tiền hàng"*, mô hình LLM (như Gemini 2.5 Flash / GPT-4o) sẽ trích xuất intent và thông tin thành dạng JSON payload để thực hiện **Function Calling / Tool Calling**.

Tuy nhiên, mô hình AI hoàn toàn có thể gặp hiện tượng **ảo tưởng (hallucination)** hoặc trích xuất sai/thiếu dữ liệu (ví dụ: số tiền âm, mã ngân hàng không tồn tại, số tài khoản trống). Để đảm bảo tính toàn vẹn và an toàn tuyệt đối cho hệ thống Core Banking, việc thiết kế các **Data Transfer Object (DTO)** phòng thủ mạnh mẽ với **Jakarta Bean Validation** ở tầng ranh giới (Boundary Tier) là bắt buộc.

---

## 2. Mã nguồn Triển khai DTO & Validation

### 2.1. Class `TransactionStatus.java` (Enum Trạng thái Giao dịch)

[TransactionStatus.java](file:///c:/Users/ACER/3D%20Objects/IT213/Session06/IT213_Session06_BT1_PhamThanhPhong_N24DTCN120/src/main/java/com/rikkei/banking/dto/TransactionStatus.java)

```java
package com.rikkei.banking.dto;

/**
 * Enum đại diện cho các trạng thái giao dịch chuyển khoản trong Core Banking RikkeiPay.
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED,
    PENDING
}
```

---

### 2.2. Java Record `TransferRequest.java` (DTO Chuyển khoản với Validation Ràng buộc)

[TransferRequest.java](file:///c:/Users/ACER/3D%20Objects/IT213/Session06/IT213_Session06_BT1_PhamThanhPhong_N24DTCN120/src/main/java/com/rikkei/banking/dto/TransferRequest.java)

```java
package com.rikkei.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Java Record đại diện cho Yêu cầu chuyển khoản từ AI Agent sang Core Banking.
 * Sử dụng Jakarta Bean Validation để chặn đứng dữ liệu không hợp lệ từ LLM.
 */
public record TransferRequest(
    @NotBlank(message = "ID tài khoản nguồn (senderAccountId) không được để trống")
    String senderAccountId,

    @NotBlank(message = "Số tài khoản người nhận (receiverAccountNumber) không được để trống")
    @Pattern(regexp = "^[0-9]{8,16}$", message = "Số tài khoản nhận phải từ 8 đến 16 chữ số")
    String receiverAccountNumber,

    @NotBlank(message = "Mã ngân hàng (bankCode) không được để trống")
    @Pattern(
        regexp = "^(VCB|TCB|MB|BIDV|CTG|ACB|VPB|TPB|STB|VIB)$",
        message = "Mã ngân hàng nhận không hợp lệ (Phải thuộc danh sách ngân hàng liên kết: VCB, TCB, MB, BIDV, CTG, ACB, VPB, TPB, STB, VIB)"
    )
    String bankCode,

    @NotNull(message = "Số tiền chuyển khoản (amount) không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền chuyển khoản tối thiểu phải từ 10,000 VND")
    @Digits(integer = 12, fraction = 2, message = "Số tiền không hợp lệ (tối đa 12 chữ số phần nguyên và 2 chữ số thập phân)")
    BigDecimal amount,

    @Size(max = 140, message = "Nội dung chuyển khoản không được vượt quá 140 ký tự")
    String description
) {}
```

---

### 2.3. Java Record `TransferResponse.java` (DTO Phản hồi Giao dịch)

[TransferResponse.java](file:///c:/Users/ACER/3D%20Objects/IT213/Session06/IT213_Session06_BT1_PhamThanhPhong_N24DTCN120/src/main/java/com/rikkei/banking/dto/TransferResponse.java)

```java
package com.rikkei.banking.dto;

import java.time.LocalDateTime;

/**
 * Java Record đại diện cho Kết quả xử lý giao dịch chuyển khoản từ Core Banking.
 */
public record TransferResponse(
    String transactionId,
    TransactionStatus status,
    String message,
    LocalDateTime timestamp
) {
    public static TransferResponse success(String transactionId, String message) {
        return new TransferResponse(transactionId, TransactionStatus.SUCCESS, message, LocalDateTime.now());
    }

    public static TransferResponse failed(String transactionId, String message) {
        return new TransferResponse(transactionId, TransactionStatus.FAILED, message, LocalDateTime.now());
    }
}
```

---

## 3. Phân tích Lập trình Phòng thủ (Defensive Validation Analysis)

### 3.1. Sử dụng `BigDecimal` cho Tiền tệ (`amount`)
- **Vấn đề của `double` / `float`:** Các kiểu dữ liệu số thực chấm động (`double`, `float`) vi phạm nguyên tắc lưu trữ chính xác số học nhị phân (Binary Floating-Point Representational Error), dẫn đến các sai số nguy hiểm (ví dụ: `10000.00` bị lưu thành `9999.999999999998`).
- **Giải pháp `BigDecimal`:** Đảm bảo độ chính xác tuyệt đối từng xu trong tính toán và kiểm tra hạn mức tài chính ngân hàng.
- **Ràng buộc `@DecimalMin(value = "10000.00")`:** Chặn đứng các giao dịch dưới hạn mức chuyển khoản tối thiểu 10,000 VND theo chính sách nghiệp vụ của RikkeiPay.

### 3.2. Ràng buộc Mã ngân hàng nhận (`bankCode`) với `@Pattern`
- LLM có thể trích xuất các mã ngân hàng viết sai (ví dụ: `"VIETCOMBANK"`, `"BankOfChina"`, `"BANK_XYZ"`).
- Việc sử dụng Regex `^(VCB|TCB|MB|BIDV|CTG|ACB|VPB|TPB|STB|VIB)$` giới hạn cứng danh sách ngân hàng thuộc hệ thống chuyển tiền nhanh NAPAS được RikkeiPay hỗ trợ, tránh lỗi khi routing giao dịch sang bên thứ ba.

### 3.3. Ràng buộc Số tài khoản nhận (`receiverAccountNumber`)
- Sử dụng `@Pattern(regexp = "^[0-9]{8,16}$")` giúp loại bỏ trường hợp LLM vô tình lấy phải số điện thoại, ký tự đặc biệt hoặc chuỗi chữ cái không thể thực thi giao dịch.

---

## 4. Minh chứng Chạy Thực tế & Log Console

Lớp kiểm thử [TransferRequestValidationTest.java](file:///c:/Users/ACER/3D%20Objects/IT213/Session06/IT213_Session06_BT1_PhamThanhPhong_N24DTCN120/src/test/java/com/rikkei/banking/TransferRequestValidationTest.java) được xây dựng để giả lập gửi các request chứa dữ liệu không hợp lệ từ LLM tới hệ thống validation:

```java
package com.rikkei.banking;

import com.rikkei.banking.dto.TransferRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Test 1: Request hợp lệ - Vượt qua lớp Validation phòng thủ")
    void testValidTransferRequest() {
        TransferRequest request = new TransferRequest(
            "ACC_123456",
            "0987654321",
            "VCB",
            new BigDecimal("50000.00"),
            "Chuyen tien mua hang"
        );

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Request hợp lệ không được có lỗi validation!");
        System.out.println("=== TEST 1 SUCCESS: Request hợp lệ và vượt qua lớp Validation phòng thủ! ===");
    }

    @Test
    @DisplayName("Test 2: Vi phạm số tiền tối thiểu (< 10,000 VND)")
    void testAmountLessThanMinimum() {
        TransferRequest request = new TransferRequest(
            "ACC_123456",
            "0987654321",
            "TCB",
            new BigDecimal("5000.00"), // Vi phạm < 10,000 VND
            "Chuyen tien ca phe"
        );

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        System.out.println("\n=======================================================");
        System.out.println("=== TEST 2: KẾT QUẢ CÁCH LY REQUEST LỖI (SỐ TIỀN < 10,000 VND) ===");
        System.out.println("=======================================================");
        for (ConstraintViolation<TransferRequest> v : violations) {
            System.out.println("[VALIDATION ERROR] Thuộc tính: " + v.getPropertyPath() +
                               " | Giá trị truyền vào: " + v.getInvalidValue() +
                               " | Thông báo lỗi: " + v.getMessage());
        }
    }

    @Test
    @DisplayName("Test 3: Vi phạm mã ngân hàng rác & số tài khoản nhận rỗng")
    void testInvalidBankCodeAndEmptyReceiverAccount() {
        TransferRequest request = new TransferRequest(
            "", // Vi phạm Blank
            "123", // Vi phạm pattern (chỉ 3 chữ số)
            "BANK_XYZ", // Mã ngân hàng không tồn tại trong hệ thống
            new BigDecimal("20000.00"),
            "Chuyen tien"
        );

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        System.out.println("\n=======================================================");
        System.out.println("=== TEST 3: KẾT QUẢ CHẶN ĐỨNG REQUEST CHỨA NHIỀU DỮ LIỆU RÁC ===");
        System.out.println("=======================================================");
        for (ConstraintViolation<TransferRequest> v : violations) {
            System.out.println("[VALIDATION ERROR] Thuộc tính: " + v.getPropertyPath() +
                               " | Giá trị truyền vào: " + v.getInvalidValue() +
                               " | Thông báo lỗi: " + v.getMessage());
        }
    }
}
```

### 📋 Log Console Kết Quả Thực Thi Thực Tế:

```text
=== TEST 1 SUCCESS: Request hợp lệ và vượt qua lớp Validation phòng thủ! ===

=======================================================
=== TEST 2: KẾT QUẢ CÁCH LY REQUEST LỖI (SỐ TIỀN < 10,000 VND) ===
=======================================================
[VALIDATION ERROR] Thuộc tính: amount | Giá trị truyền vào: 5000.00 | Thông báo lỗi: Số tiền chuyển khoản tối thiểu phải từ 10,000 VND

=======================================================
=== TEST 3: KẾT QUẢ CHẶN ĐỨNG REQUEST CHỨA NHIỀU DỮ LIỆU RÁC ===
=======================================================
[VALIDATION ERROR] Thuộc tính: senderAccountId | Giá trị truyền vào:  | Thông báo lỗi: ID tài khoản nguồn (senderAccountId) không được để trống
[VALIDATION ERROR] Thuộc tính: receiverAccountNumber | Giá trị truyền vào: 123 | Thông báo lỗi: Số tài khoản nhận phải từ 8 đến 16 chữ số
[VALIDATION ERROR] Thuộc tính: bankCode | Giá trị truyền vào: BANK_XYZ | Thông báo lỗi: Mã ngân hàng nhận không hợp lệ (Phải thuộc danh sách ngân hàng liên kết: VCB, TCB, MB, BIDV, CTG, ACB, VPB, TPB, STB, VIB)
```

---

## 5. Kết luận & Tổng kết

1. **Hiệu quả phòng thủ:** Việc thiết kế DTO dưới dạng Java Record kết hợp Jakarta Bean Validation giúp chặn đứng 100% các dữ liệu sai lệch, rác hoặc ảo tưởng do LLM trích xuất trước khi gọi vào Core Banking Services.
2. **Tuân thủ Chuẩn mực Doanh nghiệp:** Dữ liệu tiền tệ được chuẩn hóa bằng `BigDecimal`, phân định trạng thái rõ ràng qua Enum `TransactionStatus` và định dạng phản hồi chuẩn qua `TransferResponse`.

