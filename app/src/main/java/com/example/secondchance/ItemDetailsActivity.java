package com.example.secondchance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ItemDetailsActivity extends AppCompatActivity {

    private TextView itemTitle, itemAuthor, itemCategory, itemPrice, itemCondition, itemDescription, sellerName, txtRecommended;
    private ViewPager2 viewPagerImages;
    private Button btnContactSeller, btnBuyNow;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewRecommended;
    private ImageView backIcon;
    private DatabaseReference itemsRef, usersRef;
    private String itemId;
    private List<Item> recommendedItems = new ArrayList<>();
    private RecommendedAdapter recommendedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        // Initialize views
        itemTitle = findViewById(R.id.itemTitle);
        itemAuthor = findViewById(R.id.itemAuthor);
        itemCategory = findViewById(R.id.itemCategory);
        itemPrice = findViewById(R.id.itemPrice);
        itemCondition = findViewById(R.id.itemCondition);
        itemDescription = findViewById(R.id.itemDescription);
        sellerName = findViewById(R.id.sellerName);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        btnContactSeller = findViewById(R.id.btnContactSeller);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        progressBar = findViewById(R.id.progressBar);
        txtRecommended = findViewById(R.id.txtRecommended);
        recyclerViewRecommended = findViewById(R.id.recyclerViewRecommended);
        backIcon=findViewById(R.id.backIcon);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        itemsRef = database.getReference("items");
        usersRef = database.getReference("users");

        // Get itemId from intent
        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Set up ViewPager2 with adapter
        ImageAdapter imageAdapter = new ImageAdapter(this);
        viewPagerImages.setAdapter(imageAdapter);
        viewPagerImages.setOffscreenPageLimit(2); // Keep both fragments in memory for smooth cycling
        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Optional: Log or handle page changes if needed
            }
        });

        // Fetch item details
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String author = snapshot.child("author").getValue(String.class);
                    String category = snapshot.child("category").getValue(String.class);
                    String price = snapshot.child("price").getValue(String.class);
                    String condition = snapshot.child("condition").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String userId = snapshot.child("userId").getValue(String.class);
                    String imageUrl1 = snapshot.child("imageurl1").getValue(String.class);
                    String imageUrl2 = snapshot.child("imageurl2").getValue(String.class);

                    itemTitle.setText(title != null ? title : "N/A");
                    itemAuthor.setText(author != null ? author : "N/A");
                    itemCategory.setText(category != null ? category : "N/A");
                    itemPrice.setText(price != null ? price : "₹0");
                    itemCondition.setText(condition != null ? condition : "N/A");
                    itemDescription.setText(description != null ? description : "N/A");

                    // Update ViewPager images
                    imageAdapter.setImageUrls(imageUrl1, imageUrl2);
                    imageAdapter.notifyDataSetChanged();

                    // Fetch seller name
                    if (userId != null) {
                        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String name = userSnapshot.child("name").getValue(String.class);
                                    sellerName.setText(name != null ? name : "Unknown Seller");
                                } else {
                                    sellerName.setText("Unknown Seller");
                                }
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                sellerName.setText("Unknown Seller");
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        sellerName.setText("Unknown Seller");
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ItemDetailsActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ItemDetailsActivity.this, "Failed to load item", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up RecyclerView for all items
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendedAdapter = new RecommendedAdapter(recommendedItems, this);
        recyclerViewRecommended.setAdapter(recommendedAdapter);

        // Fetch all items for "Recommended for You"
        itemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recommendedItems.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    if (item != null && !item.getId().equals(itemId)) { // Exclude current item
                        recommendedItems.add(item);
                    }
                }
                recommendedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ItemDetailsActivity.this, "Failed to load recommended items", Toast.LENGTH_SHORT).show();
            }
        });

        // Back icon click
        backIcon.setOnClickListener(v -> finish());

        // Contact Seller button click
        btnContactSeller.setOnClickListener(v -> {
            String sellerPhone = "9491552302"; // Hardcoded from users data, fetch dynamically if needed
            Toast.makeText(this, "Contacting seller at " + sellerPhone, Toast.LENGTH_SHORT).show();
            // TODO: Implement intent (e.g., startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sellerPhone))));
        });

        // Buy Now button click
        btnBuyNow.setOnClickListener(v -> {
            Intent intent = new Intent(ItemDetailsActivity.this, BuyNowActivity.class);
            intent.putExtra("itemId", itemId);
            startActivity(intent);
        });
    }

    // Item class to map Firebase data
    public static class Item {
        private String id, title, author, category, condition, description, price, userId, imageurl1, imageurl2;

        public Item() {
            // Default constructor required for Firebase
        }

        public Item(String id, String title, String author, String category, String condition, String description, String price, String userId, String imageurl1, String imageurl2) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.category = category;
            this.condition = condition;
            this.description = description;
            this.price = price;
            this.userId = userId;
            this.imageurl1 = imageurl1;
            this.imageurl2 = imageurl2;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public String getCondition() { return condition; }
        public String getDescription() { return description; }
        public String getPrice() { return price; }
        public String getUserId() { return userId; }
        public String getImageurl1() { return imageurl1; }
        public String getImageurl2() { return imageurl2; }
    }

    // Adapter for Recommended Items
    public static class RecommendedAdapter extends RecyclerView.Adapter<RecommendedAdapter.ViewHolder> {

        private List<Item> items;
        private final Context context;

        public RecommendedAdapter(List<Item> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            if (item != null) {
                holder.title.setText(item.getTitle() != null ? item.getTitle() : "N/A");
                holder.price.setText(item.getPrice() != null ? item.getPrice() : "₹0");

                // Load image using Glide (use imageUrl1, fallback to placeholder if null)
                Glide.with(context)
                        .load(item.getImageurl1() != null && !item.getImageurl1().isEmpty() ? item.getImageurl1() : R.drawable.placeholder_image)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(holder.image);

                // Click listener to open item details
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ItemDetailsActivity.class);
                    intent.putExtra("itemId", item.getId());
                    context.startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, price;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.itemImage);
                title = itemView.findViewById(R.id.itemTitle);
                price = itemView.findViewById(R.id.itemPrice);
            }
        }
    }

    // Adapter for ViewPager2 Images
    private class ImageAdapter extends FragmentStateAdapter {
        private String imageUrl1, imageUrl2;

        public ImageAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        public void setImageUrls(String url1, String url2) {
            this.imageUrl1 = url1 != null ? url1 : "";
            this.imageUrl2 = url2 != null ? url2 : "";
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ImageFragment.newInstance(position == 0 ? imageUrl1 : imageUrl2);
        }

        @Override
        public int getItemCount() {
            return 2; // Two images to cycle
        }
    }

    // Fragment to display each image
    public static class ImageFragment extends Fragment {
        private static final String ARG_IMAGE_URL = "image_url";

        public static ImageFragment newInstance(String imageUrl) {
            ImageFragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putString(ARG_IMAGE_URL, imageUrl);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_image, container, false);
            ImageView imageView = view.findViewById(R.id.imageView);
            String imageUrl = getArguments() != null ? getArguments().getString(ARG_IMAGE_URL) : "";

            Glide.with(this)
                    .load(imageUrl.isEmpty() ? R.drawable.placeholder_image : imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .into(imageView);

            return view;
        }
    }
}