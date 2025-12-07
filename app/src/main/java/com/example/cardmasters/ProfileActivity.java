package com.example.cardmasters;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.utils.FirebaseUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView txtEmail;
    private EditText edtUsername;
    private Button btnUpdate, btnDelete, btnBack;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);



        // --- Firebase ---
        mAuth = FirebaseAuth.getInstance();//TODO move all firebase stuff to a separate class
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        init();
    }

    private void updateUsername() {
        String newUsername = edtUsername.getText().toString().trim();
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Username update error", e);
                });
    }
    private void deleteCurrentUser()
    {   currentUser.delete()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, RegisterActivity.class));
                    //TODO clear all card decks data
                    finish(); // return to previous activity
                } else {
                    Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete user error", task.getException());
                }
            }
            );
    }

    private void init() {

        // --- Views ---
        txtEmail = findViewById(R.id.txtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btn_back_to_main);

        // --- Populate fields ---
        txtEmail.setText(currentUser.getEmail());

        // Optional: load username from Firestore
        FirebaseUtils.getUsername(new FirebaseUtils.UsernameCallback() {
            @Override
            public void onUsernameLoaded(String username) {
                edtUsername.setText(username);
            }

            @Override
            public void onError(Exception e) {
                Log.e("ProfileActivity", "Failed to load username", e);
            }
        });
        // --- Update username ---
        btnUpdate.setOnClickListener(v -> updateUsername());

        // --- Delete user ---
        btnDelete.setOnClickListener(v -> deleteCurrentUser());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

}
