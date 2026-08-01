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
import com.financasbot.model.User;
import com.financasbot.service.FinanceService;
import com.financasbot.service.GroqService;
import com.financasbot.service.UserService;

public class TelegramVoiceBot extends TelegramLongPollingBot {

    private final GroqService groqService = new GroqService();
    private final FinanceService financeService = new FinanceService();
    private final UserService userService = new UserService();

    @Override
    public String getBotUsername() {
        return "financasbot";
    }

    @Override
    public String getBotToken() {
        return EnvConfig.get("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasVoice()) {
            String chatId = update.getMessage().getChatId().toString();
            String firstName = update.getMessage().getFrom().getFirstName();

            try {
                // 1. Extrai o objeto Voice da mensagem
                Voice voice = update.getMessage().getVoice();

                // Envia aviso ao usuário de que está processando
                sendTextMessage(chatId, "Processando seu áudio...");

                // 2. Baixa o arquivo de áudio do servidor do Telegram
                GetFile getFile = new GetFile();
                getFile.setFileId(voice.getFileId());
                File file = execute(getFile);

                byte[] audioBytes;
                try (InputStream is = downloadFileAsStream(file)) {
                    audioBytes = is.readAllBytes();
                }

                // 3. Garante/Cria o perfil do usuário no banco
                User user = userService.getOrCreateUser(chatId, firstName);

                // 4. Processa o áudio via Groq
                GroqService.AudioParsedResult result = groqService.processAudio(audioBytes);

                if (result != null && result.amount() != null) {
                    Transaction transaction = new Transaction();
                    // Associa o ID do perfil à transação
                    transaction.setUserId(user.getId());
                    transaction.setAmount(result.amount());
                    transaction.setCategory(result.category());
                    transaction.setDate(LocalDate.now());

                    boolean saved = financeService.saveTransaction(transaction);

                    if (saved) {
                        String msg = String.format(" Gasto registrado %s!\n **Valor:** R$ %.2f\n **Categoria:** %s",
                                user.getName(), result.amount(), result.category());
                        sendTextMessage(chatId, msg);
                    } else {
                        sendTextMessage(chatId, " Erro ao salvar transação no banco de dados.");
                    }
                } else {
                    sendTextMessage(chatId, " Não consegui entender a quantia ou a categoria no áudio.");
                }

            } catch (Exception e) {
                System.err.println("[Bot Error]: " + e.getMessage());
                sendTextMessage(chatId, " Erro ao processar o áudio.");
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