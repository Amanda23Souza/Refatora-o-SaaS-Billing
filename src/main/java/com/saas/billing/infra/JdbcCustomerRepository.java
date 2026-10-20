package com.saas.billing.infra;

import com.saas.billing.domain.Money;
import com.saas.billing.port.CustomerRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcCustomerRepository implements CustomerRepository {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;

    public JdbcCustomerRepository(String dbUrl, String dbUser, String dbPass) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
    }

    @Override
    public Optional<CustomerData> findById(String customerId) {
        String sql = "SELECT email, status FROM customers WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CustomerData(rs.getString("email"), rs.getString("status")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Falha ao buscar cliente: " + customerId, e);
        }
    }

    @Override
    public void saveInvoice(String customerId, Money amount, boolean paid) {
        String sql = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            stmt.setDouble(2, amount.toDouble());
            stmt.setString(3, paid ? "PAID" : "FAILED");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Falha ao salvar fatura de: " + customerId, e);
        }
    }
}