package com.saas.billing.infra;

import com.saas.billing.domain.Money;
import com.saas.billing.port.PaymentGateway;

public class TokenPaymentGateway implements PaymentGateway {
    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith("tok_");
    }
}