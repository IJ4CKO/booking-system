package com.bookingapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Provides email sending functionality for the booking and inventory system.
 *
 * This class is used to send customer emails during the website booking request
 * workflow. It sends emails when a deposit is requested, when a booking is
 * confirmed and when a booking request is rejected.
 */
public class EmailService {

    /**
     * SMTP host used for sending emails through Gmail.
     */
    private static final String SMTP_HOST = "smtp.gmail.com";

    /**
     * SMTP port used for TLS email sending.
     */
    private static final String SMTP_PORT = "587";

    /**
     * Business email account used to send customer emails.
     */
    private static final String EMAIL_USERNAME = "fjsweetbake1@gmail.com";

    /**
     * Name of the environment variable that stores the email password.
     *
     * The password is not stored directly in the source code.
     */
    private static final String EMAIL_PASSWORD_ENV = "BOOKING_APP_EMAIL_PASSWORD";

    /**
     * Formats a money value for use in customer emails.
     *
     * If the amount is null, £0.00 is returned. Otherwise, the amount is rounded
     * to two decimal places and shown with the pound symbol.
     *
     * @param amount the money value to format
     * @return the formatted currency value
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "£0.00";
        }

        return "£"
                + amount.setScale(
                        2,
                        RoundingMode.HALF_UP
                ).toPlainString();
    }

    /**
     * Safely prepares text for use in an email.
     *
     * If the value is null or blank, the fallback text is returned instead.
     * Otherwise, the trimmed value is returned.
     *
     * @param value    the text value to check
     * @param fallback the fallback text to use if the value is blank
     * @return the cleaned email text
     */
    private String safeEmailText(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    /**
     * Sends an email to tell the customer that a deposit is required.
     *
     * This email is sent after the booking request has been reviewed and
     * provisionally accepted by the business owner or staff member. It explains
     * that the booking is not yet confirmed and gives the order total, required
     * deposit and remaining balance.
     *
     * @param customerEmail    the customer's email address
     * @param customerName     the customer's name
     * @param date             the requested booking date
     * @param time             the requested booking time
     * @param orderTotal       the final order total
     * @param requiredDeposit  the deposit required from the customer
     * @param remainingBalance the balance remaining after the deposit
     * @throws Exception if the email cannot be sent
     */
    public void sendBookingAwaitingDepositEmail(
            String customerEmail,
            String customerName,
            String date,
            String time,
            BigDecimal orderTotal,
            BigDecimal requiredDeposit,
            BigDecimal remainingBalance
    ) throws Exception {

        String subject
                = "Deposit required for your booking request";

        String body = """
            Hello %s,

            Your booking request has been reviewed and
            provisionally accepted.

            Requested date: %s
            Requested time: %s

            Final order total: %s
            Required deposit: %s
            Remaining balance after deposit: %s

            Your booking is not yet confirmed. It will only be
            confirmed after the required deposit has been
            received and verified.

            The deposit should be paid by bank transfer using
            the instructions provided by the business.

            Please verify any unexpected changes to the bank
            details using the business's known contact details.

            Thank you,
            Fiona June Cakes & Events
            """.formatted(
                customerName,
                date,
                time,
                formatCurrency(orderTotal),
                formatCurrency(requiredDeposit),
                formatCurrency(remainingBalance)
        );

        sendEmail(
                customerEmail,
                subject,
                body
        );
    }

    /**
     * Sends an email to tell the customer that their booking is confirmed.
     *
     * This email is sent after the deposit has been received and verified. It
     * gives the customer their request reference, booking reference, payment
     * reference, fulfilment details and remaining balance.
     *
     * @param customerEmail    the customer's email address
     * @param customerName     the customer's name
     * @param requestId        the website booking request ID
     * @param bookingId        the confirmed booking ID
     * @param date             the confirmed booking date
     * @param time             the confirmed booking time
     * @param fulfilmentType   whether the booking is for collection or delivery
     * @param dropoffAddress   the delivery address, if delivery is selected
     * @param orderTotal       the final order total
     * @param depositPaid      the deposit amount paid by the customer
     * @param remainingBalance the balance still to be paid
     * @param paymentReference the bank-transfer payment reference
     * @throws Exception if the email cannot be sent
     */
    public void sendBookingConfirmedEmail(
            String customerEmail,
            String customerName,
            int requestId,
            int bookingId,
            String date,
            String time,
            String fulfilmentType,
            String dropoffAddress,
            BigDecimal orderTotal,
            BigDecimal depositPaid,
            BigDecimal remainingBalance,
            String paymentReference
    ) throws Exception {

        String fulfilmentDescription;

        if ("DELIVERY".equalsIgnoreCase(
                fulfilmentType
        )) {
            fulfilmentDescription
                    = "Delivery to: "
                    + safeEmailText(
                            dropoffAddress,
                            "Address not supplied"
                    );

        } else {
            fulfilmentDescription
                    = "Collection";
        }

        String subject
                = "Your booking is confirmed - Booking "
                + bookingId;

        String body = """
            Hello %s,

            Your deposit has been received and your booking is
            now confirmed.

            Request reference: FJ-REQ-%d
            Confirmed booking reference: FJ-BKG-%d
            Bank-transfer reference: %s

            Date: %s
            Time: %s
            Fulfilment: %s

            Final order total: %s
            Deposit received: %s
            Remaining balance: %s

            The remaining balance must be paid no later than
            48 hours before the collection or delivery.

            Please contact the business as soon as possible if
            any of these booking details are incorrect.

            Thank you,
            Fiona June Cakes & Events
            """.formatted(
                customerName,
                requestId,
                bookingId,
                safeEmailText(
                        paymentReference,
                        "Not recorded"
                ),
                date,
                time,
                fulfilmentDescription,
                formatCurrency(orderTotal),
                formatCurrency(depositPaid),
                formatCurrency(remainingBalance)
        );

        sendEmail(
                customerEmail,
                subject,
                body
        );
    }

    /**
     * Sends an email to tell the customer that their booking request was
     * rejected.
     *
     * The rejection reason is included so that the customer understands why the
     * request could not be accepted.
     *
     * @param customerEmail   the customer's email address
     * @param customerName    the customer's name
     * @param rejectionReason the reason the booking request was rejected
     * @throws Exception if the email cannot be sent
     */
    public void sendBookingRejectedEmail(
            String customerEmail,
            String customerName,
            String rejectionReason
    ) throws Exception {

        String subject = "Your booking request could not be accepted";

        String body = """
                Hello %s,

                Unfortunately, your booking request could not be accepted.

                Reason:
                %s

                Please feel free to get in touch to discuss an alternative.

                Thank you,
                Fiona June Cakes & Events
                """.formatted(
                    customerName,
                    rejectionReason
        );

        sendEmail(
                customerEmail,
                subject,
                body
        );
    }

    /**
     * Sends an email using the configured SMTP settings.
     *
     * The email password is loaded from an environment variable so that the
     * password is not written directly into the source code.
     *
     * @param to      the recipient email address
     * @param subject the email subject line
     * @param body    the email body text
     * @throws Exception if the environment variable is missing or the email
     *                   cannot be sent
     */
    private void sendEmail(
            String to,
            String subject,
            String body
    ) throws Exception {

        String emailPassword
                = getRequiredEnvironmentVariable(
                        EMAIL_PASSWORD_ENV
                );

        Properties properties = new Properties();

        properties.put(
                "mail.smtp.auth",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        properties.put(
                "mail.smtp.host",
                SMTP_HOST
        );

        properties.put(
                "mail.smtp.port",
                SMTP_PORT
        );

        Session session
                = Session.getInstance(
                        properties,
                        new Authenticator() {
                    @Override
                    protected PasswordAuthentication
                            getPasswordAuthentication() {

                        return new PasswordAuthentication(
                                EMAIL_USERNAME,
                                emailPassword
                        );
                    }
                }
                );

        Message message
                = new MimeMessage(session);

        message.setFrom(
                new InternetAddress(
                        EMAIL_USERNAME
                )
        );

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(to)
        );

        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    /**
     * Gets a required environment variable.
     *
     * This is used to load sensitive configuration values, such as the email
     * password, without storing them directly in the Java source code.
     *
     * @param variableName the name of the environment variable to read
     * @return the trimmed environment variable value
     * @throws IllegalStateException if the environment variable is missing or blank
     */
    private String getRequiredEnvironmentVariable(
            String variableName
    ) {
        String value
                = System.getenv(variableName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "The environment variable "
                    + variableName
                    + " has not been configured."
            );
        }

        return value.trim();
    }
}