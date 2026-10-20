package com.saas.billing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.saas.billing.domain.BillingCommand;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                       PaymentGateway paymentGateway,
                                       NotificationService notificationService,
                                       Clock clock) {
        this.customerRepository = customerRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        double basePrice = command.basePrice().toDouble();
        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15); // 15% após 2 anos
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10); // 10% após 1 ano
        }

        if (command.isNonProfit()) {
            calculatedPrice = calculatedPrice * 0.70; // 30% off para ONGs
        }

        if (command.promoCode() != null && !command.promoCode().trim().isEmpty()) {
            if (command.promoCode().equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (command.promoCode().equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (command.overdueDays() > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * command.overdueDays();
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }

        Optional<CustomerRepository.CustomerData> customer = customerRepository.findById(command.customerId());
        if (customer.isEmpty()) {
            return logs;
        }

        if (!"ACTIVE".equalsIgnoreCase(customer.get().status())) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return logs;
        }

        boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), com.saas.billing.domain.Money.of(calculatedPrice));
        logs.add(paymentSuccess
                ? "Pagamento processado com sucesso: R$ " + calculatedPrice
                : "Falha no pagamento do cartão para: " + command.customerId());

        customerRepository.saveInvoice(command.customerId(), com.saas.billing.domain.Money.of(calculatedPrice), paymentSuccess);
        notificationService.sendInvoiceEmail(customer.get().email(), com.saas.billing.domain.Money.of(calculatedPrice));
        logs.add("E-mail disparado para: " + customer.get().email());

        return logs;
    }
}
