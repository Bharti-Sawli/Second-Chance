package com.example.secondchance;

public class Category {
    private String name;
    private int iconResId; // Use drawable resource for simplicity

    public Category(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
}