package com.saas.billing.infra;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationService;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;

public class EmailNotificationService implements NotificationService {
    @Override
    public void sendInvoiceEmail(String toEmail, Money amount) {
        try {
            SimpleEmail emailSender = new SimpleEmail();
            emailSender.setHostName("smtp.empresa.com");
            emailSender.setSmtpPort(587);
            emailSender.setFrom("billing@empresa.com");
            emailSender.setSubject("Sua Fatura Mensal");
            emailSender.setMsg("Olá! Sua fatura foi processada no valor de " + amount);
            emailSender.addTo(toEmail);
            // emailSender.send();
        } catch (EmailException e) {
            throw new RepositoryException("Falha ao enviar e-mail para: " + toEmail, e);
        }
    }
}