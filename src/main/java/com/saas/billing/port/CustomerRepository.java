package com.saas.billing.port;

import com.saas.billing.domain.Money;
import java.util.Optional;

public interface CustomerRepository {
    Optional<CustomerData> findById(String customerId);
    void saveInvoice(String customerId, Money amount, boolean paid);

    record CustomerData(String email, String status) {}
}