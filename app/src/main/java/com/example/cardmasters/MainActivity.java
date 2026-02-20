package com.example.cardmasters;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.utils.LeaderboardAdapter;
import com.example.cardmasters.utils.lootboxes_utils.AlarmUtils;

public class MainActivity extends AppCompatActivity {

    // Buttons
    ImageButton btnSettings, btnTasks, btnProfile, btnFriends;
    Button btnGame;
    ImageButton btnLootbox, btnCollection, btnLeaderboard;

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

        AlarmUtils.checkNotificationPermission(this,this);
        AlarmUtils.setupLootBoxAlarm(this, 10, 34);

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
                startActivity(new Intent(this, MatchmakingActivity.class)));


        btnCollection.setOnClickListener(v ->
                startActivity(new Intent(this, CollectionActivity.class)));

        btnProfile.setOnClickListener(v ->{
                startActivity(new Intent(this, ProfileActivity.class));
                finish();});

        btnLootbox.setOnClickListener(v -> {//TODO הוסף קלפים ויהלומים
            Intent intent = new Intent(this, LootBoxActivity.class);
            lootboxLauncher.launch(intent);
        });

        // === DIALOGS ===
        btnSettings.setOnClickListener(v -> showCustomDialog("Settings"));//TODO יחסית בסוף להוסיף הגדרות צליל
        btnTasks.setOnClickListener(v -> showCustomDialog("Tasks"));//TODO כמות יהלומים ולוטבוקסים userPrefsמשימות רנדומליות המעניקות פרסים, רושמות ב

        btnLeaderboard.setOnClickListener(v -> LeaderboardAdapter.showLeaderboardDialog(this));
        btnFriends.setOnClickListener(v -> showCustomDialog("Friends"));//TODO הצג את כל השחקנים, הצג את החברים למעלה
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
