package com.bookStore.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.bookStore.entity.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiRecommendationService {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getAiRecommendations(List<Book> books, String genre) {
        String bookList = books.stream()
                .map(b -> String.format("Title: %s, Author: %s, Genre: %s, Description: %s, Rating: %.1f", b.getName(), b.getAuthor(), b.getGenre(), b.getDescription(), b.getRating() != null ? b.getRating() : 0.0))
                .collect(Collectors.joining("\n"));
        String prompt = "You are BookWiseAi, an expert book recommender. Based on the following list of books and the genre '" + genre + "', recommend the top 3 books. Respond in a short, user-friendly paragraph and list the recommended books with a brief reason for each.\n\n" + bookList;

        try {
            String requestBody = String.format("{\"model\":\"llama3\",\"prompt\":\"%s\"}", prompt.replace("\"", "\\\""));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(OLLAMA_API_URL, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("response")) {
                    return root.get("response").asText();
                }
            }
        } catch (Exception e) {
            return "AI recommendation service is unavailable. Please try again later.";
        }
        return "No recommendations available at this time.";
    }
} 