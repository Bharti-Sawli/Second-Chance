package com.example.secondchance;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class BookSoldActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookOrderAdapter adapter;
    private List<BuyNowActivity.OrderData> soldBooks = new ArrayList<>();
    private DatabaseReference ordersRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books_sold_activity);

        Toolbar toolbar = findViewById(R.id.toolbarSold);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Books Sold");
        }

        recyclerView = findViewById(R.id.recyclerViewSoldBooks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookOrderAdapter(soldBooks, this);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        if (currentUser != null) {
            fetchSoldBooks();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchSoldBooks() {
        ordersRef.orderByChild("sellerUserId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        soldBooks.clear();
                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            BuyNowActivity.OrderData order = orderSnap.getValue(BuyNowActivity.OrderData.class);
                            if (order != null) soldBooks.add(order);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BookSoldActivity.this, "Failed to load sold books", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish(); // Close the current activity after navigating
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}