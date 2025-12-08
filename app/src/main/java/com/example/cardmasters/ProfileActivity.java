package com.example.cardmasters;

import static java.security.AccessController.getContext;

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
import com.example.cardmasters.utils.UserPrefsUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView txtEmail;
    private EditText edtUsername;
    private Button btnUpdate, btnDelete, btnBack;


    private FirebaseUser currentUser;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // --- Firebase ---
        currentUser = FirebaseUtils.getCurrentUser(); // << CLEAN, no FirebaseAuth here

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        init();
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
        edtUsername.setText(UserPrefsUtils.getUsername(ProfileActivity.this));

        // --- Update username ---
        btnUpdate.setOnClickListener(v -> {
            String newUsername = edtUsername.getText().toString().trim();
            FirebaseUtils.updateUsername(ProfileActivity.this, newUsername, null);
        });

        // --- Delete user ---
        btnDelete.setOnClickListener(v -> {
            FirebaseUtils.reauthAndDelete(ProfileActivity.this, () -> {
                // After deletion, go to Register
                startActivity(new Intent(this, RegisterActivity.class));
                finish();
            });
        });

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }



}
