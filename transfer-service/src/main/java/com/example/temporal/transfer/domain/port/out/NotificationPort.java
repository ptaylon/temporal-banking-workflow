package com.example.temporal.transfer.domain.port.out;

/**
 * Output port (driven port) for notifications
 * Defines contract for sending transfer notifications
 */
public interface NotificationPort {

    /**
     * Notify that transfer was initiated
     */
    void notifyTransferInitiated(Long transferId);

    /**
     * Notify that transfer was completed
     */
    void notifyTransferCompleted(Long transferId);

    /**
     * Notify that transfer failed
     */
    void notifyTransferFailed(Long transferId, String reason);
}
