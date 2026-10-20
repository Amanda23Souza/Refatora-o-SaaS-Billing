package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PriceCalculator;
import com.saas.billing.infra.RepositoryException;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;

/**
 * Suite de testes unitarios de SubscriptionBillingService e PriceCalculator.
 * Compativel tanto com execucao automatica via JUnit 5 (mvn test)
 * quanto via standalone main (java -cp).
 */
public class SubscriptionBillingServiceTest {

    public static void main(String[] args) {
        System.out.println(">>> Executando Testes de Unidade de SubscriptionBillingService via Standalone Runner...");

        SubscriptionBillingServiceTest suite = new SubscriptionBillingServiceTest();
        suite.testCalculoFidelidade12Meses();
        suite.testCalculoFidelidade24Meses();
        suite.testDescontoOng();
        suite.testCupomBlackFridayEmNovembro();
        suite.testCupomBlackFridayForaDeNovembro();
        suite.testCupomWelcomeParaClienteNovo();
        suite.testMultaEJurosPorAtraso();
        suite.testClienteInativo();
        suite.testClienteNaoEncontrado();
        suite.testFalhaNoCartao();
        suite.testSucessoNoPagamentoECompatibilidadeFachada();
        suite.testErroFatalDeInfraestruturaCapturado();

        System.out.println(">>> TODOS OS TESTES PASSARAM COM SUCESSO! (12/12)");
    }

    private static Clock createFixedClock(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
    }

    // =========================================================================
    // TESTES DE CALCULO DE PRECO (PriceCalculator)
    // =========================================================================

    @Test
    @DisplayName("Fidelidade acima de 12 meses deve conceder 10% de desconto")
    public void testCalculoFidelidade12Meses() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20);
        LocalDate signupDate = LocalDate.of(2025, 9, 20); // 13 meses

        BillingCommand command = new BillingCommand(
            "cust_1", "PRO", Money.of(100.0), signupDate, 0, null, false, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(90.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Fidelidade acima de 24 meses deve conceder 15% de desconto")
    public void testCalculoFidelidade24Meses() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20);
        LocalDate signupDate = LocalDate.of(2024, 9, 20); // 25 meses

        BillingCommand command = new BillingCommand(
            "cust_2", "PRO", Money.of(100.0), signupDate, 0, null, false, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(85.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Entidade sem fins lucrativos (ONG) deve receber 30% de desconto")
    public void testDescontoOng() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20);
        LocalDate signupDate = LocalDate.of(2026, 10, 1);

        BillingCommand command = new BillingCommand(
            "cust_3", "PRO", Money.of(100.0), signupDate, 0, null, true, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(70.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Cupom BLACKFRIDAY em novembro deve conceder R$ 30 fixos de desconto")
    public void testCupomBlackFridayEmNovembro() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 11, 25); // Novembro
        LocalDate signupDate = LocalDate.of(2026, 10, 1);

        BillingCommand command = new BillingCommand(
            "cust_4", "PRO", Money.of(100.0), signupDate, 0, "BLACKFRIDAY", false, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(70.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Cupom BLACKFRIDAY fora de novembro nao deve ter efeito")
    public void testCupomBlackFridayForaDeNovembro() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20); // Outubro
        LocalDate signupDate = LocalDate.of(2026, 10, 1);

        BillingCommand command = new BillingCommand(
            "cust_5", "PRO", Money.of(100.0), signupDate, 0, "BLACKFRIDAY", false, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(100.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Cupom WELCOME10 deve conceder 10% para cliente recente")
    public void testCupomWelcomeParaClienteNovo() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20);
        LocalDate signupDate = LocalDate.of(2026, 10, 1); // 0 meses

        BillingCommand command = new BillingCommand(
            "cust_6", "PRO", Money.of(100.0), signupDate, 0, "WELCOME10", false, "tok_123"
        );

        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(90.0, price.toDouble(), 0.001);
    }

    @Test
    @DisplayName("Multa e juros devem ser calculados corretamente para dias de atraso")
    public void testMultaEJurosPorAtraso() {
        PriceCalculator calculator = new PriceCalculator();
        LocalDate referenceDate = LocalDate.of(2026, 10, 20);
        LocalDate signupDate = LocalDate.of(2026, 10, 1);

        BillingCommand command = new BillingCommand(
            "cust_7", "PRO", Money.of(100.0), signupDate, 10, null, false, "tok_123"
        );

        // 100 + 2% (2.0) + (100 * 0.001 * 10 = 1.0) = 103.0
        Money price = calculator.calculateFinalPrice(command, referenceDate);
        assertEquals(103.0, price.toDouble(), 0.001);
    }

    // =========================================================================
    // TESTES DE FLUXO DO SERVICO (SubscriptionBillingService)
    // =========================================================================

    @Test
    @DisplayName("Cliente inativo deve cancelar a cobranca sem faturar")
    public void testClienteInativo() {
        MockCustomerRepository repo = new MockCustomerRepository();
        repo.customers.put("cust_inativo", new CustomerRepository.CustomerData("inactive@empresa.com", "SUSPENDED"));

        Clock clock = createFixedClock(LocalDate.of(2026, 10, 20));
        SubscriptionBillingService service = new SubscriptionBillingService(
            repo, new MockPaymentGateway(), new MockNotificationService(), clock
        );

        BillingCommand command = new BillingCommand(
            "cust_inativo", "PRO", Money.of(100.0), LocalDate.of(2026, 1, 1), 0, null, false, "tok_123"
        );

        List<String> logs = service.processMonthlyBilling(command);
        assertTrue(logs.contains("Cliente inativo. Cobrança cancelada: cust_inativo"));
        assertEquals(0, repo.savedInvoices.size());
    }

    @Test
    @DisplayName("Cliente inexistente deve retornar logs vazios")
    public void testClienteNaoEncontrado() {
        MockCustomerRepository repo = new MockCustomerRepository();
        Clock clock = createFixedClock(LocalDate.of(2026, 10, 20));
        SubscriptionBillingService service = new SubscriptionBillingService(
            repo, new MockPaymentGateway(), new MockNotificationService(), clock
        );

        BillingCommand command = new BillingCommand(
            "cust_inexistente", "PRO", Money.of(100.0), LocalDate.of(2026, 1, 1), 0, null, false, "tok_123"
        );

        List<String> logs = service.processMonthlyBilling(command);
        assertTrue(logs.isEmpty());
    }

    @Test
    @DisplayName("Falha no pagamento deve registrar fatura com status nao pago")
    public void testFalhaNoCartao() {
        MockCustomerRepository repo = new MockCustomerRepository();
        repo.customers.put("cust_falha", new CustomerRepository.CustomerData("falha@empresa.com", "ACTIVE"));

        MockPaymentGateway gateway = new MockPaymentGateway();
        gateway.shouldSucceed = false;

        Clock clock = createFixedClock(LocalDate.of(2026, 10, 20));
        SubscriptionBillingService service = new SubscriptionBillingService(
            repo, gateway, new MockNotificationService(), clock
        );

        BillingCommand command = new BillingCommand(
            "cust_falha", "PRO", Money.of(100.0), LocalDate.of(2026, 1, 1), 0, null, false, "tok_invalido"
        );

        List<String> logs = service.processMonthlyBilling(command);
        assertTrue(logs.contains("Falha no pagamento do cartão para: cust_falha"));
        assertEquals(1, repo.savedInvoices.size());
        assertFalse(repo.savedInvoices.get(0).paid());
    }

    @Test
    @DisplayName("Sucesso no pagamento deve gerar fatura paga e disparar notificacao")
    public void testSucessoNoPagamentoECompatibilidadeFachada() {
        MockCustomerRepository repo = new MockCustomerRepository();
        repo.customers.put("cust_ok", new CustomerRepository.CustomerData("ok@empresa.com", "ACTIVE"));

        MockPaymentGateway gateway = new MockPaymentGateway();
        gateway.shouldSucceed = true;

        MockNotificationService notifications = new MockNotificationService();
        Clock clock = createFixedClock(LocalDate.of(2026, 10, 20));

        SubscriptionBillingService service = new SubscriptionBillingService(
            repo, gateway, notifications, clock
        );

        // Testando metodo de fachada legado
        List<String> logs = service.processMonthlyBilling(
            "cust_ok", "PRO", 100.0, "2026-10-01", 0, null, false, "tok_valido"
        );

        assertTrue(logs.contains("Pagamento processado com sucesso: R$ 100.0"));
        assertTrue(logs.contains("E-mail disparado para: ok@empresa.com"));
        assertEquals(1, repo.savedInvoices.size());
        assertTrue(repo.savedInvoices.get(0).paid());
        assertEquals(1, notifications.sentEmails.size());
    }

    @Test
    @DisplayName("Falha de infraestrutura deve registrar log de erro fatal sem quebrar a aplicacao")
    public void testErroFatalDeInfraestruturaCapturado() {
        CustomerRepository failingRepo = new CustomerRepository() {
            @Override
            public Optional<CustomerData> findById(String customerId) {
                throw new RepositoryException("Conexao com o banco recusada", new RuntimeException());
            }

            @Override
            public void saveInvoice(String customerId, Money amount, boolean paid) {}
        };

        Clock clock = createFixedClock(LocalDate.of(2026, 10, 20));
        SubscriptionBillingService service = new SubscriptionBillingService(
            failingRepo, new MockPaymentGateway(), new MockNotificationService(), clock
        );

        BillingCommand command = new BillingCommand(
            "cust_db_down", "PRO", Money.of(100.0), LocalDate.of(2026, 1, 1), 0, null, false, "tok_123"
        );

        List<String> logs = service.processMonthlyBilling(command);
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).startsWith("Erro fatal no processamento: Conexao com o banco recusada"));
    }

    // =========================================================================
    // MOCKS PARA TESTES UNITARIOS PURISTAS
    // =========================================================================

    private static class MockCustomerRepository implements CustomerRepository {
        final Map<String, CustomerData> customers = new HashMap<>();
        final List<SavedInvoice> savedInvoices = new ArrayList<>();

        record SavedInvoice(String customerId, Money amount, boolean paid) {}

        @Override
        public Optional<CustomerData> findById(String customerId) {
            return Optional.ofNullable(customers.get(customerId));
        }

        @Override
        public void saveInvoice(String customerId, Money amount, boolean paid) {
            savedInvoices.add(new SavedInvoice(customerId, amount, paid));
        }
    }

    private static class MockPaymentGateway implements PaymentGateway {
        boolean shouldSucceed = true;

        @Override
        public boolean charge(String cardToken, Money amount) {
            return shouldSucceed && cardToken != null && cardToken.startsWith("tok_");
        }
    }

    private static class MockNotificationService implements NotificationService {
        final List<String> sentEmails = new ArrayList<>();

        @Override
        public void sendInvoiceEmail(String toEmail, Money amount) {
            sentEmails.add(toEmail + ": " + amount);
        }
    }
}
