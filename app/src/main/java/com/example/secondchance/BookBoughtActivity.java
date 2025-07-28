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

public class BookBoughtActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookOrderAdapter adapter;
    private List<BuyNowActivity.OrderData> boughtBooks = new ArrayList<>();
    private DatabaseReference ordersRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.books_bought_activity);

        Toolbar toolbar = findViewById(R.id.toolbarBought);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Books Bought");
        }

        recyclerView = findViewById(R.id.recyclerViewBoughtBooks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookOrderAdapter(boughtBooks, this);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        if (currentUser != null) {
            fetchBoughtBooks();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBoughtBooks() {
        ordersRef.orderByChild("buyerUserId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boughtBooks.clear();
                        for (DataSnapshot orderSnap : snapshot.getChildren()) {
                            BuyNowActivity.OrderData order = orderSnap.getValue(BuyNowActivity.OrderData.class);
                            if (order != null) boughtBooks.add(order);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BookBoughtActivity.this, "Failed to load bought books", Toast.LENGTH_SHORT).show();
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