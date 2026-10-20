package com.saas.billing.domain;

import java.time.LocalDate;

/**
 * Especialista no cálculo de precificação, descontos e acréscimos por atraso.
 * 
 * Padrões e Refatorações Aplicadas:
 * - Extract Class (Martin Fowler): isola a lógica de cálculo de preços (SRP).
 * - Replace Magic Number with Symbolic Constant: substitui taxas e descontos por constantes nomeadas.
 */
public class PriceCalculator {

    public static final double LOYALTY_DISCOUNT_TIER_1 = 0.10; // 10% após 1 ano (12 meses)
    public static final double LOYALTY_DISCOUNT_TIER_2 = 0.15; // 15% após 2 anos (24 meses)
    public static final double NON_PROFIT_DISCOUNT_RATE = 0.70; // 30% de desconto para ONGs
    public static final double BLACK_FRIDAY_FIXED_DISCOUNT = 30.0; // R$ 30,00 fixos em novembro
    public static final double WELCOME_PROMO_RATE = 0.90; // 10% de desconto para recém-chegados
    public static final double OVERDUE_PENALTY_RATE = 0.02; // Multa fixa de 2%
    public static final double OVERDUE_DAILY_INTEREST_RATE = 0.001; // Juros de 0.1% ao dia

    public Money calculateFinalPrice(BillingCommand command, LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate signupDate = command.signupDate();

        int monthsActive = calculateMonthsActive(signupDate, today);
        double basePrice = command.basePrice().toDouble();
        double calculatedPrice = basePrice;

        calculatedPrice = applyLoyaltyDiscount(calculatedPrice, basePrice, monthsActive);
        calculatedPrice = applyNonProfitDiscount(calculatedPrice, command.isNonProfit());
        calculatedPrice = applyPromoCodeDiscount(calculatedPrice, command.promoCode(), today, monthsActive);
        calculatedPrice = applyOverdueFees(calculatedPrice, command.overdueDays());

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }

        return Money.of(calculatedPrice);
    }

    public int calculateMonthsActive(LocalDate signupDate, LocalDate referenceDate) {
        return (referenceDate.getYear() - signupDate.getYear()) * 12
                + (referenceDate.getMonthValue() - signupDate.getMonthValue());
    }

    public double applyLoyaltyDiscount(double currentPrice, double basePrice, int monthsActive) {
        if (monthsActive > 24) {
            return currentPrice - (basePrice * LOYALTY_DISCOUNT_TIER_2);
        } else if (monthsActive > 12) {
            return currentPrice - (basePrice * LOYALTY_DISCOUNT_TIER_1);
        }
        return currentPrice;
    }

    public double applyNonProfitDiscount(double currentPrice, boolean isNonProfit) {
        if (isNonProfit) {
            return currentPrice * NON_PROFIT_DISCOUNT_RATE;
        }
        return currentPrice;
    }

    public double applyPromoCodeDiscount(double currentPrice, String promoCode, LocalDate referenceDate, int monthsActive) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return currentPrice;
        }

        if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && referenceDate.getMonthValue() == 11) {
            return currentPrice - BLACK_FRIDAY_FIXED_DISCOUNT;
        } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
            return currentPrice * WELCOME_PROMO_RATE;
        }
        return currentPrice;
    }

    public double applyOverdueFees(double currentPrice, int overdueDays) {
        if (overdueDays > 0) {
            double penalty = currentPrice * OVERDUE_PENALTY_RATE;
            double dailyInterest = (currentPrice * OVERDUE_DAILY_INTEREST_RATE) * overdueDays;
            return currentPrice + penalty + dailyInterest;
        }
        return currentPrice;
    }
}
