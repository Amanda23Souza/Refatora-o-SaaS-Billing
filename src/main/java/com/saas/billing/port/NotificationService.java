package com.saas.billing.port;

import com.saas.billing.domain.Money;

public interface NotificationService {
    void sendInvoiceEmail(String toEmail, Money amount);
}