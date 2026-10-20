package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Parameter Object representando a requisicao de faturamento.
 * Implementado como record para imutabilidade e concisao (Java 14+).
 */
public record BillingCommand(
    String customerId,
    String planType,
    Money basePrice,
    LocalDate signupDate,
    int overdueDays,
    String promoCode,
    boolean isNonProfit,
    String paymentCardToken
) {
    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId nao pode ser nulo");
        Objects.requireNonNull(basePrice, "basePrice nao pode ser nulo");
        Objects.requireNonNull(signupDate, "signupDate nao pode ser nulo");
        overdueDays = Math.max(0, overdueDays);
        promoCode = (promoCode != null) ? promoCode.trim() : null;
    }
}
