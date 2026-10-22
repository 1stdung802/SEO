package com.dung.seoanalyzer.seoanalyzer.service;

/**
 * Lỗi khi gọi dịch vụ AI (thiếu key, vượt hạn mức, mất kết nối...).
 * Mang theo mã HTTP để controller trả về đúng trạng thái cho client.
 */
public class AiServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    public AiServiceException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public AiServiceException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
