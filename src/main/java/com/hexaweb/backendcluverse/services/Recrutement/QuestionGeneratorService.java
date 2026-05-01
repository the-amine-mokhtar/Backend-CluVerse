package com.hexaweb.backendcluverse.services.Recrutement;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionGeneratorService {

    private final GroqService groqService;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> generateQuestions(
            String clubName,
            String clubDescription,
            String campaignTitle,
            int questionCount,
            List<String> themes,
            String additionalInstructions) {

        String themesStr = String.join(", ", themes);
        String prompt = "Tu es un expert en recrutement pour clubs universitaires. " +
                "Génère exactement " + questionCount + " questions de candidature pour le club \"" + clubName + "\".\n\n" +
                "Description du club : " + clubDescription + "\n" +
                "Campagne : " + campaignTitle + "\n" +
                "Thèmes à couvrir : " + themesStr + "\n" +
                (additionalInstructions != null && !additionalInstructions.isEmpty() ?
                        "Instructions supplémentaires : " + additionalInstructions + "\n" : "") +
                "\nRÈGLES STRICTES :\n" +
                "- Génère EXACTEMENT " + questionCount + " questions\n" +
                "- Chaque question doit avoir un type parmi : TEXT, TEXTAREA, MULTIPLE_CHOICE\n" +
                "- Utilise MULTIPLE_CHOICE uniquement si tu fournis des options\n" +
                "- Les questions doivent être en français\n" +
                "- Les questions doivent sembler rédigées par un humain, naturelles et pertinentes\n\n" +
                "Retourne UNIQUEMENT ce JSON valide, sans markdown :\n" +
                "[\n" +
                "  {\n" +
                "    \"label\": \"Texte de la question\",\n" +
                "    \"type\": \"TEXT | TEXTAREA | MULTIPLE_CHOICE\",\n" +
                "    \"required\": true | false,\n" +
                "    \"options\": [\"Option1\", \"Option2\"] // seulement si MULTIPLE_CHOICE\n" +
                "  }\n" +
                "]";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "Tu es un expert en recrutement. Retourne UNIQUEMENT du JSON valide, sans markdown, sans explication."));
        messages.add(Map.of("role", "user", "content", prompt));

        Map<String, Object> response = groqService.chatRaw(messages, 0.7);

        try {
            String content = (String) response.get("content");
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
            }
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']');
            if (start >= 0 && end >= 0) {
                cleaned = cleaned.substring(start, end + 1);
            }
            return objectMapper.readValue(cleaned, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse generated questions: " + e.getMessage());
        }
    }
}