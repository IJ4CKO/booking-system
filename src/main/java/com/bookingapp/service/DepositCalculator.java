package com.bookingapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the deposit required for a booking request.
 *
 * This utility class applies the agreed deposit rules for the business.
 * The deposit amount is based on the final order total entered by the
 * business owner or staff member.
 */
public final class DepositCalculator {

    /**
     * Lower order total threshold used for deposit calculation.
     */
    private static final BigDecimal LOWER_THRESHOLD =
            new BigDecimal("150.00");

    /**
     * Upper order total threshold used for deposit calculation.
     */
    private static final BigDecimal UPPER_THRESHOLD =
            new BigDecimal("200.00");

    /**
     * Deposit required for orders below the lower threshold.
     */
    private static final BigDecimal LOWER_DEPOSIT =
            new BigDecimal("10.00");

    /**
     * Deposit required for orders between the lower and upper thresholds.
     */
    private static final BigDecimal MIDDLE_DEPOSIT =
            new BigDecimal("20.00");

    /**
     * Deposit required for orders above the upper threshold.
     */
    private static final BigDecimal UPPER_DEPOSIT =
            new BigDecimal("50.00");

    /**
     * Prevents objects of this utility class from being created.
     *
     * All methods in this class are static, so the class does not need to be
     * instantiated.
     */
    private DepositCalculator() {
        // Prevent this utility class from being instantiated.
    }

    /**
     * Calculates the deposit required for an order.
     *
     * The deposit rules are:
     *
     * - Below £150.00: £10.00
     * - £150.00 to £200.00 inclusive: £20.00
     * - Above £200.00: £50.00
     *
     * @param orderTotal the final order price
     * @return the required deposit
     * @throws IllegalArgumentException if the order total is null or negative
     */
    public static BigDecimal calculateRequiredDeposit(
            BigDecimal orderTotal
    ) {
        if (orderTotal == null) {
            throw new IllegalArgumentException(
                    "The order total is required."
            );
        }

        BigDecimal normalisedTotal =
                orderTotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        if (normalisedTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "The order total cannot be negative."
            );
        }

        if (normalisedTotal.compareTo(LOWER_THRESHOLD) < 0) {
            return LOWER_DEPOSIT;
        }

        if (normalisedTotal.compareTo(UPPER_THRESHOLD) <= 0) {
            return MIDDLE_DEPOSIT;
        }

        return UPPER_DEPOSIT;
    }
}