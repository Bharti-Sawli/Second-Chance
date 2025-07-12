package com.example.secondchance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SaleBookActivity extends AppCompatActivity {

    private static final String TAG = "SaleBookActivity";
    private static final int PICK_IMAGE_REQUEST1 = 1;
    private static final int PICK_IMAGE_REQUEST2 = 2;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private EditText inputTitle, inputAuthor, inputCategory, inputPrice, inputCondition, inputDescription;
    private ImageView imageView1, imageView2;
    private Button btnChooseImage1, btnChooseImage2, btnSubmit;
    private ProgressBar progressBar;
    private Uri imageUri1, imageUri2;
    private int permissionRetryCount = 0;
    private static final int MAX_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_book);

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized");
        }

        // Initialize views
        inputTitle = findViewById(R.id.inputTitle);
        inputAuthor = findViewById(R.id.inputAuthor);
        inputCategory = findViewById(R.id.inputCategory);
        inputPrice = findViewById(R.id.inputPrice);
        inputCondition = findViewById(R.id.inputCondition);
        inputDescription = findViewById(R.id.inputDescription);
        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);
        btnChooseImage1 = findViewById(R.id.btnChooseImage1);
        btnChooseImage2 = findViewById(R.id.btnChooseImage2);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        // Request permission on app start
        requestStoragePermissionOnStart();

        // Image selection for ImageView 1
        btnChooseImage1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    openFileChooser(PICK_IMAGE_REQUEST1);
                } else {
                    requestStoragePermissionWithRetry();
                    Toast.makeText(SaleBookActivity.this, "Storage permission required. Please grant permission.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Image selection for ImageView 2
        btnChooseImage2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    openFileChooser(PICK_IMAGE_REQUEST2);
                } else {
                    requestStoragePermissionWithRetry();
                    Toast.makeText(SaleBookActivity.this, "Storage permission required. Please grant permission.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Submit button logic
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    showProgress(true);
                    uploadData();
                } else {
                    Toast.makeText(SaleBookActivity.this, "Please fill all the details and submit", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestStoragePermissionOnStart() {
        if (!checkPermission()) {
            Log.d(TAG, "Requesting storage permission on app start");
            String[] permissions = getRequiredPermissions();
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Storage permission already granted on app start");
        }
    }

    private void requestStoragePermissionWithRetry() {
        if (permissionRetryCount < MAX_RETRIES) {
            Log.d(TAG, "Requesting storage permission (Retry #" + (permissionRetryCount + 1) + ")");
            String[] permissions = getRequiredPermissions();
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE);
            permissionRetryCount++;
        } else {
            Log.d(TAG, "Max retries reached. Opening app settings.");
            Toast.makeText(this, "Max retries reached. Please enable storage permission in settings.", Toast.LENGTH_SHORT).show();
            openAppSettings();
        }
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted");
                Toast.makeText(this, "Storage permission granted. Try selecting an image again.", Toast.LENGTH_SHORT).show();
                permissionRetryCount = 0; // Reset retry count on success
            } else {
                Log.d(TAG, "Storage permission denied");
                Toast.makeText(this, "Storage permission denied. Please enable it in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST1) {
                imageUri1 = data.getData();
                imageView1.setImageURI(imageUri1);
            } else if (requestCode == PICK_IMAGE_REQUEST2) {
                imageUri2 = data.getData();
                imageView2.setImageURI(imageUri2);
            }
        } else {
            Log.d(TAG, "Image selection failed or canceled: resultCode=" + resultCode);
            Toast.makeText(this, "Image selection failed. Ensure storage permission is granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs() {
        boolean isValid = !inputTitle.getText().toString().isEmpty() &&
                !inputAuthor.getText().toString().isEmpty() &&
                !inputCategory.getText().toString().isEmpty() &&
                !inputPrice.getText().toString().isEmpty() &&
                !inputCondition.getText().toString().isEmpty() &&
                !inputDescription.getText().toString().isEmpty() &&
                imageUri1 != null &&
                imageUri2 != null;
        Log.d(TAG, "Validation result: " + (isValid ? "Valid" : "Invalid"));
        return isValid;
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!show);
        } else {
            Log.w(TAG, "ProgressBar not found in layout");
            btnSubmit.setEnabled(!show);
        }
    }

    private void uploadData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String productId = UUID.randomUUID().toString();

            StorageReference storageRef = FirebaseStorage.getInstance().getReference("book_images/" + productId);
            StorageReference imageRef1 = storageRef.child("image1_" + System.currentTimeMillis() + ".jpg");
            StorageReference imageRef2 = storageRef.child("image2_" + System.currentTimeMillis() + ".jpg");

            UploadTask uploadTask1 = imageRef1.putFile(imageUri1);
            UploadTask uploadTask2 = imageRef2.putFile(imageUri2);

            uploadTask1.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageRef1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUrl1 = uri.toString();
                            uploadTask2.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    imageRef2.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String imageUrl2 = uri.toString();
                                            saveToDatabase(productId, userId, imageUrl1, imageUrl2);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            showProgress(false);
                                            Log.e(TAG, "Failed to get image URL 2: " + e.getMessage());
                                            Toast.makeText(SaleBookActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showProgress(false);
                                    Log.e(TAG, "Failed to upload image 2: " + e.getMessage());
                                    Toast.makeText(SaleBookActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showProgress(false);
                            Log.e(TAG, "Failed to get image URL 1: " + e.getMessage());
                            Toast.makeText(SaleBookActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    showProgress(false);
                    if (e.getCause() instanceof java.net.UnknownHostException) {
                        Log.e(TAG, "Network error: " + e.getMessage());
                        Toast.makeText(SaleBookActivity.this, "Network error: Please check your internet connection", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to upload image 1: " + e.getMessage());
                        Toast.makeText(SaleBookActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            showProgress(false);
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToDatabase(String productId, String userId, String imageUrl1, String imageUrl2) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("items");

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("productId", productId);
        bookData.put("userId", userId);
        bookData.put("title", inputTitle.getText().toString().trim());
        bookData.put("author", inputAuthor.getText().toString().trim());
        bookData.put("category", inputCategory.getText().toString().trim());
        bookData.put("price", inputPrice.getText().toString().trim());
        bookData.put("condition", inputCondition.getText().toString().trim());
        bookData.put("description", inputDescription.getText().toString().trim());
        bookData.put("imageUrl1", imageUrl1);
        bookData.put("imageUrl2", imageUrl2);

        databaseRef.child(productId).setValue(bookData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showProgress(false);
                        Log.d(TAG, "Data saved successfully");
                        Toast.makeText(SaleBookActivity.this, "Your book has been kept on sale successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SaleBookActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showProgress(false);
                        if (e.getCause() instanceof java.net.UnknownHostException) {
                            Log.e(TAG, "Network error during save: " + e.getMessage());
                            Toast.makeText(SaleBookActivity.this, "Network error: Please check your internet connection", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to save data: " + e.getMessage());
                            Toast.makeText(SaleBookActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}