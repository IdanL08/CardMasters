package com.example.cardmasters;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.utils.AnimatedChestView;
import com.example.cardmasters.utils.CardDatabaseHelper;
import com.example.cardmasters.utils.UIUtils;

public class LootBoxActivity extends AppCompatActivity {
    private CardDatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private Button btnBack, btnOpen, btnOpen3;
    private AnimatedChestView chestView;
    private LinearLayout cardContainer;
    private TextView tvLootboxCount;
    private int lootboxCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loot_box);

        dbHelper = new CardDatabaseHelper(this);
        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        lootboxCount = prefs.getInt("lootbox_count", 9);

        btnBack = findViewById(R.id.btn_back_to_main);
        chestView = findViewById(R.id.chestView);
        btnOpen = findViewById(R.id.btnOpen);
        btnOpen3 = findViewById(R.id.btnOpen3);
        cardContainer = findViewById(R.id.cardContainer);
        tvLootboxCount = findViewById(R.id.tv_lootbox_count);

        updateUI();

        btnOpen.setOnClickListener(v -> handleOpen(1));
        btnOpen3.setOnClickListener(v -> handleOpen(3));
        btnBack.setOnClickListener(v -> finish());
    }

    private void handleOpen(int amount) {
        // 1. Update data immediately
        lootboxCount -= amount;
        prefs.edit().putInt("lootbox_count", lootboxCount).apply();

        // 2. Immediate UI feedback: update text and disable buttons manually
        tvLootboxCount.setText("Lootboxes: " + lootboxCount);
        btnOpen.setEnabled(false);
        btnOpen3.setEnabled(false);
        btnOpen.setAlpha(0.5f);
        btnOpen3.setAlpha(0.5f);

        cardContainer.removeAllViews();
        chestView.openChest();

        // 3. Start the card creation loop
        for (int i = 0; i < amount; i++) {
            createWonCard(i, amount);
        }
    }

    private void updateUI() {
        tvLootboxCount.setText("Lootboxes: " + lootboxCount);

        // Enable buttons based on the current count
        btnOpen.setEnabled(lootboxCount >= 1);
        btnOpen.setAlpha(lootboxCount >= 1 ? 1.0f : 0.5f);

        btnOpen3.setEnabled(lootboxCount >= 3);
        btnOpen3.setAlpha(lootboxCount >= 3 ? 1.0f : 0.5f);
    }

    private void createWonCard(int index, int totalAmount) {
        // Slight delay before card appears (chest opening time)
        new Handler().postDelayed(() -> {
            Card wonCard = dbHelper.getRandomCard();
            if (wonCard != null) {
                dbHelper.addCardsToCollection(wonCard.getId(), 1);
                View cardView = UIUtils.createViewCard(getLayoutInflater(), cardContainer, wonCard, this);
                cardContainer.addView(cardView);

                // Entrance Animation
                cardView.setAlpha(0f);
                cardView.animate().alpha(1f).setDuration(400).start();

                // Wait 2 seconds while user looks at the card
                new Handler().postDelayed(() -> {
                    chestView.closeChest();

                    // Exit Animation
                    if (cardView.getParent() != null) {
                        cardView.animate()
                                .alpha(0f)
                                .setDuration(500)
                                .withEndAction(() -> {
                                    cardContainer.removeView(cardView);

                                    // 4. ONLY re-enable buttons after the LAST card has vanished
                                    if (index == totalAmount - 1) {
                                        updateUI();
                                    }
                                }).start();
                    }
                }, 2000);
            }
        }, 800);
    }
}