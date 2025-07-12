package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.secondchance.adapter.BookAdapter;
import com.example.secondchance.adapter.CategoryAdapter;
import com.example.secondchance.model.Book;
import com.example.secondchance.model.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView rvBooks, rvCategories;
    private BookAdapter bookAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Book> bookList = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvBooks = findViewById(R.id.rvBooks);
        rvCategories = findViewById(R.id.rvCategories);
        searchView = findViewById(R.id.searchView);

        // Setup Book RecyclerView
        bookAdapter = new BookAdapter(this, bookList);
        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        rvBooks.setAdapter(bookAdapter);

        // Setup Category RecyclerView
        categoryList = getCategories();
        categoryAdapter = new CategoryAdapter(categoryList, category -> filterBooksByCategory(category.getName()));
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Fetch books from Firestore
        fetchBooksFromFirestore();

        // Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBooks(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterBooks(newText);
                return true;
            }
        });

        // FloatingActionButton to add book
        FloatingActionButton fab = findViewById(R.id.fabAddBook);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, AddBookActivity.class));
        });

        // BottomNavigationView setup (implement as needed)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        // bottomNav.setOnNavigationItemSelectedListener(...);
    }

    private void fetchBooksFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("books")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Book book = doc.toObject(Book.class);
                        bookList.add(book);
                    }
                    bookAdapter.setBooks(bookList);
                });
    }

    private void filterBooks(String query) {
        List<Book> filtered = new ArrayList<>();
        for (Book book : bookList) {
            if (book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(book);
            }
        }
        bookAdapter.setBooks(filtered);
    }

    private void filterBooksByCategory(String category) {
        if (category.equals("All")) {
            bookAdapter.setBooks(bookList);
            return;
        }
        List<Book> filtered = new ArrayList<>();
        for (Book book : bookList) {
            if (book.getCategory().equalsIgnoreCase(category)) {
                filtered.add(book);
            }
        }
        bookAdapter.setBooks(filtered);
    }

    private List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("All", R.drawable.ic_category_placeholder));
        categories.add(new Category("Fiction", R.drawable.ic_category_placeholder));
        categories.add(new Category("Non-fiction", R.drawable.ic_category_placeholder));
        categories.add(new Category("Academic", R.drawable.ic_category_placeholder));
        categories.add(new Category("Comics", R.drawable.ic_category_placeholder));

        // Add more categories and icons as needed
        return categories;
    }
}