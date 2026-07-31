package com.financasbot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.financasbot.bot.TelegramVoiceBot;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO FINANÇAS BOT ===");

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramVoiceBot());
            System.out.println("🤖 Bot de Voz do Telegram rodando e aguardando mensagens!");
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar o bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}