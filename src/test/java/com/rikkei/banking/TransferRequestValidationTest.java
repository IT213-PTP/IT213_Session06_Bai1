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
    @DisplayName("Test 1: Request hợp lệ - Không phát sinh lỗi validation")
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
            "123", // Vi phạm pattern (ít hơn 8 số)
            "BANK_XYZ", // Mã ngân hàng không nằm trong danh sách
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
