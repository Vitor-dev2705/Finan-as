package com.financasbot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Não foi encontrado");
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = EnvConfig.get("DB_URL");
        String user = EnvConfig.get("DB_USER");
        String pass = EnvConfig.get("DB_PASS");
        return DriverManager.getConnection(url, user, pass);
    }

}
