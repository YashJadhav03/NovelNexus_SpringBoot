package com.bookStore.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookStore.entity.Book;
import com.bookStore.service.BookService;
import com.bookStore.service.GeminiService;

@Controller
public class EnhancedBookController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private GeminiService geminiService;
    
    /**
     * Advanced recommendations page
     */
    @GetMapping("/advanced-recommendations")
    public String showAdvancedRecommendations(Model model) {
        try {
            List<String> genres = bookService.getAllGenres();
            model.addAttribute("genres", genres);
            model.addAttribute("message", "Select a genre to get AI-powered recommendations");
            model.addAttribute("recommendedBooks", new ArrayList<Book>());
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load genres: " + e.getMessage());
            model.addAttribute("recommendedBooks", new ArrayList<Book>());
        }
        return "advancedRecommendations";
    }
    
    @PostMapping("/advanced-recommendations")
    public String getAdvancedRecommendations(@RequestParam("genre") String genre, Model model) {
        try {
            List<Book> books = bookService.getBooksByGenre(genre);
            if (books.isEmpty()) {
                model.addAttribute("error", "No books found for genre: " + genre);
                model.addAttribute("genres", bookService.getAllGenres());
                model.addAttribute("recommendedBooks", new ArrayList<Book>());
                return "advancedRecommendations";
            }
            
            String aiRecommendations = geminiService.getAdvancedRecommendations(books, genre);
            
            model.addAttribute("recommendedBooks", books);
            model.addAttribute("selectedGenre", genre);
            model.addAttribute("aiRecommendations", aiRecommendations);
            model.addAttribute("genres", bookService.getAllGenres());
            model.addAttribute("message", "AI recommendations generated successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error generating recommendations: " + e.getMessage());
            model.addAttribute("genres", bookService.getAllGenres());
            model.addAttribute("recommendedBooks", new ArrayList<Book>());
        }
        return "advancedRecommendations";
    }
    
    /**
     * Add recommended book to available books
     */
    @PostMapping("/add-recommended-book")
    public String addRecommendedBook(@RequestParam("bookName") String bookName,
                                   @RequestParam("author") String author,
                                   @RequestParam("genre") String genre,
                                   @RequestParam("description") String description,
                                   @RequestParam("price") String price,
                                   @RequestParam("rating") Double rating,
                                   Model model) {
        try {
            // Create new book from recommendation
            Book newBook = new Book();
            newBook.setName(bookName);
            newBook.setAuthor(author);
            newBook.setGenre(genre);
            newBook.setDescription(description);
            newBook.setPrice(price);
            newBook.setRating(rating);
            
            // Save the book
            bookService.save(newBook);
            
            model.addAttribute("message", "Book '" + bookName + "' added successfully to available books!");
            model.addAttribute("genres", bookService.getAllGenres());
            model.addAttribute("recommendedBooks", new ArrayList<Book>());
            
        } catch (Exception e) {
            model.addAttribute("error", "Error adding book: " + e.getMessage());
            model.addAttribute("genres", bookService.getAllGenres());
            model.addAttribute("recommendedBooks", new ArrayList<Book>());
        }
        return "advancedRecommendations";
    }
    
    /**
     * Content generation page
     */
    @GetMapping("/content-generation")
    public String showContentGeneration(Model model) {
        try {
            List<Book> books = bookService.getAllBook();
            model.addAttribute("books", books);
            model.addAttribute("message", "Select a book to generate AI content");
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load books: " + e.getMessage());
        }
        return "contentGeneration";
    }
    
    @PostMapping("/generate-description")
    public String generateDescription(@RequestParam("bookId") int bookId, Model model) {
        try {
            Book book = bookService.getBookById(bookId);
            String generatedDescription = geminiService.generateBookDescription(book);
            
            model.addAttribute("book", book);
            model.addAttribute("generatedDescription", generatedDescription);
            model.addAttribute("books", bookService.getAllBook());
            model.addAttribute("message", "Book description generated successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error generating description: " + e.getMessage());
            model.addAttribute("books", bookService.getAllBook());
        }
        return "contentGeneration";
    }
    
    @PostMapping("/generate-review")
    public String generateReview(@RequestParam("bookId") int bookId, Model model) {
        try {
            Book book = bookService.getBookById(bookId);
            String generatedReview = geminiService.generateBookReview(book);
            
            model.addAttribute("book", book);
            model.addAttribute("generatedReview", generatedReview);
            model.addAttribute("books", bookService.getAllBook());
            model.addAttribute("message", "Book review generated successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error generating review: " + e.getMessage());
            model.addAttribute("books", bookService.getAllBook());
        }
        return "contentGeneration";
    }
    
    /**
     * Smart search page
     */
    @GetMapping("/smart-search")
    public String showSmartSearch(Model model) {
        model.addAttribute("message", "Enter your search query for semantic book discovery");
        return "smartSearch";
    }
    
    @PostMapping("/semantic-search")
    public String performSemanticSearch(@RequestParam("query") String query, Model model) {
        try {
            if (query == null || query.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a search query");
                return "smartSearch";
            }
            
            List<Book> allBooks = bookService.getAllBook();
            if (allBooks.isEmpty()) {
                model.addAttribute("error", "No books available for search");
                return "smartSearch";
            }
            
            String searchResults = geminiService.performSemanticSearch(query, allBooks);
            
            model.addAttribute("query", query);
            model.addAttribute("searchResults", searchResults);
            model.addAttribute("message", "Search completed successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error performing search: " + e.getMessage());
            model.addAttribute("query", query);
        }
        return "smartSearch";
    }
    
    /**
     * Multi-language support page
     */
    @GetMapping("/multi-language")
    public String showMultiLanguage(Model model) {
        try {
            List<Book> books = bookService.getAllBook();
            model.addAttribute("books", books);
            model.addAttribute("message", "Select a book and language for translation");
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load books: " + e.getMessage());
        }
        return "multiLanguage";
    }
    
    @PostMapping("/translate-book")
    public String translateBook(@RequestParam("bookId") int bookId, 
                               @RequestParam("targetLanguage") String targetLanguage, 
                               Model model) {
        try {
            if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
                model.addAttribute("error", "Please select a target language");
                model.addAttribute("books", bookService.getAllBook());
                return "multiLanguage";
            }
            
            Book book = bookService.getBookById(bookId);
            String translatedContent = geminiService.translateBookContent(book, targetLanguage);
            
            model.addAttribute("book", book);
            model.addAttribute("targetLanguage", targetLanguage);
            model.addAttribute("translatedContent", translatedContent);
            model.addAttribute("books", bookService.getAllBook());
            model.addAttribute("message", "Translation completed successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error translating content: " + e.getMessage());
            model.addAttribute("books", bookService.getAllBook());
        }
        return "multiLanguage";
    }
    
    /**
     * Reading list generation
     */
    @GetMapping("/reading-lists")
    public String showReadingLists(Model model) {
        model.addAttribute("message", "Enter a theme to generate a curated reading list");
        return "readingLists";
    }
    
    @PostMapping("/generate-reading-list")
    public String generateReadingList(@RequestParam("theme") String theme, Model model) {
        try {
            if (theme == null || theme.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a theme");
                return "readingLists";
            }
            
            List<Book> allBooks = bookService.getAllBook();
            if (allBooks.isEmpty()) {
                model.addAttribute("error", "No books available for reading list generation");
                return "readingLists";
            }
            
            String readingList = geminiService.generateReadingList(theme, allBooks);
            
            model.addAttribute("theme", theme);
            model.addAttribute("readingList", readingList);
            model.addAttribute("message", "Reading list generated successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Error generating reading list: " + e.getMessage());
            model.addAttribute("theme", theme);
        }
        return "readingLists";
    }
} 