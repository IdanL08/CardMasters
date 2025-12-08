package com.example.cardmasters;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();//אם מוחקים מהפייר בייס את המשתמש עדיין יכנס אבל זה לא משנה כי לא מוחקים אותו משם

        if (user != null) {
            // Auto-login
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // First time → go to register
            startActivity(new Intent(this, RegisterActivity.class));
        }

        finish();
    }
}
