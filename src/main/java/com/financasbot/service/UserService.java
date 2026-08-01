package com.financasbot.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.financasbot.config.DatabaseConfig;
import com.financasbot.model.User;

public class UserService {

    /**
     * Busca um usuário pelo ID ou cadastra um novo perfil automaticamente se não existir.
     */
    public User getOrCreateUser(String userId, String name) {
        String selectSql = "SELECT id, name, email FROM usuarios WHERE id = ?";
        String insertSql = "INSERT INTO usuarios (id, name) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Tenta buscar o usuário
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(rs.getString("id"), rs.getString("name"), rs.getString("email"));
                    }
                }
            }

            // 2. Se não encontrou, insere o novo perfil
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, userId);
                stmt.setString(2, name);
                stmt.executeUpdate();
                System.out.println("[SQL] Novo perfil criado para o usuário: " + name + " (" + userId + ")");
                return new User(userId, name, null);
            }

        } catch (Exception e) {
            System.err.println("[SQL] Erro ao gerenciar perfil do usuário: " + e.getMessage());
            return null;
        }
    }
}