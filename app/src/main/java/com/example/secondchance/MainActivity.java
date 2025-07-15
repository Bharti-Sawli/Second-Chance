package com.example.secondchance;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.secondchance.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private ItemAdapter recommendedAdapter, recentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        setupWelcome();
        setupSearch();
        setupCategories();
        setupRecyclerViews();
        setupBottomNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupWelcome() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.child("users").child(user.getUid()).child("name")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().getValue() != null) {
                            binding.txtWelcome.setText(getString(R.string.welcome, task.getResult().getValue(String.class)));
                        } else {
                            binding.txtWelcome.setText(R.string.welcome_generic);
                        }
                    });
        }
    }

    private void setupSearch() {
        binding.searchIcon.setOnClickListener(v -> {
            String query = binding.inputSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, R.string.enter_search_query, Toast.LENGTH_SHORT).show();
            } else {
                searchListings(query);
            }
        });
    }

    private void setupCategories() {
        binding.categoryBooks.setOnClickListener(v -> filterListings("Books"));
        binding.categoryElectronics.setOnClickListener(v -> filterListings("Electronics"));
        binding.categoryClothing.setOnClickListener(v -> filterListings("Clothing"));
        binding.categoryOthers.setOnClickListener(v -> filterListings("Others"));
    }

    private void setupRecyclerViews() {
        binding.recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<Item> recommendedItems = new ArrayList<>();
        List<Item> recentItems = new ArrayList<>();

        recommendedAdapter = new ItemAdapter(recommendedItems, this);
        recentAdapter = new ItemAdapter(recentItems, this);

        binding.recyclerViewRecommended.setAdapter(recommendedAdapter);
        binding.recyclerViewRecent.setAdapter(recentAdapter);

        db.child("items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                recommendedItems.clear();
                recentItems.clear();
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    Item item = itemSnap.getValue(Item.class);
                    if (item != null) {
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
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.failed_to_load_items, Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Fetch error: ", error.toException());
            }
        });
    }

    private void searchListings(String query) {
        List<Item> results = new ArrayList<>();
        ItemAdapter searchAdapter = new ItemAdapter(results, this);
        binding.recyclerViewRecommended.setAdapter(searchAdapter);

        db.child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                results.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Item item = snap.getValue(Item.class);
                    if (item != null && item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                        results.add(item);
                    }
                }
                searchAdapter.notifyDataSetChanged();
                if (results.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.no_results, query), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, R.string.search_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterListings(String category) {
        List<Item> filteredItems = new ArrayList<>();
        ItemAdapter filterAdapter = new ItemAdapter(filteredItems, this);
        binding.recyclerViewRecommended.setAdapter(filterAdapter);

        db.child("items").orderByChild("category").equalTo(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                filteredItems.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Item item = snap.getValue(Item.class);
                    if (item != null) {
                        filteredItems.add(item);
                    }
                }
                filterAdapter.notifyDataSetChanged();
                if (filteredItems.isEmpty()) {
