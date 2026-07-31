package com.financasbot.bot;

import java.io.InputStream;
import java.time.LocalDate;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;

import com.financasbot.config.EnvConfig;
import com.financasbot.model.Transaction;
import com.financasbot.service.FinanceService;
import com.financasbot.service.GroqService;

public class TelegramVoiceBot extends TelegramLongPollingBot {

    private final GroqService groqService = new GroqService();
    private final FinanceService financeService = new FinanceService();

    @Override
    public String getBotUsername() {
        return "SeuBotUsername"; // ajuste para o username do seu bot se necessário
    }

    @Override
    public String getBotToken() {
        return EnvConfig.get("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasVoice()) {
            String chatId = update.getMessage().getChatId().toString();
            Voice voice = update.getMessage().getVoice();

            try {
                // Envia aviso ao usuário de que está processando
                sendTextMessage(chatId, "🎧 Processando áudio via Groq (Whisper + Llama 3.3)...");

                // Baixa o arquivo de áudio
                GetFile getFile = new GetFile();
                getFile.setFileId(voice.getFileId());
                File file = execute(getFile);
                
                byte[] audioBytes;
                try (InputStream is = downloadFileAsStream(file)) {
                    audioBytes = is.readAllBytes();
                }

                // Processa o áudio na Groq
                GroqService.AudioParsedResult result = groqService.processAudio(audioBytes);

                if (result != null && result.amount() != null) {
                    Transaction transaction = new Transaction();
                    transaction.setUserId(chatId);
                    transaction.setAmount(result.amount());
                    transaction.setCategory(result.category());
                    transaction.setDate(LocalDate.now());

                    boolean saved = financeService.saveTransaction(transaction);

                    if (saved) {
                        String msg = String.format("✅ Gastos registrados!\n💰 **Valor:** R$ %.2f\n🏷️ **Categoria:** %s", 
                                result.amount(), result.category());
                        sendTextMessage(chatId, msg);
                    } else {
                        sendTextMessage(chatId, "⚠️ Erro ao salvar transação no banco de dados.");
                    }
                } else {
                    sendTextMessage(chatId, "❌ Não consegui entender a quantia ou a categoria no áudio.");
                }

            } catch (Exception e) {
                System.err.println("[Bot Error]: " + e.getMessage());
                sendTextMessage(chatId, "❌ Erro ao processar o áudio.");
            }
        }
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (Exception e) {
            System.err.println("[Bot Error - SendMessage]: " + e.getMessage());
        }
    }
}