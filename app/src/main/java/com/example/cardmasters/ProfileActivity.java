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
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show();
                    UserPrefsUtils.saveUsername(ProfileActivity.this, newUsername); // << SAVE LOCALLY
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Username update error", e);
                });
    }
    private void reauthenticateAndDelete() {

        String email = UserPrefsUtils.getEmail(this);
        String password = UserPrefsUtils.getPassword(this);

        if (email == null || password == null) {
            Toast.makeText(this, "Missing saved credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);//יוצר מין כרטיס כניסה בעזרת עצמים של הפיירבייס

        user.reauthenticate(credential).addOnSuccessListener(aVoid -> {

            String uid = user.getUid();

            // 1) delete document from Firestore
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(unused -> {

                        // 2) delete the FirebaseAuth user
                        user.delete().addOnSuccessListener(aVoid1 -> {

                            // 3) clear shared preferences
                            UserPrefsUtils.clear(ProfileActivity.this);

                            Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();

                            //TODO clear local db

                            // 4) go to Register / Splash
                            startActivity(new Intent(this, RegisterActivity.class));
                            finish();

                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to delete Auth user", Toast.LENGTH_SHORT).show();
                        });

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete Firestore profile", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Re-authentication failed", Toast.LENGTH_SHORT).show();
        });
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
        btnUpdate.setOnClickListener(v -> updateUsername());

        // --- Delete user ---
        btnDelete.setOnClickListener(v -> reauthenticateAndDelete());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

}
