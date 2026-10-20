package com.saas.billing.domain;

import java.time.LocalDate;

public final class BillingCommand {

    private final String customerId;
    private final String planType;
    private final Money basePrice;
    private final LocalDate signupDate;
    private final int overdueDays;
    private final String promoCode;
    private final boolean nonProfit;
    private final String paymentCardToken;

    public BillingCommand(String customerId, String planType, Money basePrice,
                           LocalDate signupDate, int overdueDays, String promoCode,
                           boolean nonProfit, String paymentCardToken) {
        this.customerId = customerId;
        this.planType = planType;
        this.basePrice = basePrice;
        this.signupDate = signupDate;
        this.overdueDays = overdueDays;
        this.promoCode = promoCode;
        this.nonProfit = nonProfit;
        this.paymentCardToken = paymentCardToken;
    }

    public String customerId() { return customerId; }
    public String planType() { return planType; }
    public Money basePrice() { return basePrice; }
    public LocalDate signupDate() { return signupDate; }
    public int overdueDays() { return overdueDays; }
    public String promoCode() { return promoCode; }
    public boolean isNonProfit() { return nonProfit; }
    public String paymentCardToken() { return paymentCardToken; }
}