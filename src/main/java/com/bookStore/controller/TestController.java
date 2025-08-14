package com.bookStore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookStore.service.GeminiService;

@RestController
public class TestController {
    
    @Autowired
    private GeminiService geminiService;
    
    @GetMapping("/test-gemini")
    public String testGemini() {
        try {
            String response = geminiService.generateResponse("Hello, this is a test. Please respond with 'NovelNexus is working!'");
            return "✅ Gemini API Test Result: " + response;
        } catch (Exception e) {
            return "❌ Gemini API Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-gemini-simple")
    public String testGeminiSimple() {
        try {
            String response = geminiService.generateResponse("Say 'Hello from NovelNexus'");
            return "Simple Test: " + response;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/test-gemini-status")
    public String testGeminiStatus() {
        return "🔍 Gemini Service Status Check:\n" +
               "- Service initialized: " + (geminiService != null ? "✅" : "❌") + "\n" +
               "- Ready for testing";
    }
} 