package com.bookStore.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookStore.entity.Book;
import com.bookStore.repository.BookRepository;

@Service
public class BookService {
	
	@Autowired
	private BookRepository bRepo;
	
	public void save(Book b) {
		bRepo.save(b);
	}
	
	public List<Book> getAllBook(){
		return bRepo.findAll();
	}
	
	public Book getBookById(int id) {
		Optional<Book> book = bRepo.findById(id);
		if (book.isPresent()) {
			return book.get();
		} else {
			throw new RuntimeException("Book not found with ID: " + id);
		}
	}
	
	public void deleteById(int id) {
		bRepo.deleteById(id);
	}
	
	public List<Book> getBooksByGenre(String genre) {
		if (genre == null || genre.trim().isEmpty()) {
			return getAllBook();
		}
		return bRepo.findByGenreFlexible(genre);
	}
	
	public List<String> getAllGenres() {
		return bRepo.findAll().stream()
			.map(Book::getGenre)
			.filter(genre -> genre != null && !genre.trim().isEmpty())
			.distinct()
			.sorted()
			.toList();
	}
}
