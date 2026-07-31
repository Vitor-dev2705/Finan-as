package com.financasbot.service;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.financasbot.config.DatabaseConfig;
import com.financasbot.model.Transaction;

public class FinanceService {

    public boolean saveTransaction(Transaction transaction) {
        System.out.println("[SQL] Salvando transação...");
        String sql = "INSERT INTO transacoes (user_id, amount, category, \"date\") VALUES (?, ?, ?, ?)";

        try (var conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getUserId());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getCategory());
            stmt.setDate(4, Date.valueOf(transaction.getDate() != null ? transaction.getDate() : java.time.LocalDate.now()));

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[SQL] Erro ao salvar: " + e.getMessage());
            return false;
        }
    }

    public List<Transaction> getMonthlyTransactions(String userId, int month, int year) {
        System.out.println("[SQL] Buscando gastos do mês " + month + "/" + year);
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT id, user_id, amount, category, \"date\" FROM transacoes "
                + "WHERE user_id = ? "
                + "AND EXTRACT(MONTH FROM \"date\") = ? "
                + "AND EXTRACT(YEAR FROM \"date\") = ?";

        try (var conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setInt(2, month);
            stmt.setInt(3, year);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction();
                    t.setId(rs.getLong("id"));
                    t.setUserId(rs.getString("user_id"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setCategory(rs.getString("category"));

                    Date dbDate = rs.getDate("date");
                    if (dbDate != null) {
                        t.setDate(dbDate.toLocalDate());
                    }
                    transactions.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println("[SQL] Erro ao buscar: " + e.getMessage());
        }

        return transactions;
    }
}
