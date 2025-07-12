package com.example.secondchance;

public class Book {
    private String id, title, author, imageUrl, condition, category, sellerId;
    private double price;

    public Book() {} // Needed for Firestore

    public Book(String id, String title, String author, String imageUrl, String condition, String category, String sellerId, double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.category = category;
        this.sellerId = sellerId;
        this.price = price;
    }

    // Getters and setters...
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getImageUrl() { return imageUrl; }
    public String getCondition() { return condition; }
    public String getCategory() { return category; }
    public String getSellerId() { return sellerId; }
    public double getPrice() { return price; }
}