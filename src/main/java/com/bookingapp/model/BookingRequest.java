package com.bookingapp.model;
 
import java.math.BigDecimal;
 
/**
 * An enquiry submitted by a customer, before it becomes a booking.
 */
public class BookingRequest {
 
    /** Database identifier; zero until the request has been saved. */
    private int requestId;
 
    /** The name the customer gave when submitting. */
    private String fullName;
 
    /** The email address the customer gave. */
    private String email;
 
    /** The telephone number the customer gave. */
    private String phone;
 
    /** The kind of service asked for, where a single one was chosen. */
    private String serviceType;
 
    /** Display only: a readable summary of everything asked for across the request's lines. */
    private String requestedServicesSummary;
 
    /** Whether the order is to be collected or delivered; stored as {@code fulfilment_type}. */
    private String collectionOrDelivery;
 
    /** Where the order is to be delivered; unset for a collection. */
    private String dropoffAddress;
 
    /** The single product asked for, on requests predating the request lines table. */
    private Integer productId;
 
    /** How many of that single product; on requests predating the request lines table. */
    private Integer quantity;
 
    /** The date the order is wanted for, as {@code YYYY-MM-DD}. */
    private String eventDate;
 
    /** The time the order is wanted for. */
    private String eventTime;
 
    /** Whatever the customer wrote in their own words. */
    private String details;
 
    /** Where the request came from, for example the website. */
    private String source;
 
    /** Where the request has reached in the review workflow. */
    private String status;
 
    /** Why the request was turned down; stored as {@code reject_reason}. */
    private String rejectionReason;
 
    /** When the request was submitted, stamped by the database. */
    private String submittedAt;
 
    /** What the order comes to; unset until the request has been priced. */
    private BigDecimal orderTotal;
 
    /** The deposit to be collected before confirming; unset until the request has been priced. */
    private BigDecimal requiredDeposit;
 
    /** Whether a deposit has been requested, is pending, or has been paid. */
    private String depositStatus;
 
    /** When the deposit was asked for; unset until it has been. */
    private String depositRequestedDate;
 
    /** When the deposit was received; unset until it has been. */
    private String depositPaidDate;
 
    /** How the deposit is to be paid. */
    private String paymentMethod;
 
    /** The reference given when the deposit was paid; may be absent. */
    private String paymentReference;
 
    /*
     * The confirmed booking created from this request.
     *
     * This is separate from requestId because booking IDs and
     * request IDs are generated independently.
     *
     * Unset until the request is confirmed, and unique once it is,
     * so a request can never produce two bookings.
     */
    private Integer bookingId;
    
    public BookingRequest() {
    }
 
    /**
     * Creates a request from its stored columns, together with the summary of
     * what was asked for.
     *
     * @param requestId                the database identifier
     * @param fullName                 the customer's name
     * @param email                    the customer's email address
     * @param phone                    the customer's telephone number
     * @param serviceType              the kind of service asked for
     * @param requestedServicesSummary a readable summary of everything asked for
     * @param collectionOrDelivery     whether the order is to be collected or delivered
     * @param dropoffAddress           the delivery address, or {@code null} for a collection
     * @param productId                the single product asked for, on older requests
     * @param quantity                 how many of it, on older requests
     * @param eventDate                the date the order is wanted for
     * @param eventTime                the time the order is wanted for
     * @param details                  what the customer wrote in their own words
     * @param source                   where the request came from
     * @param status                   where the request has reached
     * @param rejectionReason          why it was turned down, if it was
     * @param submittedAt              when it was submitted
     * @param orderTotal               what the order comes to, or {@code null} if unpriced
     * @param requiredDeposit          the deposit wanted, or {@code null} if unpriced
     * @param depositStatus            whether a deposit has been requested or paid
     * @param depositRequestedDate     when the deposit was asked for
     * @param depositPaidDate          when the deposit was received
     * @param paymentMethod            how the deposit is to be paid
     * @param paymentReference         the reference given on payment
     * @param bookingId                the booking created from this request, or {@code null}
     */
    public BookingRequest(
            int requestId,
            String fullName,
            String email,
            String phone,
            String serviceType,
            String requestedServicesSummary,
            String collectionOrDelivery,
            String dropoffAddress,
            Integer productId,
            Integer quantity,
            String eventDate,
            String eventTime,
            String details,
            String source,
            String status,
            String rejectionReason,
            String submittedAt,
            BigDecimal orderTotal,
            BigDecimal requiredDeposit,
            String depositStatus,
            String depositRequestedDate,
            String depositPaidDate,
            String paymentMethod,
            String paymentReference,
            Integer bookingId
    ) {
        this.requestId = requestId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.serviceType = serviceType;
        this.requestedServicesSummary
                = requestedServicesSummary;
        this.collectionOrDelivery
                = collectionOrDelivery;
        this.dropoffAddress = dropoffAddress;
        this.productId = productId;
        this.quantity = quantity;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.details = details;
        this.source = source;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
        this.orderTotal = orderTotal;
        this.requiredDeposit = requiredDeposit;
        this.depositStatus = depositStatus;
        this.depositRequestedDate
                = depositRequestedDate;
        this.depositPaidDate = depositPaidDate;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.bookingId = bookingId;
    }
 
    /**
     * @return the database identifier, or zero if the request has not been saved
     */
    public int getRequestId() {
        return requestId;
    }
 
    /**
     * @param requestId the database identifier
     */
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }
 
    /**
     * @return the customer's name
     */
    public String getFullName() {
        return fullName;
    }
 
    /**
     * @param fullName the customer's name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
 
    /**
     * @return the customer's email address
     */
    public String getEmail() {
        return email;
    }
 
    /**
     * @param email the customer's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
 
    /**
     * @return the customer's telephone number
     */
    public String getPhone() {
        return phone;
    }
 
    /**
     * @param phone the customer's telephone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }
 
    /**
     * @return the kind of service asked for
     */
    public String getServiceType() {
        return serviceType;
    }
 
    /**
     * @param serviceType the kind of service asked for
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
 
    /**
     * Returns a readable summary of everything the customer asked for.
     *
     * @return the summary, or {@code null} if it was not assembled
     */
    public String getRequestedServicesSummary() {
        return requestedServicesSummary;
    }
 
    /**
     * @param requestedServicesSummary a readable summary of everything asked for
     */
    public void setRequestedServicesSummary(
            String requestedServicesSummary
    ) {
        this.requestedServicesSummary
                = requestedServicesSummary;
    }
 
    /**
     * @return whether the order is to be collected or delivered
     */
    public String getCollectionOrDelivery() {
        return collectionOrDelivery;
    }
 
    /**
     * @param collectionOrDelivery whether the order is to be collected or delivered
     */
    public void setCollectionOrDelivery(
            String collectionOrDelivery
    ) {
        this.collectionOrDelivery
                = collectionOrDelivery;
    }
 
    /**
     * @return the delivery address, or {@code null} where the order is being collected
     */
    public String getDropoffAddress() {
        return dropoffAddress;
    }
 
    /**
     * @param dropoffAddress the delivery address
     */
    public void setDropoffAddress(
            String dropoffAddress
    ) {
        this.dropoffAddress = dropoffAddress;
    }
 
    /**
     * Returns the single product asked for, on requests predating the request
     * lines table.
     *
     * @return the product asked for, or {@code null} on a newer request
     */
    public Integer getProductId() {
        return productId;
    }
 
    /**
     * @param productId the single product asked for, on older requests
     */
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
 
    /**
     * Returns how many of the single product were asked for, on requests
     * predating the request lines table.
     *
     * @return the quantity, or {@code null} on a newer request
     */
    public Integer getQuantity() {
        return quantity;
    }
 
    /**
     * @param quantity how many of the single product, on older requests
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
 
    /**
     * @return the date the order is wanted for, as {@code YYYY-MM-DD}
     */
    public String getEventDate() {
        return eventDate;
    }
 
    /**
     * @param eventDate the date the order is wanted for, as {@code YYYY-MM-DD}
     */
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }
 
    /**
     * @return the time the order is wanted for
     */
    public String getEventTime() {
        return eventTime;
    }
 
    /**
     * @param eventTime the time the order is wanted for
     */
    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }
 
    /**
     * @return whatever the customer wrote in their own words
     */
    public String getDetails() {
        return details;
    }
 
    /**
     * @param details whatever the customer wrote in their own words
     */
    public void setDetails(String details) {
        this.details = details;
    }
 
    /**
     * @return where the request came from, for example the website
     */
    public String getSource() {
        return source;
    }
 
    /**
     * @param source where the request came from
     */
    public void setSource(String source) {
        this.source = source;
    }
 
    /**
     * Returns where the request has reached in the review workflow.
     *
     * @return the current status
     */
    public String getStatus() {
        return status;
    }
 
    /**
     * @param status where the request has reached
     */
    public void setStatus(String status) {
        this.status = status;
    }
 
    /**
     * @return why the request was turned down, or {@code null} if it was not
     */
    public String getRejectionReason() {
        return rejectionReason;
    }
 
    /**
     * @param rejectionReason why the request was turned down
     */
    public void setRejectionReason(
            String rejectionReason
    ) {
        this.rejectionReason = rejectionReason;
    }
 
    /**
     * @return when the request was submitted
     */
    public String getSubmittedAt() {
        return submittedAt;
    }
 
    /**
     * @param submittedAt when the request was submitted
     */
    public void setSubmittedAt(
            String submittedAt
    ) {
        this.submittedAt = submittedAt;
    }
 
    /**
     * Returns what the order comes to.
     * @return the order total to two decimal places, or {@code null} if unpriced
     */
    public BigDecimal getOrderTotal() {
        return orderTotal;
    }
 
    /**
     * @param orderTotal what the order comes to
     */
    public void setOrderTotal(
            BigDecimal orderTotal
    ) {
        this.orderTotal = orderTotal;
    }
 
    /**
     * Returns the deposit to be collected before the request can be confirmed.
     *
     * @return the deposit to two decimal places, or {@code null} if unpriced
     */
    public BigDecimal getRequiredDeposit() {
        return requiredDeposit;
    }
 
    /**
     * @param requiredDeposit the deposit to be collected before confirming
     */
    public void setRequiredDeposit(
            BigDecimal requiredDeposit
    ) {
        this.requiredDeposit = requiredDeposit;
    }
 
    /**
     * @return whether a deposit has been requested, is pending, or has been paid
     */
    public String getDepositStatus() {
        return depositStatus;
    }
 
    /**
     * @param depositStatus whether a deposit has been requested, is pending, or paid
     */
    public void setDepositStatus(
            String depositStatus
    ) {
        this.depositStatus = depositStatus;
    }
 
    /**
     * @return when the deposit was asked for, or {@code null} if it has not been
     */
    public String getDepositRequestedDate() {
        return depositRequestedDate;
    }
 
    /**
     * @param depositRequestedDate when the deposit was asked for
     */
    public void setDepositRequestedDate(
            String depositRequestedDate
    ) {
        this.depositRequestedDate
                = depositRequestedDate;
    }
 
    /**
     * @return when the deposit was received, or {@code null} if it has not been
     */
    public String getDepositPaidDate() {
        return depositPaidDate;
    }
 
    /**
     * @param depositPaidDate when the deposit was received
     */
    public void setDepositPaidDate(
            String depositPaidDate
    ) {
        this.depositPaidDate = depositPaidDate;
    }
 
    /**
     * @return how the deposit is to be paid
     */
    public String getPaymentMethod() {
        return paymentMethod;
    }
 
    /**
     * @param paymentMethod how the deposit is to be paid
     */
    public void setPaymentMethod(
            String paymentMethod
    ) {
        this.paymentMethod = paymentMethod;
    }
 
    /**
     * @return the reference given when the deposit was paid, or {@code null} if none was
     */
    public String getPaymentReference() {
        return paymentReference;
    }
 
    /**
     * @param paymentReference the reference given when the deposit was paid
     */
    public void setPaymentReference(
            String paymentReference
    ) {
        this.paymentReference = paymentReference;
    }
 
    /**
     * Returns the booking created from this request.
     *
     * @return the booking created from this request, or {@code null} if not yet confirmed
     */
    public Integer getBookingId() {
        return bookingId;
    }
 
    /**
     * @param bookingId the booking created from this request
     */
    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }
 
    /** Display only: a readable summary of the products asked for and their quantities. */
    private String productQuantitiesSummary;
 
    /**
     * Returns a readable summary of the products asked for and their
     * quantities.
     *
     * @return the summary, or {@code null} if it was not assembled
     */
    public String getProductQuantitiesSummary() {
        return productQuantitiesSummary;
    }
 
    /**
     * @param productQuantitiesSummary a readable summary of the products asked for
     */
    public void setProductQuantitiesSummary(
            String productQuantitiesSummary
    ) {
        this.productQuantitiesSummary
                = productQuantitiesSummary;
    }
}