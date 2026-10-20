package com.saas.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(double factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public double toDouble() {
        return amount.doubleValue();
    }

    @Override
    public String toString() {
        return "R$ " + amount.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        return amount.equals(((Money) o).amount);
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }
}