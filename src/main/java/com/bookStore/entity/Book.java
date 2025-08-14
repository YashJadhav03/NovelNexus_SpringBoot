package com.bookStore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Book {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int id;
	private String name;
	private String author;
	private String price;
    @Column(name = "genre")
    private String genre;
	private String description;
	private Double rating;
	@Column(name = "buying_link")
	private String buyingLink;
	public Book(int id, String name, String author, String price, String genre, String description, Double rating, String buyingLink) {
		super();
		this.id = id;
		this.name = name;
		this.author = author;
		this.price = price;
		this.genre = genre;
		this.description = description;
		this.rating = rating;
		this.buyingLink = buyingLink;
	}
	public Book() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Double getRating() {
		return rating;
	}
	public void setRating(Double rating) {
		this.rating = rating;
	}
	public String getBuyingLink() {
		return buyingLink;
	}
	public void setBuyingLink(String buyingLink) {
		this.buyingLink = buyingLink;
	}
	
}
