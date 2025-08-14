package com.bookStore.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bookStore.entity.Book;

@Service
public class FallbackRecommendationService {
    
    /**
     * Generate fallback recommendations when Gemini API is unavailable
     */
    public String generateFallbackRecommendations(List<Book> books, String genre) {
        if (books == null || books.isEmpty()) {
            return "No books available for recommendations.";
        }
        
        // Filter books by genre and get top 5
        List<Book> genreBooks = books.stream()
                .filter(book -> book.getGenre() != null && 
                               book.getGenre().toLowerCase().contains(genre.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
        
        if (genreBooks.isEmpty()) {
            // If no books in exact genre, get top rated books
            genreBooks = books.stream()
                    .sorted((b1, b2) -> {
                        Double rating1 = b1.getRating() != null ? b1.getRating() : 0.0;
                        Double rating2 = b2.getRating() != null ? b2.getRating() : 0.0;
                        return rating2.compareTo(rating1);
                    })
                    .limit(5)
                    .collect(Collectors.toList());
        }
        
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("📚 **NovelNexus Recommendations for ").append(genre).append("**\n\n");
        recommendations.append("Here are some great books you might enjoy:\n\n");
        
        for (int i = 0; i < genreBooks.size(); i++) {
            Book book = genreBooks.get(i);
            recommendations.append(i + 1).append(". **").append(book.getName()).append("** by ").append(book.getAuthor()).append("\n");
            recommendations.append("   Genre: ").append(book.getGenre()).append("\n");
            if (book.getRating() != null) {
                recommendations.append("   Rating: ").append(String.format("%.1f", book.getRating())).append("⭐\n");
            }
            if (book.getDescription() != null && !book.getDescription().isEmpty()) {
                String shortDesc = book.getDescription().length() > 100 ? 
                    book.getDescription().substring(0, 100) + "..." : 
                    book.getDescription();
                recommendations.append("   ").append(shortDesc).append("\n");
            }
            recommendations.append("\n");
        }
        
        recommendations.append("💡 *Note: These are basic recommendations. For AI-powered personalized suggestions, please try again when the Gemini API is available.*");
        
        return recommendations.toString();
    }
    
    /**
     * Generate a simple book description
     */
    public String generateSimpleDescription(Book book) {
        StringBuilder description = new StringBuilder();
        description.append("**").append(book.getName()).append("** by ").append(book.getAuthor()).append("\n\n");
        description.append("Genre: ").append(book.getGenre()).append("\n");
        if (book.getRating() != null) {
            description.append("Rating: ").append(String.format("%.1f", book.getRating())).append("⭐\n");
        }
        description.append("\n");
        
        if (book.getDescription() != null && !book.getDescription().isEmpty()) {
            description.append(book.getDescription());
        } else {
            description.append("A captivating ").append(book.getGenre().toLowerCase()).append(" novel that will keep you engaged from start to finish.");
        }
        
        return description.toString();
    }
} 