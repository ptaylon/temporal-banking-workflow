package com.example.temporal.notification.domain.port.out;

/**
 * Port for email sending operations
 * Defines what the domain needs for sending emails
 */
public interface EmailPort {

    /**
     * Sends an email notification
     * @param recipient the recipient email address
     * @param subject the email subject
     * @param body the email body
     * @return true if email was sent successfully
     */
    boolean sendEmail(String recipient, String subject, String body);

    /**
     * Sends an email notification with detailed result
     * @param recipient the recipient email address
     * @param subject the email subject
     * @param body the email body
     * @return the email result
     */
    default EmailResult sendEmailWithResult(String recipient, String subject, String body) {
        boolean success = sendEmail(recipient, subject, body);
        if (success) {
            return EmailResult.success();
        } else {
            return EmailResult.failure("Failed to send email");
        }
    }

    /**
     * Email notification result
     */
    record EmailResult(
            boolean isSuccess,
            String errorMessage
    ) {
        public static EmailResult success() {
            return new EmailResult(true, null);
        }

        public static EmailResult failure(String errorMessage) {
            return new EmailResult(false, errorMessage);
        }
    }
}
