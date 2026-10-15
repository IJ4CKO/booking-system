package com.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Automated JUnit tests for key booking, stock, FIFO, deposit and
 * website booking request logic.
 *
 * These tests use simple test versions of the system logic so that important
 * business rules can be checked automatically without clicking through the
 * JavaFX interface each time.
 */
public class BookingLogicTest {

    /*
     * -------------------------------------------------------------------------
     * Duplicate booking prevention tests.
     * 10 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: duplicate booking expected = {9}")
    @DisplayName("Duplicate active booking prevention")
    @CsvSource({
            "10, 6, 18/09/2026, 14:00, CONFIRMED, 10, 6, 18/09/2026, 14:00, true",
            "10, 6, 18/09/2026, 14:00, CONFIRMED, 11, 6, 18/09/2026, 14:00, false",
            "10, 6, 18/09/2026, 14:00, CONFIRMED, 10, 7, 18/09/2026, 14:00, false",
            "10, 6, 18/09/2026, 14:00, CONFIRMED, 10, 6, 19/09/2026, 14:00, false",
            "10, 6, 18/09/2026, 14:00, CONFIRMED, 10, 6, 18/09/2026, 15:00, false",
            "10, 6, 18/09/2026, 14:00, CANCELLED, 10, 6, 18/09/2026, 14:00, false",
            "10, 6, 18/09/2026, 14:00, REJECTED, 10, 6, 18/09/2026, 14:00, false",
            "10, 6, 18/09/2026, 14:00, confirmed, 10, 6, 18/09/2026, 14:00, true",
            "10, 6, 18/09/2026, 14:00, READY_TO_CONFIRM, 10, 6, 18/09/2026, 14:00, true",
            "10, 6, 18/09/2026, 14:00, AWAITING_STOCK, 10, 6, 18/09/2026, 14:00, true"
    })
    void duplicateActiveBookingPrevention(
            int existingCustomerId,
            int existingProductId,
            String existingDate,
            String existingTime,
            String existingStatus,
            int checkCustomerId,
            int checkProductId,
            String checkDate,
            String checkTime,
            boolean expectedDuplicate
    ) {
        TestBookingService service = new TestBookingService();

        service.addBooking(new TestBooking(
                1,
                existingCustomerId,
                existingProductId,
                existingDate,
                existingTime,
                existingStatus
        ));

        boolean duplicateExists = service.activeBookingAlreadyExists(
                checkCustomerId,
                checkProductId,
                checkDate,
                checkTime
        );

        assertEquals(expectedDuplicate, duplicateExists);
    }

    /*
     * -------------------------------------------------------------------------
     * Booking validation tests.
     * 10 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: booking validation expected = {5}")
    @DisplayName("Booking validation")
    @CsvSource({
            "10, 6, 18/09/2026, 14:00, 1, true",
            "0, 6, 18/09/2026, 14:00, 1, false",
            "10, 0, 18/09/2026, 14:00, 1, false",
            "10, 6, '', 14:00, 1, false",
            "10, 6, 31/02/2026, 14:00, 1, false",
            "10, 6, 18/09/2026, '', 1, false",
            "10, 6, 18/09/2026, 25:00, 1, false",
            "10, 6, 18/09/2026, 14:00, 0, false",
            "10, 6, 18/09/2026, 14:00, -2, false",
            "10, 6, 18/09/2026, 14:00, 1000, false"
    })
    void bookingValidation(
            int customerId,
            int productId,
            String bookingDate,
            String bookingTime,
            int quantity,
            boolean expectedValid
    ) {
        boolean valid = BookingValidator.isValidBooking(
                customerId,
                productId,
                bookingDate,
                bookingTime,
                quantity
        );

        assertEquals(expectedValid, valid);
    }

    /*
     * -------------------------------------------------------------------------
     * Booking cancellation/status rule tests.
     * 8 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: can cancel {0} = {1}")
    @DisplayName("Booking cancellation status rules")
    @CsvSource({
            "CONFIRMED, true",
            "AWAITING_STOCK, true",
            "READY_TO_CONFIRM, true",
            "PENDING_REVIEW, true",
            "CANCELLED, false",
            "COMPLETED, false",
            "REJECTED, false",
            "'', false"
    })
    void bookingCancellationStatusRules(
            String status,
            boolean expectedCanCancel
    ) {
        boolean canCancel = BookingStatusRules.canCancel(status);

        assertEquals(expectedCanCancel, canCancel);
    }

    /*
     * -------------------------------------------------------------------------
     * Stock deduction tests.
     * 10 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: stock {0} minus {1}")
    @DisplayName("Stock deduction")
    @CsvSource({
            "10.0, 3.0, 7.0, false",
            "10.0, 10.0, 0.0, false",
            "10.0, 0.0, 10.0, true",
            "10.0, -1.0, 10.0, true",
            "2.0, 5.0, 2.0, true",
            "2.5, 1.25, 1.25, false",
            "0.0, 1.0, 0.0, true",
            "100.0, 33.5, 66.5, false",
            "5.0, 4.99, 0.01, false",
            "5.0, 5.01, 5.0, true"
    })
    void stockDeduction(
            double startingQuantity,
            double quantityToDeduct,
            double expectedQuantity,
            boolean shouldThrowException
    ) {
        TestInventoryItem item = new TestInventoryItem(
                1,
                "Flour",
                startingQuantity
        );

        TestInventoryService service = new TestInventoryService();

        if (shouldThrowException) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deductStock(item, quantityToDeduct)
            );

            assertEquals(expectedQuantity, item.getQuantity(), 0.001);
        } else {
            service.deductStock(item, quantityToDeduct);

            assertEquals(expectedQuantity, item.getQuantity(), 0.001);
        }
    }

    /*
     * -------------------------------------------------------------------------
     * Stock restoration tests.
     * 8 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: stock {0} plus {1}")
    @DisplayName("Stock restoration")
    @CsvSource({
            "7.0, 3.0, 10.0, false",
            "0.0, 5.0, 5.0, false",
            "1.25, 1.25, 2.5, false",
            "10.0, 0.0, 10.0, true",
            "10.0, -2.0, 10.0, true",
            "100.0, 33.5, 133.5, false",
            "5.0, 0.5, 5.5, false",
            "2.0, 100.0, 102.0, false"
    })
    void stockRestoration(
            double startingQuantity,
            double quantityToRestore,
            double expectedQuantity,
            boolean shouldThrowException
    ) {
        TestInventoryItem item = new TestInventoryItem(
                1,
                "Flour",
                startingQuantity
        );

        TestInventoryService service = new TestInventoryService();

        if (shouldThrowException) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.restoreStock(item, quantityToRestore)
            );

            assertEquals(expectedQuantity, item.getQuantity(), 0.001);
        } else {
            service.restoreStock(item, quantityToRestore);

            assertEquals(expectedQuantity, item.getQuantity(), 0.001);
        }
    }

    /*
     * -------------------------------------------------------------------------
     * Insufficient stock handling tests.
     * 8 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: available {0}, required {1}")
    @DisplayName("Insufficient stock checks")
    @CsvSource({
            "10.0, 5.0, true",
            "10.0, 10.0, true",
            "10.0, 11.0, false",
            "0.0, 0.0, true",
            "0.0, 1.0, false",
            "2.5, 2.51, false",
            "2.5, 2.5, true",
            "-1.0, 1.0, false"
    })
    void insufficientStockChecks(
            double availableQuantity,
            double requiredQuantity,
            boolean expectedHasEnoughStock
    ) {
        TestInventoryItem item = new TestInventoryItem(
                1,
                "Butter",
                availableQuantity
        );

        TestInventoryService service = new TestInventoryService();

        boolean hasEnoughStock = service.hasEnoughStock(
                item,
                requiredQuantity
        );

        assertEquals(expectedHasEnoughStock, hasEnoughStock);
    }

    /*
     * -------------------------------------------------------------------------
     * FIFO batch allocation tests.
     * 10 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: {0}")
    @DisplayName("FIFO inventory batch allocation")
    @MethodSource("fifoAllocationCases")
    void fifoInventoryBatchAllocation(
            String testName,
            List<TestInventoryBatch> batches,
            double requiredQuantity,
            String expectedAllocationSummary,
            String expectedRemainingSummary,
            boolean shouldThrowException
    ) {
        FifoInventoryService service = new FifoInventoryService();

        if (shouldThrowException) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.allocateStock(batches, requiredQuantity)
            );

            assertEquals(expectedRemainingSummary, remainingSummary(batches));
        } else {
            List<TestBatchAllocation> allocations =
                    service.allocateStock(batches, requiredQuantity);

            assertEquals(expectedAllocationSummary, allocationSummary(allocations));
            assertEquals(expectedRemainingSummary, remainingSummary(batches));
        }
    }

    static Stream<Arguments> fifoAllocationCases() {
        return Stream.of(
                Arguments.of(
                        "Single batch partly used",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 10.0)
                        ),
                        5.0,
                        "1=5.0",
                        "1=5.0",
                        false
                ),
                Arguments.of(
                        "Oldest batch fully used then newer batch partly used",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 5.0),
                                new TestInventoryBatch(2, "05/01/2026", 5.0)
                        ),
                        8.0,
                        "1=5.0|2=3.0",
                        "1=0.0|2=2.0",
                        false
                ),
                Arguments.of(
                        "Batches sorted by purchase date before allocation",
                        List.of(
                                new TestInventoryBatch(1, "10/01/2026", 5.0),
                                new TestInventoryBatch(2, "01/01/2026", 5.0)
                        ),
                        6.0,
                        "2=5.0|1=1.0",
                        "1=4.0|2=0.0",
                        false
                ),
                Arguments.of(
                        "Same purchase date uses lower batch ID first",
                        List.of(
                                new TestInventoryBatch(2, "01/01/2026", 5.0),
                                new TestInventoryBatch(1, "01/01/2026", 5.0)
                        ),
                        7.0,
                        "1=5.0|2=2.0",
                        "1=0.0|2=3.0",
                        false
                ),
                Arguments.of(
                        "Exact quantity uses all available stock",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 4.0),
                                new TestInventoryBatch(2, "02/01/2026", 6.0)
                        ),
                        10.0,
                        "1=4.0|2=6.0",
                        "1=0.0|2=0.0",
                        false
                ),
                Arguments.of(
                        "Insufficient total stock is rejected",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 2.0),
                                new TestInventoryBatch(2, "02/01/2026", 2.0)
                        ),
                        5.0,
                        "",
                        "1=2.0|2=2.0",
                        true
                ),
                Arguments.of(
                        "Zero required quantity is rejected",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 10.0)
                        ),
                        0.0,
                        "",
                        "1=10.0",
                        true
                ),
                Arguments.of(
                        "Decimal quantities allocate correctly",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 1.5),
                                new TestInventoryBatch(2, "02/01/2026", 2.0)
                        ),
                        2.25,
                        "1=1.5|2=0.75",
                        "1=0.0|2=1.25",
                        false
                ),
                Arguments.of(
                        "Empty old batch is skipped",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 0.0),
                                new TestInventoryBatch(2, "02/01/2026", 4.0)
                        ),
                        2.0,
                        "2=2.0",
                        "1=0.0|2=2.0",
                        false
                ),
                Arguments.of(
                        "Three batches allocate oldest first",
                        List.of(
                                new TestInventoryBatch(1, "01/01/2026", 2.0),
                                new TestInventoryBatch(2, "02/01/2026", 3.0),
                                new TestInventoryBatch(3, "03/01/2026", 4.0)
                        ),
                        6.0,
                        "1=2.0|2=3.0|3=1.0",
                        "1=0.0|2=0.0|3=3.0",
                        false
                )
        );
    }

    /*
     * -------------------------------------------------------------------------
     * Deposit and payment status workflow tests.
     * 10 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: {0} / {1} using action {2}")
    @DisplayName("Deposit and payment status workflow")
    @CsvSource({
            "NOT_REQUESTED, PENDING_REVIEW, REQUEST_DEPOSIT, '', 25.0, PENDING, AWAITING_DEPOSIT, false, false",
            "PENDING, AWAITING_DEPOSIT, MARK_PAID, FJ-REQ-51, 25.0, PAID, READY_TO_CONFIRM, true, false",
            "PENDING, AWAITING_DEPOSIT, MARK_PAID, '', 25.0, PENDING, AWAITING_DEPOSIT, false, true",
            "PENDING, AWAITING_DEPOSIT, MARK_PAID, FJ-REQ-51, 0.0, PENDING, AWAITING_DEPOSIT, false, true",
            "PAID, READY_TO_CONFIRM, CONFIRM_BOOKING, FJ-REQ-51, 25.0, PAID, CONFIRMED, false, false",
            "PENDING, AWAITING_DEPOSIT, CONFIRM_BOOKING, FJ-REQ-51, 25.0, PENDING, AWAITING_DEPOSIT, false, true",
            "NOT_REQUESTED, PENDING_REVIEW, REJECT_REQUEST, '', 0.0, NOT_REQUESTED, REJECTED, false, false",
            "NOT_REQUESTED, PENDING_REVIEW, NO_ACTION, '', 0.0, NOT_REQUESTED, PENDING_REVIEW, false, false",
            "PAID, READY_TO_CONFIRM, REQUEST_DEPOSIT, '', 25.0, PAID, READY_TO_CONFIRM, false, true",
            "PAID, CONFIRMED, MARK_PAID, FJ-REQ-52, 25.0, PAID, CONFIRMED, false, true"
    })
    void depositAndPaymentStatusWorkflow(
            String currentDepositStatus,
            String currentRequestStatus,
            String action,
            String paymentReference,
            double depositAmount,
            String expectedDepositStatus,
            String expectedRequestStatus,
            boolean expectedPaidDateRecorded,
            boolean shouldThrowException
    ) {
        DepositWorkflowService service = new DepositWorkflowService();

        if (shouldThrowException) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.applyAction(
                            currentDepositStatus,
                            currentRequestStatus,
                            action,
                            paymentReference,
                            depositAmount
                    )
            );
        } else {
            DepositWorkflowResult result = service.applyAction(
                    currentDepositStatus,
                    currentRequestStatus,
                    action,
                    paymentReference,
                    depositAmount
            );

            assertEquals(expectedDepositStatus, result.getDepositStatus());
            assertEquals(expectedRequestStatus, result.getRequestStatus());
            assertEquals(
                    expectedPaidDateRecorded,
                    result.isPaidDateRecorded()
            );
        }
    }

    /*
     * -------------------------------------------------------------------------
     * Website booking request workflow tests.
     * 6 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: website request valid = {7}")
    @DisplayName("Website booking request validation")
    @CsvSource({
            "Jack Smith, jack@example.com, 6 inch ButterCream Cake, 1, 18/09/2026, 14:00, Collection, true",
            "'', jack@example.com, 6 inch ButterCream Cake, 1, 18/09/2026, 14:00, Collection, false",
            "Jack Smith, invalid-email, 6 inch ButterCream Cake, 1, 18/09/2026, 14:00, Collection, false",
            "Jack Smith, jack@example.com, '', 1, 18/09/2026, 14:00, Collection, false",
            "Jack Smith, jack@example.com, 6 inch ButterCream Cake, 0, 18/09/2026, 14:00, Collection, false",
            "Jack Smith, jack@example.com, 6 inch ButterCream Cake, 1, 31/02/2026, 14:00, Delivery, false"
    })
    void websiteBookingRequestValidation(
            String customerName,
            String email,
            String requestedProduct,
            int quantity,
            String requestedDate,
            String requestedTime,
            String collectionOrDelivery,
            boolean expectedValid
    ) {
        WebsiteBookingRequestValidator validator =
                new WebsiteBookingRequestValidator();

        boolean valid = validator.isValidRequest(
                customerName,
                email,
                requestedProduct,
                quantity,
                requestedDate,
                requestedTime,
                collectionOrDelivery
        );

        assertEquals(expectedValid, valid);
    }

    /*
     * -------------------------------------------------------------------------
     * General edge case tests.
     * 5 automated test cases.
     * -------------------------------------------------------------------------
     */

    @ParameterizedTest(name = "{index}: normalise {0} to {1}")
    @DisplayName("General status normalisation")
    @CsvSource({
            "' confirmed ', CONFIRMED",
            "cancelled, CANCELLED",
            "PENDING REVIEW, PENDING_REVIEW",
            "ready to confirm, READY_TO_CONFIRM",
            "awaiting-stock, AWAITING_STOCK"
    })
    void generalStatusNormalisation(
            String inputStatus,
            String expectedStatus
    ) {
        assertEquals(
                expectedStatus,
                TextHelper.normaliseStatus(inputStatus)
        );
    }

    /*
     * -------------------------------------------------------------------------
     * Helper methods.
     * -------------------------------------------------------------------------
     */

    private static String allocationSummary(
            List<TestBatchAllocation> allocations
    ) {
        List<String> parts = new ArrayList<>();

        for (TestBatchAllocation allocation : allocations) {
            parts.add(
                    allocation.getBatchId()
                    + "="
                    + allocation.getQuantityAllocated()
            );
        }

        return String.join("|", parts);
    }

    private static String remainingSummary(
            List<TestInventoryBatch> batches
    ) {
        List<TestInventoryBatch> sortedBatches = new ArrayList<>(batches);

        sortedBatches.sort(Comparator.comparingInt(TestInventoryBatch::getBatchId));

        List<String> parts = new ArrayList<>();

        for (TestInventoryBatch batch : sortedBatches) {
            parts.add(
                    batch.getBatchId()
                    + "="
                    + batch.getQuantityRemaining()
            );
        }

        return String.join("|", parts);
    }

    /*
     * -------------------------------------------------------------------------
     * Simple test classes used by the automated tests.
     * -------------------------------------------------------------------------
     */

    private static class TestBooking {

        private final int bookingId;
        private final int customerId;
        private final int productId;
        private final String bookingDate;
        private final String bookingTime;
        private final String status;

        private TestBooking(
                int bookingId,
                int customerId,
                int productId,
                String bookingDate,
                String bookingTime,
                String status
        ) {
            this.bookingId = bookingId;
            this.customerId = customerId;
            this.productId = productId;
            this.bookingDate = bookingDate;
            this.bookingTime = bookingTime;
            this.status = status;
        }

        private int getBookingId() {
            return bookingId;
        }

        private int getCustomerId() {
            return customerId;
        }

        private int getProductId() {
            return productId;
        }

        private String getBookingDate() {
            return bookingDate;
        }

        private String getBookingTime() {
            return bookingTime;
        }

        private String getStatus() {
            return status;
        }
    }

    private static class TestBookingService {

        private final List<TestBooking> bookings = new ArrayList<>();

        private void addBooking(TestBooking booking) {
            bookings.add(booking);
        }

        private boolean activeBookingAlreadyExists(
                int customerId,
                int productId,
                String bookingDate,
                String bookingTime
        ) {
            for (TestBooking booking : bookings) {
                boolean sameCustomer =
                        booking.getCustomerId() == customerId;

                boolean sameProduct =
                        booking.getProductId() == productId;

                boolean sameDate =
                        booking.getBookingDate().equals(bookingDate);

                boolean sameTime =
                        booking.getBookingTime().equals(bookingTime);

                boolean activeBooking =
                        !"CANCELLED".equalsIgnoreCase(
                                TextHelper.normaliseStatus(booking.getStatus())
                        )
                        && !"REJECTED".equalsIgnoreCase(
                                TextHelper.normaliseStatus(booking.getStatus())
                        );

                if (sameCustomer
                        && sameProduct
                        && sameDate
                        && sameTime
                        && activeBooking) {

                    return true;
                }
            }

            return false;
        }
    }

    private static class BookingValidator {

        private static boolean isValidBooking(
                int customerId,
                int productId,
                String bookingDate,
                String bookingTime,
                int quantity
        ) {
            return customerId > 0
                    && productId > 0
                    && isValidDate(bookingDate)
                    && isValidTime(bookingTime)
                    && quantity > 0
                    && quantity <= 999;
        }

        private static boolean isValidDate(String dateText) {
            if (dateText == null || dateText.isBlank()) {
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("dd/MM/uuuu", Locale.UK)
                    .withResolverStyle(ResolverStyle.STRICT);

            try {
                LocalDate.parse(dateText.trim(), formatter);
                return true;
            } catch (Exception exception) {
                return false;
            }
        }

        private static boolean isValidTime(String timeText) {
            if (timeText == null || timeText.isBlank()) {
                return false;
            }

            String trimmedTime = timeText.trim();

            if (!trimmedTime.matches("\\d{2}:\\d{2}")) {
                return false;
            }

            int hour = Integer.parseInt(trimmedTime.substring(0, 2));
            int minute = Integer.parseInt(trimmedTime.substring(3, 5));

            return hour >= 0
                    && hour <= 23
                    && minute >= 0
                    && minute <= 59;
        }
    }

    private static class BookingStatusRules {

        private static boolean canCancel(String status) {
            String normalisedStatus = TextHelper.normaliseStatus(status);

            return "CONFIRMED".equals(normalisedStatus)
                    || "AWAITING_STOCK".equals(normalisedStatus)
                    || "READY_TO_CONFIRM".equals(normalisedStatus)
                    || "PENDING_REVIEW".equals(normalisedStatus);
        }
    }

    private static class TestInventoryItem {

        private final int inventoryId;
        private final String itemName;
        private double quantity;

        private TestInventoryItem(
                int inventoryId,
                String itemName,
                double quantity
        ) {
            this.inventoryId = inventoryId;
            this.itemName = itemName;
            this.quantity = quantity;
        }

        private int getInventoryId() {
            return inventoryId;
        }

        private String getItemName() {
            return itemName;
        }

        private double getQuantity() {
            return quantity;
        }

        private void setQuantity(double quantity) {
            this.quantity = quantity;
        }
    }

    private static class TestInventoryService {

        private boolean hasEnoughStock(
                TestInventoryItem inventoryItem,
                double requiredQuantity
        ) {
            return inventoryItem.getQuantity() >= requiredQuantity;
        }

        private void deductStock(
                TestInventoryItem inventoryItem,
                double quantityToDeduct
        ) {
            if (quantityToDeduct <= 0) {
                throw new IllegalArgumentException(
                        "Quantity to deduct must be greater than zero."
                );
            }

            if (!hasEnoughStock(inventoryItem, quantityToDeduct)) {
                throw new IllegalArgumentException(
                        "Not enough stock available."
                );
            }

            double updatedQuantity =
                    inventoryItem.getQuantity() - quantityToDeduct;

            inventoryItem.setQuantity(updatedQuantity);
        }

        private void restoreStock(
                TestInventoryItem inventoryItem,
                double quantityToRestore
        ) {
            if (quantityToRestore <= 0) {
                throw new IllegalArgumentException(
                        "Quantity to restore must be greater than zero."
                );
            }

            double updatedQuantity =
                    inventoryItem.getQuantity() + quantityToRestore;

            inventoryItem.setQuantity(updatedQuantity);
        }
    }

    private static class TestInventoryBatch {

        private final int batchId;
        private final LocalDate purchaseDate;
        private double quantityRemaining;

        private TestInventoryBatch(
                int batchId,
                String purchaseDate,
                double quantityRemaining
        ) {
            this.batchId = batchId;
            this.purchaseDate = parseDate(purchaseDate);
            this.quantityRemaining = quantityRemaining;
        }

        private int getBatchId() {
            return batchId;
        }

        private LocalDate getPurchaseDate() {
            return purchaseDate;
        }

        private double getQuantityRemaining() {
            return quantityRemaining;
        }

        private void setQuantityRemaining(double quantityRemaining) {
            this.quantityRemaining = quantityRemaining;
        }

        private static LocalDate parseDate(String dateText) {
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("dd/MM/uuuu", Locale.UK)
                    .withResolverStyle(ResolverStyle.STRICT);

            return LocalDate.parse(dateText, formatter);
        }
    }

    private static class TestBatchAllocation {

        private final int batchId;
        private final double quantityAllocated;

        private TestBatchAllocation(
                int batchId,
                double quantityAllocated
        ) {
            this.batchId = batchId;
            this.quantityAllocated = quantityAllocated;
        }

        private int getBatchId() {
            return batchId;
        }

        private double getQuantityAllocated() {
            return quantityAllocated;
        }
    }

    private static class FifoInventoryService {

        private List<TestBatchAllocation> allocateStock(
                List<TestInventoryBatch> batches,
                double requiredQuantity
        ) {
            if (requiredQuantity <= 0) {
                throw new IllegalArgumentException(
                        "Required quantity must be greater than zero."
                );
            }

            double totalAvailable = 0.0;

            for (TestInventoryBatch batch : batches) {
                totalAvailable += batch.getQuantityRemaining();
            }

            if (totalAvailable < requiredQuantity) {
                throw new IllegalArgumentException(
                        "Not enough stock available across batches."
                );
            }

            List<TestInventoryBatch> sortedBatches =
                    new ArrayList<>(batches);

            sortedBatches.sort(
                    Comparator
                            .comparing(TestInventoryBatch::getPurchaseDate)
                            .thenComparingInt(TestInventoryBatch::getBatchId)
            );

            double quantityStillRequired = requiredQuantity;

            List<TestBatchAllocation> allocations = new ArrayList<>();

            for (TestInventoryBatch batch : sortedBatches) {
                if (quantityStillRequired <= 0) {
                    break;
                }

                if (batch.getQuantityRemaining() <= 0) {
                    continue;
                }

                double quantityAllocated = Math.min(
                        batch.getQuantityRemaining(),
                        quantityStillRequired
                );

                batch.setQuantityRemaining(
                        batch.getQuantityRemaining() - quantityAllocated
                );

                quantityStillRequired -= quantityAllocated;

                allocations.add(new TestBatchAllocation(
                        batch.getBatchId(),
                        quantityAllocated
                ));
            }

            return allocations;
        }
    }

    private static class DepositWorkflowResult {

        private final String depositStatus;
        private final String requestStatus;
        private final boolean paidDateRecorded;

        private DepositWorkflowResult(
                String depositStatus,
                String requestStatus,
                boolean paidDateRecorded
        ) {
            this.depositStatus = depositStatus;
            this.requestStatus = requestStatus;
            this.paidDateRecorded = paidDateRecorded;
        }

        private String getDepositStatus() {
            return depositStatus;
        }

        private String getRequestStatus() {
            return requestStatus;
        }

        private boolean isPaidDateRecorded() {
            return paidDateRecorded;
        }
    }

    private static class DepositWorkflowService {

        private DepositWorkflowResult applyAction(
                String currentDepositStatus,
                String currentRequestStatus,
                String action,
                String paymentReference,
                double depositAmount
        ) {
            String depositStatus =
                    TextHelper.normaliseStatus(currentDepositStatus);

            String requestStatus =
                    TextHelper.normaliseStatus(currentRequestStatus);

            String normalisedAction =
                    TextHelper.normaliseStatus(action);

            switch (normalisedAction) {
                case "REQUEST_DEPOSIT":
                    return requestDeposit(depositStatus, requestStatus);

                case "MARK_PAID":
                    return markDepositAsPaid(
                            depositStatus,
                            requestStatus,
                            paymentReference,
                            depositAmount
                    );

                case "CONFIRM_BOOKING":
                    return confirmBooking(depositStatus, requestStatus);

                case "REJECT_REQUEST":
                    return new DepositWorkflowResult(
                            depositStatus,
                            "REJECTED",
                            false
                    );

                case "NO_ACTION":
                    return new DepositWorkflowResult(
                            depositStatus,
                            requestStatus,
                            false
                    );

                default:
                    throw new IllegalArgumentException(
                            "Unsupported workflow action."
                    );
            }
        }

        private DepositWorkflowResult requestDeposit(
                String depositStatus,
                String requestStatus
        ) {
            if (!"NOT_REQUESTED".equals(depositStatus)
                    || !"PENDING_REVIEW".equals(requestStatus)) {

                throw new IllegalArgumentException(
                        "Deposit can only be requested from pending review."
                );
            }

            return new DepositWorkflowResult(
                    "PENDING",
                    "AWAITING_DEPOSIT",
                    false
            );
        }

        private DepositWorkflowResult markDepositAsPaid(
                String depositStatus,
                String requestStatus,
                String paymentReference,
                double depositAmount
        ) {
            if (!"PENDING".equals(depositStatus)
                    || !"AWAITING_DEPOSIT".equals(requestStatus)) {

                throw new IllegalArgumentException(
                        "Only pending deposits can be marked as paid."
                );
            }

            if (paymentReference == null || paymentReference.isBlank()) {
                throw new IllegalArgumentException(
                        "Payment reference is required."
                );
            }

            if (depositAmount <= 0) {
                throw new IllegalArgumentException(
                        "Deposit amount must be greater than zero."
                );
            }

            return new DepositWorkflowResult(
                    "PAID",
                    "READY_TO_CONFIRM",
                    true
            );
        }

        private DepositWorkflowResult confirmBooking(
                String depositStatus,
                String requestStatus
        ) {
            if (!"PAID".equals(depositStatus)
                    || !"READY_TO_CONFIRM".equals(requestStatus)) {

                throw new IllegalArgumentException(
                        "Booking can only be confirmed when deposit is paid."
                );
            }

            return new DepositWorkflowResult(
                    "PAID",
                    "CONFIRMED",
                    false
            );
        }
    }

    private static class WebsiteBookingRequestValidator {

        private boolean isValidRequest(
                String customerName,
                String email,
                String requestedProduct,
                int quantity,
                String requestedDate,
                String requestedTime,
                String collectionOrDelivery
        ) {
            return customerName != null
                    && !customerName.isBlank()
                    && isValidEmail(email)
                    && requestedProduct != null
                    && !requestedProduct.isBlank()
                    && quantity > 0
                    && BookingValidator.isValidDate(requestedDate)
                    && BookingValidator.isValidTime(requestedTime)
                    && isValidCollectionOrDelivery(collectionOrDelivery);
        }

        private boolean isValidEmail(String email) {
            if (email == null || email.isBlank()) {
                return false;
            }

            String trimmedEmail = email.trim();

            return trimmedEmail.contains("@")
                    && trimmedEmail.indexOf("@") > 0
                    && trimmedEmail.lastIndexOf(".") > trimmedEmail.indexOf("@");
        }

        private boolean isValidCollectionOrDelivery(
                String collectionOrDelivery
        ) {
            String normalisedValue =
                    TextHelper.normaliseStatus(collectionOrDelivery);

            return "COLLECTION".equals(normalisedValue)
                    || "DELIVERY".equals(normalisedValue);
        }
    }

    private static class TextHelper {

        private static String normaliseStatus(String status) {
            if (status == null || status.isBlank()) {
                return "";
            }

            return status
                    .trim()
                    .toUpperCase(Locale.UK)
                    .replace("-", "_")
                    .replace(" ", "_")
                    .replaceAll("_+", "_");
        }
    }
}