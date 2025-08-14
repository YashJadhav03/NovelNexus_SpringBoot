package com.bookStore.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class GeminiService {
    
    // Try multiple API versions for better compatibility
    private static final String[] GEMINI_ENDPOINTS = {
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent"
    };
    
    private final RestTemplate restTemplate;
    private long lastApiCall = 0;
    private static final long MIN_DELAY_BETWEEN_CALLS = 2000; // 2 seconds
    
    @Autowired
    private FallbackRecommendationService fallbackService;
    
    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.max.tokens:2048}")
    private int maxTokens;
    
    @Value("${gemini.temperature:0.7}")
    private double temperature;
    
    /**
     * Generate AI response using Gemini API with fallback endpoints and rate limiting
     */
    public String generateResponse(String prompt) {
        Exception lastException = null;
        
        for (String endpoint : GEMINI_ENDPOINTS) {
            try {
                // Add delay between API calls to respect rate limits
                long currentTime = System.currentTimeMillis();
                long timeSinceLastCall = currentTime - lastApiCall;
                if (timeSinceLastCall < MIN_DELAY_BETWEEN_CALLS) {
                    long delay = MIN_DELAY_BETWEEN_CALLS - timeSinceLastCall;
                    System.out.println("Waiting " + delay + "ms to respect rate limits...");
                    Thread.sleep(delay);
                }
                lastApiCall = System.currentTimeMillis();
                
                System.out.println("Trying Gemini endpoint: " + endpoint);
                String response = callGeminiAPI(endpoint, prompt);
                if (response != null && !response.contains("AI service is temporarily unavailable")) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                String errorMsg = e.getMessage();
                
                // Handle specific error types
                if (errorMsg.contains("429") || errorMsg.contains("quota")) {
                    System.err.println("Rate limit/quota exceeded for " + endpoint + ". Trying next endpoint...");
                    continue;
                } else if (errorMsg.contains("503") || errorMsg.contains("overloaded")) {
                    System.err.println("Model overloaded for " + endpoint + ". Trying next endpoint...");
                    continue;
                } else if (errorMsg.contains("404")) {
                    System.err.println("Model not found for " + endpoint + ". Trying next endpoint...");
                    continue;
                } else {
                    System.err.println("Failed with endpoint " + endpoint + ": " + errorMsg);
                    continue;
                }
            }
        }
        
        // If all endpoints fail, return helpful error message
        String errorMsg = "⚠️ Gemini API is currently unavailable due to:\n";
        if (lastException != null) {
            String lastError = lastException.getMessage();
            if (lastError.contains("429") || lastError.contains("quota")) {
                errorMsg += "• Rate limit exceeded (free tier quota reached)\n";
                errorMsg += "• Please wait a few minutes and try again\n";
                errorMsg += "• Or upgrade to a paid plan for higher limits";
            } else if (lastError.contains("503")) {
                errorMsg += "• Service temporarily overloaded\n";
                errorMsg += "• Please try again in a few minutes";
            } else {
                errorMsg += "• Technical issues with the API\n";
                errorMsg += "• Please try again later";
            }
        } else {
            errorMsg += "• Unknown error occurred\n";
            errorMsg += "• Please try again later";
        }
        
        return errorMsg;
    }
    
    /**
     * Call specific Gemini API endpoint
     */
    private String callGeminiAPI(String apiUrl, String prompt) {
        try {
            String url = apiUrl + "?key=" + apiKey;
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            ObjectNode contents = objectMapper.createObjectNode();
            ObjectNode parts = objectMapper.createObjectNode();
            parts.put("text", prompt);
            
            contents.putArray("parts").add(parts);
            requestBody.putArray("contents").add(contents);
            
            // Add generation config
            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("temperature", temperature);
            requestBody.set("generationConfig", generationConfig);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                    JsonNode candidate = root.get("candidates").get(0);
                    if (candidate.has("content") && candidate.get("content").has("parts")) {
                        JsonNode responseParts = candidate.get("content").get("parts");
                        if (responseParts.isArray() && responseParts.size() > 0) {
                            return responseParts.get(0).get("text").asText();
                        }
                    }
                }
            }
            
            System.err.println("Failed to get response from Gemini API: " + response.getBody());
            return "AI service is temporarily unavailable. Please try again later.";
            
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "AI service is temporarily unavailable. Please try again later.";
        }
    }
    
    /**
     * Get advanced book recommendations using Gemini with fallback
     */
    @Cacheable(value = "gemini-recommendations", key = "#genre")
    public String getAdvancedRecommendations(List<Book> books, String genre) {
        // Try AI recommendations first
        String aiResponse = generateResponse("Recommend 3-5 books for " + genre + " genre from this list:\n" +
            books.stream().limit(10).map(b -> b.getName() + " by " + b.getAuthor() + " (" + b.getGenre() + ")").collect(Collectors.joining("\n")) +
            "\n\nProvide brief, engaging recommendations with reasons why each book is perfect for " + genre + " readers.");
        
        // If AI fails, use fallback
        if (aiResponse.contains("⚠️") || aiResponse.contains("unavailable")) {
            return fallbackService.generateFallbackRecommendations(books, genre);
        }
        
        return aiResponse;
    }
    
    /**
     * Generate book description using Gemini with fallback
     */
    @Cacheable(value = "gemini-descriptions", key = "#book.id")
    public String generateBookDescription(Book book) {
        // Try AI description first
        String aiResponse = generateResponse("Generate a compelling book description for '" + book.getName() + "' by " + book.getAuthor() + ". Genre: " + book.getGenre() + ". Keep it between 150-200 words.");
        
        // If AI fails, use fallback
        if (aiResponse.contains("⚠️") || aiResponse.contains("unavailable")) {
            return fallbackService.generateSimpleDescription(book);
        }
        
        return aiResponse;
    }
    
    /**
     * Generate book review using Gemini
     */
    public String generateBookReview(Book book) {
        String prompt = String.format(
            "Write a professional book review for '%s' by %s. " +
            "Genre: %s. Rating: %s. " +
            "Include a brief plot summary, analysis of writing style, character development, " +
            "themes, strengths, weaknesses, and target audience. " +
            "Keep it balanced and informative.",
            book.getName(), book.getAuthor(), book.getGenre(), book.getRating());
        
        return generateResponse(prompt);
    }
    
    /**
     * Perform semantic search using Gemini
     */
    public String performSemanticSearch(String query, List<Book> books) {
        String bookList = books.stream()
                .map(b -> String.format("Title: %s, Author: %s, Genre: %s, Description: %s", 
                    b.getName(), b.getAuthor(), b.getGenre(), b.getDescription()))
                .collect(Collectors.joining("\n"));
        
        String prompt = String.format(
            "Perform semantic search for books related to: '%s'. " +
            "Available books:\n%s\n\n" +
            "Return the most semantically relevant books with relevance scores and explanations.",
            query, bookList);
        
        return generateResponse(prompt);
    }
    
    /**
     * Translate book content using Gemini
     */
    public String translateBookContent(Book book, String targetLanguage) {
        String prompt = String.format(
            "Translate the following book information to %s: " +
            "Title: %s, Author: %s, Description: %s. " +
            "Maintain the original meaning, style, and cultural context.",
            targetLanguage, book.getName(), book.getAuthor(), book.getDescription());
        
        return generateResponse(prompt);
    }
    
    /**
     * Generate reading list using Gemini
     */
    public String generateReadingList(String theme, List<Book> books) {
        String bookList = books.stream()
                .map(b -> String.format("Title: %s, Author: %s, Genre: %s", 
                    b.getName(), b.getAuthor(), b.getGenre()))
                .collect(Collectors.joining("\n"));
        
        String prompt = String.format(
            "Create a themed reading list for: '%s'. " +
            "Available books:\n%s\n\n" +
            "Select 5-7 books that fit the theme and provide a brief explanation for each selection.",
            theme, bookList);
        
        return generateResponse(prompt);
    }
} 