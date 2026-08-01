package com.financasbot;

// Importações do Servidor HTTP do Java Nativo
import java.net.InetSocketAddress;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.financasbot.bot.TelegramVoiceBot;
import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO FINANÇAS BOT ===");

        // Inicia o servidor HTTP simples para satisfazer o Health Check do Render
        try {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exchange -> {
                String response = "Bot ativo e rodando!";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            });
            server.start();
            System.out.println("🌐 Servidor de Health Check rodando na porta: " + port);
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível iniciar o servidor HTTP: " + e.getMessage());
        }

        // Inicia o Bot do Telegram
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramVoiceBot());
            System.out.println(" Bot de Voz do Telegram rodando e aguardando mensagens!");
        } catch (Exception e) {
            System.err.println(" Erro ao iniciar o bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}