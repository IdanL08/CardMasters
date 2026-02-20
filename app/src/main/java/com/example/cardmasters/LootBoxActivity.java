package com.example.cardmasters;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        // אתחול
        dbHelper = new CardDatabaseHelper(this);
        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        lootboxCount = prefs.getInt("lootbox_count", 0);

        btnBack = findViewById(R.id.btn_back_to_main);
        chestView = findViewById(R.id.chestView);
        btnOpen = findViewById(R.id.btnOpen);
        btnOpen3 = findViewById(R.id.btnOpen3);
        cardContainer = findViewById(R.id.cardContainer);
        tvLootboxCount = findViewById(R.id.tv_lootbox_count);



        // עדכון UI ראשוני
        updateUI();

        btnOpen.setOnClickListener(v -> handleOpen(1));
        btnOpen3.setOnClickListener(v -> handleOpen(3));
        btnBack.setOnClickListener(v -> finish());
    }

    private void handleOpen(int amount) {
        // גריעת התיבות ושמירה
        lootboxCount -= amount;
        prefs.edit().putInt("lootbox_count", lootboxCount).apply();

        // ניקוי מסך ונעילת כפתורים
        cardContainer.removeAllViews();
        btnOpen.setEnabled(false);
        btnOpen3.setEnabled(false);

        updateUI(); // יעדכן את ה-Alpha מיד
        chestView.openChest();

        for (int i = 0; i < amount; i++) {
            createWonCard();
        }
    }

    private void updateUI() {
        tvLootboxCount.setText("Lootboxes: " + lootboxCount);

        // כפתור 1
        btnOpen.setEnabled(lootboxCount >= 1);
        btnOpen.setAlpha(lootboxCount >= 1 ? 1.0f : 0.5f);

        // כפתור 3
        btnOpen3.setEnabled(lootboxCount >= 3);
        btnOpen3.setAlpha(lootboxCount >= 3 ? 1.0f : 0.5f);
    }

    private void createWonCard() {
        new android.os.Handler().postDelayed(() -> {
            Card wonCard = dbHelper.getRandomCard();
            if (wonCard != null) {
                dbHelper.addCardsToCollection(wonCard.getId(), 1);
                View cardView = UIUtils.createViewCard(getLayoutInflater(), cardContainer, wonCard, this);
                cardContainer.addView(cardView);

                cardView.setAlpha(0f);
                cardView.animate().alpha(1f).setDuration(400).start();

                new android.os.Handler().postDelayed(() -> {
                    chestView.closeChest();
                    updateUI(); // החזרת הכפתורים למצב לחיץ/אפור לפי המלאי החדש

                    if (cardView.getParent() != null) {
                        cardView.animate().alpha(0f).setDuration(500)
                                .withEndAction(() -> cardContainer.removeView(cardView)).start();
                    }
                }, 2000);
            }
        }, 800);
    }


}