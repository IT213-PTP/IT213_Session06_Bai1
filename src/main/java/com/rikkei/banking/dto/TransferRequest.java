package com.rikkei.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Java Record đại diện cho yêu cầu chuyển khoản ngân hàng trong hệ sinh thái RikkeiPay.
 * Được bổ sung các Annotation Validation để lập trình phòng thủ trước dữ liệu trích xuất từ LLM.
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
