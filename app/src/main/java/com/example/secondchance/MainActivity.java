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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private TextView txtWelcome;
    private EditText inputSearch;
    private ImageView searchIcon, profileIcon;
    private Button categoryBooks, categoryElectronics, categoryClothing, categoryOthers;
    private RecyclerView recyclerViewRecommended, recyclerViewRecent;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check network availability
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        txtWelcome = findViewById(R.id.txtWelcome);
        inputSearch = findViewById(R.id.inputSearch);
        searchIcon = findViewById(R.id.searchIcon);
        profileIcon = findViewById(R.id.profileIcon);
        categoryBooks = findViewById(R.id.categoryBooks);
        categoryElectronics = findViewById(R.id.categoryElectronics);
        categoryClothing = findViewById(R.id.categoryClothing);
        categoryOthers = findViewById(R.id.categoryOthers);
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Set welcome message
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.child("users").child(user.getUid()).child("name").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().getValue() != null) {
                    String name = task.getResult().getValue(String.class);
                    txtWelcome.setText("Welcome, " + name + "!");
                } else {
                    txtWelcome.setText("Welcome, User!");
                    Log.e("MainActivity", "Failed to fetch user name: " + task.getException());
                }
            });
        }

        // Set up search functionality
        searchIcon.setOnClickListener(v -> {
            String query = inputSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            } else {
                searchListings(query);
            }
        });

        // Set up category buttons
        categoryBooks.setOnClickListener(v -> filterListings("JEE"));
        categoryElectronics.setOnClickListener(v -> filterListings("NEET"));
        categoryClothing.setOnClickListener(v -> filterListings("UPSE"));
        categoryOthers.setOnClickListener(v -> filterListings("Others"));

        // Set up profile icon navigation
        profileIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            finish();
        });

        // Set up RecyclerViews
        setupRecyclerViews();

        // Set up bottom navigation
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                recreate();
                return true;
            } else if (itemId == R.id.nav_listings) {
                startActivity(new Intent(MainActivity.this, ListingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupRecyclerViews() {
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<Item> recommendedItems = new ArrayList<>();
        List<Item> recentItems = new ArrayList<>();
        ItemAdapter recommendedAdapter = new ItemAdapter(recommendedItems, MainActivity.this);
        ItemAdapter recentAdapter = new ItemAdapter(recentItems, MainActivity.this);
        recyclerViewRecommended.setAdapter(recommendedAdapter);
        recyclerViewRecent.setAdapter(recentAdapter);

        db.child("items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                recommendedItems.clear();
                recentItems.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID
                        }
                        if (recommendedItems.size() < 5) {
                            recommendedItems.add(item);
                        } else {
                            recentItems.add(item);
                        }
                    }
                }
                recommendedAdapter.notifyDataSetChanged();
                recentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to fetch items: ", databaseError.toException());
                Toast.makeText(MainActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterListings(String category) {
        List<Item> filteredItems = new ArrayList<>();
        ItemAdapter filteredAdapter = new ItemAdapter(filteredItems, MainActivity.this);
        recyclerViewRecommended.setAdapter(filteredAdapter);

        db.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                filteredItems.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null && item.getCategory().toLowerCase().contains(category.toLowerCase())) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID
                        }
                        filteredItems.add(item);
                    }
                }
                filteredAdapter.notifyDataSetChanged();
                if (filteredItems.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No items found in " + category, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to filter items: ", databaseError.toException());
                Toast.makeText(MainActivity.this, "Failed to filter items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchListings(String query) {
        List<Item> searchResults = new ArrayList<>();
        ItemAdapter searchAdapter = new ItemAdapter(searchResults, MainActivity.this);
        recyclerViewRecommended.setAdapter(searchAdapter);

        db.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                searchResults.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null && item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        if (item.getId() == null || item.getId().isEmpty()) {
                            item.setId(snapshot.getKey()); // Use Firebase key as ID
                        }
                        searchResults.add(item);
                    }
                }
                searchAdapter.notifyDataSetChanged();
                if (searchResults.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No items found for \"" + query + "\"", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to search items: ", databaseError.toException());
                Toast.makeText(MainActivity.this, "Failed to search items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static class Item {
        private String id;
        private String userId;
        private String title;
        private String author;
        private String category;
        private String price;
        private String condition;
        private String description;
        private String imageUrl1; // Updated to match ItemDetailsActivity
        private String imageUrl2; // Updated to match ItemDetailsActivity

        public Item() {
            this.id = UUID.randomUUID().toString(); // Default ID if none provided
        }

        public Item(String id, String userId, String title, String author, String category, String price, String condition, String description, String imageUrl1, String imageUrl2) {
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

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
        private List<Item> items;
        private final Context context;

        public ItemAdapter(List<Item> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_layout, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            Item item = items.get(position);
            holder.title.setText(item.getTitle() != null ? item.getTitle() : "N/A");
            holder.price.setText(item.getPrice() != null ? item.getPrice() : "₹0");

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
                Item itemClicked = items.get(position);
                Log.d("ItemAdapter", "Clicked item: " + itemClicked.getTitle() + ", ID: " + itemClicked.getId());
                if (itemClicked.getId() != null) {
                    Intent intent = new Intent(MainActivity.this, ItemDetailsActivity.class);
                    intent.putExtra("itemId", itemClicked.getId());
                    startActivity(intent);
                } else {
                    Log.e("ItemAdapter", "Item ID is null for: " + itemClicked.getTitle());
                    Toast.makeText(context, "Error: Invalid item selected", Toast.LENGTH_SHORT).show();
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