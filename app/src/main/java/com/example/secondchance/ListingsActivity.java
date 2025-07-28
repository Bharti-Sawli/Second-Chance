package com.example.secondchance;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListingsActivity extends AppCompatActivity {

    private static final String TAG = "ListingsActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private EditText inputSearch;
    private ImageView searchIcon, profileIcon;
    private Button categoryBooks, categoryElectronics, categoryClothing, categoryOthers;
    private TextView clearAllText; // Added for "Clear All" functionality
    private RecyclerView recyclerViewRecommended;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_listing);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: ", e);
            Toast.makeText(this, "Error loading layout", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check network availability
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Firebase
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseDatabase.getInstance().getReference();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: ", e);
            Toast.makeText(this, "Error initializing Firebase", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize UI elements
        try {
            inputSearch = findViewById(R.id.inputSearch);
            searchIcon = findViewById(R.id.searchIcon);
            profileIcon = findViewById(R.id.profileIcon);
            categoryBooks = findViewById(R.id.categoryBooks);
            categoryElectronics = findViewById(R.id.categoryElectronics);
            categoryClothing = findViewById(R.id.categoryClothing);
            categoryOthers = findViewById(R.id.categoryOthers);
            clearAllText = findViewById(R.id.clearAllText); // Initialize "Clear All" TextView
            recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
            bottomNavigation = findViewById(R.id.bottomNavigation);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize UI elements: ", e);
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up search functionality
        searchIcon.setOnClickListener(v -> {
            String query = inputSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(ListingsActivity.this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            } else {
                searchListings(query);
            }
        });

        // Set up category buttons
        categoryBooks.setOnClickListener(v -> filterListings("JEE"));
        categoryElectronics.setOnClickListener(v -> filterListings("NEET"));
        categoryClothing.setOnClickListener(v -> filterListings("UPSC"));
        categoryOthers.setOnClickListener(v -> filterListings("Others"));

        // Set up profile icon click
        profileIcon.setOnClickListener(v -> {
            try {
                startActivity(new Intent(ListingsActivity.this, ProfileActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to start ProfileActivity: ", e);
                Toast.makeText(ListingsActivity.this, "Error opening profile", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up "Clear All" click to recreate activity
        clearAllText.setOnClickListener(v -> {
            recreate(); // Recreate the activity to reset filters
        });

        // Set up RecyclerView
        setupRecyclerView();

        // Set up bottom navigation
        try {
            bottomNavigation.setSelectedItemId(R.id.nav_listings); // Highlight Listings tab
            bottomNavigation.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                try {
                    if (itemId == R.id.nav_home) {
                        startActivity(new Intent(ListingsActivity.this, MainActivity.class));
                        finish();
                        return true;
                    } else if (itemId == R.id.nav_listings) {
                        // Already on Listings
                        recreate();
                        return true;
                    } else if (itemId == R.id.nav_profile) {
                        startActivity(new Intent(ListingsActivity.this, ProfileActivity.class));
                        finish();
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    Log.e(TAG, "Navigation error: ", e);
                    Toast.makeText(ListingsActivity.this, "Navigation error", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up bottom navigation: ", e);
            Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            try {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start LoginActivity: ", e);
                Toast.makeText(this, "Error redirecting to login", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        // Set layout manager for vertical scrolling
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // Initialize adapter with empty list
        List<Item> recommendedItems = new ArrayList<>();
        ItemAdapter recommendedAdapter = new ItemAdapter(recommendedItems, ListingsActivity.this);
        recyclerViewRecommended.setAdapter(recommendedAdapter);

        // Fetch data from Firebase
        db.child("items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                recommendedItems.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID if not set
                        }
                        recommendedItems.add(item);
                    }
                }
                recommendedAdapter.notifyDataSetChanged();
                if (recommendedItems.isEmpty()) {
                    Toast.makeText(ListingsActivity.this, "No items available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch items: ", databaseError.toException());
                Toast.makeText(ListingsActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterListings(String category) {
        List<Item> filteredItems = new ArrayList<>();
        ItemAdapter filteredAdapter = new ItemAdapter(filteredItems, ListingsActivity.this);
        recyclerViewRecommended.setAdapter(filteredAdapter);

        db.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                filteredItems.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null && item.getTitle() != null && item.getTitle().toLowerCase().contains(category.toLowerCase())) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID if not set
                        }
                        filteredItems.add(item);
                    }
                }
                filteredAdapter.notifyDataSetChanged();
                if (filteredItems.isEmpty()) {
                    Toast.makeText(ListingsActivity.this, "No items found in " + category, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to filter items: ", databaseError.toException());
                Toast.makeText(ListingsActivity.this, "Failed to filter items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchListings(String query) {
        List<Item> searchResults = new ArrayList<>();
        ItemAdapter searchAdapter = new ItemAdapter(searchResults, ListingsActivity.this);
        recyclerViewRecommended.setAdapter(searchAdapter);

        db.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                searchResults.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null && item.getTitle() != null && item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID if not set
                        }
                        searchResults.add(item);
                    }
                }
                searchAdapter.notifyDataSetChanged();
                if (searchResults.isEmpty()) {
                    Toast.makeText(ListingsActivity.this, "No items found for \"" + query + "\"", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to search items: ", databaseError.toException());
                Toast.makeText(ListingsActivity.this, "Failed to search items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Network check failed: ", e);
            return false;
        }
    }

    // Item class for data model
    public static class Item {
        private String id;
        private String userId;
        private String title;
        private String author;
        private String category;
        private String price;
        private String condition;
        private String description;
        private String imageUrl1;
        private String imageUrl2;

        // Required empty constructor for Firebase
        public Item() {
            // Default constructor
        }

        public Item(String id, String userId, String title, String author, String category, String price,
                    String condition, String description, String imageUrl1, String imageUrl2) {
            this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
            this.userId = userId;
            this.title = title;
            this.author = author;
            this.category = category;
            this.price = price;
            this.condition = condition;
            this.description = description;
            this.imageUrl1 = imageUrl1;
            this.imageUrl2 = imageUrl2;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl1() { return imageUrl1; }
        public void setImageUrl1(String imageUrl1) { this.imageUrl1 = imageUrl1; }
        public String getImageUrl2() { return imageUrl2; }
        public void setImageUrl2(String imageUrl2) { this.imageUrl2 = imageUrl2; }
    }

    // RecyclerView adapter
    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private List<Item> items;
        private final Context context;

        public ItemAdapter(List<Item> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_listing_layout, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            Item item = items.get(position);
            holder.title.setText(item.getTitle() != null ? item.getTitle() : "No Title");
            holder.price.setText(item.getPrice() != null ? item.getPrice() : "No Price");

            // Load image using Glide (use imageUrl1, fallback to placeholder if null)
            if (item.getImageUrl1() != null && !item.getImageUrl1().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl1())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.placeholder_image);
            }

            holder.itemView.setOnClickListener(v -> {
                try {
                    Item itemClicked = items.get(position);
                    Log.d(TAG, "Clicked item: " + itemClicked.getTitle() + ", ID: " + itemClicked.getId());
                    if (itemClicked.getId() != null && !itemClicked.getId().isEmpty()) {
                        Intent intent = new Intent(ListingsActivity.this, ItemDetailsActivity.class);
                        intent.putExtra("itemId", itemClicked.getId());
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "Item ID is null or empty for: " + itemClicked.getTitle());
                        Toast.makeText(context, "Error: Invalid item selected", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start ItemDetailsActivity: ", e);
                    Toast.makeText(ListingsActivity.this, "Error opening item details", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, price;

            public ItemViewHolder(android.view.View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.itemImage);
                title = itemView.findViewById(R.id.itemTitle);
                price = itemView.findViewById(R.id.itemPrice);
            }
        }
    }
}