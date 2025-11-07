package com.example.cardmasters;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Buttons
    Button btnSettings, btnTasks, btnProfile, btnFriends;
    Button btnGame, btnLootbox, btnCollection, btnLeaderboard;

    // Launcher for result
    private final ActivityResultLauncher<Intent> lootboxLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Handle returned data here
                            String reward = result.getData().getStringExtra("lootResult");
                            // For now just log it or toast it
                            android.widget.Toast.makeText(this, "Got: " + reward, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        // Find views
        btnSettings = findViewById(R.id.btn_settings);
        btnTasks = findViewById(R.id.btn_tasks);
        btnProfile = findViewById(R.id.btn_profile);
        btnFriends = findViewById(R.id.btn_friends);
        btnGame = findViewById(R.id.btn_game);
        btnLootbox = findViewById(R.id.btn_lootbox);
        btnCollection = findViewById(R.id.btn_collection);
        btnLeaderboard = findViewById(R.id.btn_leaderboard);

        // === ACTIVITY INTENTS ===

        btnGame.setOnClickListener(v ->
                startActivity(new Intent(this, GameActivity.class)));


        btnCollection.setOnClickListener(v ->
                startActivity(new Intent(this, CollectionActivity.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        btnLootbox.setOnClickListener(v -> {
            Intent intent = new Intent(this, LootBoxActivity.class);
            lootboxLauncher.launch(intent);
        });

        // === DIALOGS ===
        btnSettings.setOnClickListener(v -> showCustomDialog("Settings"));
        btnTasks.setOnClickListener(v -> showCustomDialog("Tasks"));
        btnLeaderboard.setOnClickListener(v -> showCustomDialog("Leaderboard"));
        btnFriends.setOnClickListener(v -> showCustomDialog("Friends"));
    }

    // Simple reusable custom dialog method
    private void showCustomDialog(String title) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_custom); // create this layout below
        dialog.setCancelable(true);
        dialog.setTitle(title);

        // Optional: you can add a close button or dynamic text inside
        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        dialogTitle.setText(title);

        Button btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}