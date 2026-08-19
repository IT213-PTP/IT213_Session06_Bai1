package com.rikkei.banking.dto;

import java.time.LocalDateTime;

/**
 * Java Record đại diện cho kết quả xử lý giao dịch chuyển khoản từ Core Banking RikkeiPay.
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
