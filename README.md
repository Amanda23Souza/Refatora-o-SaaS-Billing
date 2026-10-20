# Atividade: Refatoração de Software

## Descrição

Atividade prática da disciplina de Engenharia de Software / Refatoração (UFG).

**Arquivo Original do Professor:** `SubscriptionBillingService.java`  
**Período:** Inicia em 13/10/2026 às 00h00 e finaliza em 20/10/2026 às 17h59.

---

## Arquitetura e Proposta de Refatoração

Esta refatoração transforma o serviço monolítico de faturamento (`SubscriptionBillingService`) em uma arquitetura limpa, coesa e desacoplada, fundamentada nos princípios **SOLID**, **Clean Architecture (Ports & Adapters)** e no catálogo de padrões de Martin Fowler.

### 1. Code Smells Identificados e Soluções Aplicadas

| Code Smell Original | Problema Identificado | Técnica de Refatoração Aplicada | Princípio / Padrão |
| :--- | :--- | :--- | :--- |
| **God Method / Long Method** | O método `processMonthlyBilling` acumulava regras de cálculo de preços, consulta JDBC, validação de cartão, inserção de faturas, e-mail e logs. | **Extract Class** (`PriceCalculator`) e **Extract Method**. | **SRP** (*Single Responsibility Principle*) |
| **Primitive Obsession & Long Parameter List** | O método recebia 8 parâmetros primitivos soltos (`String`, `double`, `int`, etc.), além de manipular valores monetários como `double`. | **Introduce Parameter Object** (`BillingCommand` como `record`) e **Value Object** (`Money` com `BigDecimal`). | DDD, Coesão e Precisão Financeira |
| **Hardcoded Credentials & Magic Numbers** | Usuário, senha e URL JDBC expostos diretamente. Taxas e percentuais (`0.15`, `0.10`, `0.70`, `30.0`, `0.02`, `0.001`) soltos. | **Replace Magic Number with Symbolic Constant** e parametrização configurável por variáveis de ambiente. | Manutenibilidade e Segurança (CWE-798) |
| **Tight Coupling & DIP Violation** | Acoplamento direto com `DriverManager` e `SimpleEmail`, impedindo testes de unidade sem banco PostgreSQL e rede. | **Invert Dependencies** com interfaces/portas (`CustomerRepository`, `PaymentGateway`, `NotificationService`). | **DIP** (*Dependency Inversion Principle*) |
| **Dificuldade de Testabilidade Temporal** | Dependência direta de `LocalDate.now()` no meio do algoritmo, impossibilitando testes determinísticos para o cupom `BLACKFRIDAY` (novembro). | Injeção de `Clock` e desacoplamento da referência temporal nos cálculos. | Testabilidade Pura |
| **Quebra de Retrocompatibilidade** | Modernização de assinaturas sem preservar chamadores legados quebra contratos públicos de API. | **Preserve Whole Object / Facade**: sobrecarga do método legado com os 8 parâmetros primitivos delegando para o novo fluxo. | Retrocompatibilidade de API |
| **Perda de Resiliência Original** | A falha em chamadas de I/O não capturadas no serviço propagava exceções em vez de registrar erro fatal. | **Tratamento Defensivo**: captura de exceções de infraestrutura registrando log de erro fatal e garantindo resiliência. | Robustez Operacional |

---

### 2. Estrutura Modular de Pacotes

```text
src/
├── main/java/com/saas/billing/
│   ├── SubscriptionBillingService.java     # Orquestrador do fluxo e fachada legada
│   ├── domain/                             # Núcleo de regras puras de negócio
│   │   ├── BillingCommand.java             # Parameter Object imutável (record)
│   │   ├── Money.java                      # Value Object para precisão monetária
│   │   └── PriceCalculator.java            # Especialista puro no cálculo de preços (SRP)
│   ├── port/                               # Portas e abstrações (Hexagonal)
│   │   ├── CustomerRepository.java
│   │   ├── NotificationService.java
│   │   └── PaymentGateway.java
│   └── infra/                              # Adaptadores e implementações concretas
│       ├── JdbcCustomerRepository.java
│       ├── EmailNotificationService.java
│       ├── TokenPaymentGateway.java
│       └── RepositoryException.java
└── test/java/com/saas/billing/
    └── SubscriptionBillingServiceTest.java # Suíte JUnit 5 cobrindo 12 cenários
```

---

### 3. Como Compilar e Executar os Testes

O projeto utiliza o Apache Maven configurado com suporte para Java 21:

```bash
# Executar a suíte de testes unitários automatizados (12 cenários):
mvn test

# Compilar e empacotar o projeto:
mvn clean package
```
