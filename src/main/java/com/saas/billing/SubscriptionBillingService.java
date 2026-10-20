package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PriceCalculator;
import com.saas.billing.infra.EmailNotificationService;
import com.saas.billing.infra.JdbcCustomerRepository;
import com.saas.billing.infra.TokenPaymentGateway;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;

/**
 * Servico central de faturamento de assinaturas.
 *
 * Refatoracoes aplicadas:
 * - SRP: Delega o calculo de precos e aplicacao de descontos/juros para PriceCalculator.
 * - DIP: Depende de abstracoes (CustomerRepository, PaymentGateway, NotificationService, Clock).
 * - Parameter Object: Agrupa parametros soltos em BillingCommand.
 * - Facade / Backward Compatibility: Mantem metodo legado com parametros primitivos e construtor default.
 * - Resiliencia: Trata falhas de infraestrutura registrando logs de erro fatal sem propagar exceptions.
 */
public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;
    private final PriceCalculator priceCalculator;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                       PaymentGateway paymentGateway,
                                       NotificationService notificationService,
                                       Clock clock,
                                       PriceCalculator priceCalculator) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository nao pode ser nulo");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway nao pode ser nulo");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService nao pode ser nulo");
        this.clock = Objects.requireNonNull(clock, "clock nao pode ser nulo");
        this.priceCalculator = Objects.requireNonNull(priceCalculator, "priceCalculator nao pode ser nulo");
    }

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                       PaymentGateway paymentGateway,
                                       NotificationService notificationService,
                                       Clock clock) {
        this(customerRepository, paymentGateway, notificationService, clock, new PriceCalculator());
    }

    /**
     * Construtor de conveniencia com implementacoes padrao de infraestrutura.
     */
    public SubscriptionBillingService() {
        this(
            new JdbcCustomerRepository(),
            new TokenPaymentGateway(),
            new EmailNotificationService(),
            Clock.systemDefaultZone(),
            new PriceCalculator()
        );
    }

    /**
     * Metodo de fachada com a assinatura original (retrocompatibilidade).
     */
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
                                             String signupDateStr, int overdueDays, String promoCode,
                                             boolean isNonProfit, String paymentCardToken) {
        try {
            BillingCommand command = new BillingCommand(
                customerId,
                planType,
                Money.of(basePrice),
                LocalDate.parse(signupDateStr),
                overdueDays,
                promoCode,
                isNonProfit,
                paymentCardToken
            );
            return processMonthlyBilling(command);
        } catch (Exception e) {
            List<String> logs = new ArrayList<>();
            logs.add("Erro fatal no processamento: " + e.getMessage());
            return logs;
        }
    }

    /**
     * Metodo principal de processamento utilizando o Parameter Object BillingCommand.
     */
    public List<String> processMonthlyBilling(BillingCommand command) {
        List<String> logs = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now(clock);
            Money calculatedPrice = priceCalculator.calculateFinalPrice(command, today);

            Optional<CustomerRepository.CustomerData> customer = customerRepository.findById(command.customerId());
            if (customer.isEmpty()) {
                return logs;
            }

            if (!"ACTIVE".equalsIgnoreCase(customer.get().status())) {
                logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                return logs;
            }

            boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), calculatedPrice);
            logs.add(paymentSuccess
                    ? "Pagamento processado com sucesso: R$ " + calculatedPrice.toDouble()
                    : "Falha no pagamento do cartão para: " + command.customerId());

            customerRepository.saveInvoice(command.customerId(), calculatedPrice, paymentSuccess);
            notificationService.sendInvoiceEmail(customer.get().email(), calculatedPrice);
            logs.add("E-mail disparado para: " + customer.get().email());

        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
