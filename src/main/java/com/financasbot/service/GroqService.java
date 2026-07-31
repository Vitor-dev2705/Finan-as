package com.financasbot.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financasbot.config.EnvConfig;

public class GroqService {

    private final String apiKey = EnvConfig.get("GROQ_API_KEY");
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public record AudioParsedResult(Double amount, String category) {}

    public AudioParsedResult processAudio(byte[] audioBytes) {
        try {
            // Passo 1: Transcrever áudio usando Groq Whisper (whisper-large-v3)
            String transcribedText = transcribeAudioWithWhisper(audioBytes);
            if (transcribedText == null || transcribedText.isBlank()) {
                System.err.println("[Groq] Falha ao transcrever o áudio.");
                return null;
            }

            System.out.println("[Groq Whisper] Transcrição: " + transcribedText);

            // Passo 2: Extrair JSON (valor e categoria) usando Llama 3.3 (llama-3.3-70b-versatile)
            return parseTransactionWithLlama(transcribedText);

        } catch (Exception e) {
            System.err.println("[Groq] Erro ao processar áudio: " + e.getMessage());
            return null;
        }
    }

    private String transcribeAudioWithWhisper(byte[] audioBytes) throws Exception {
        String boundary = "---Boundary" + UUID.randomUUID().toString();
        
        // Constrói o multipart/form-data para o endpoint de áudio
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        sb.append("whisper-large-v3\r\n");
        
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.ogg\"\r\n");
        sb.append("Content-Type: audio/ogg\r\n\r\n");

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[headerBytes.length + audioBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(audioBytes, 0, body, headerBytes.length, audioBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + audioBytes.length, footerBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/audio/transcriptions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[Groq Whisper Error " + response.statusCode() + "]: " + response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("text").asText();
    }

    private AudioParsedResult parseTransactionWithLlama(String text) throws Exception {
        String systemPrompt = "Você é um assistente financeiro. Extraia o valor gasto e a categoria da mensagem."
                + " Responda APENAS um JSON válido no formato exato: {\"amount\": 45.50, \"category\": \"Alimentação\"}. Sem markdown, sem explicações.";

        // Montamos a estrutura usando Map/Jackson para evitar qualquer quebra no JSON enviado à API
        java.util.Map<String, Object> systemMessage = java.util.Map.of("role", "system", "content", systemPrompt);
        java.util.Map<String, Object> userMessage = java.util.Map.of("role", "user", "content", text);

        java.util.Map<String, Object> requestPayload = java.util.Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", java.util.List.of(systemMessage, userMessage),
                "temperature", 0.1
        );

        String jsonPayload = mapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[Groq Llama Error " + response.statusCode() + "]: " + response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());
        String content = root.path("choices").get(0).path("message").path("content").asText().trim();

        // Limpa potenciais blocos de código markdown que o LLM possa retornar (ex: ```json ... ```)
        content = content.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                         .replaceAll("(?s)```\\s*(.*?)\\s*```", "$1")
                         .trim();

        JsonNode parsedJson = mapper.readTree(content);
        Double amount = parsedJson.path("amount").asDouble();
        String category = parsedJson.path("category").asText("Outros");

        return new AudioParsedResult(amount, category);
    }
}