package com.example.cardmasters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.utils.CardDatabaseHelper;
import com.example.cardmasters.utils.UIUtils;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.btn_back_to_main).setOnClickListener(v -> finish());

        populateSpecialCardsGrid();
    }

    private void populateSpecialCardsGrid() {
        LinearLayout cardsContainer = findViewById(R.id.special_cards_container);
        CardDatabaseHelper dbHelper = new CardDatabaseHelper(this);

        // הטקסט המדויק שביקשת
        String[][] specialCardsData = {
                {"mr_house", "Mr. House", "שולף קלף נוסף בסוף כל תור."},
                {"slot_machine", "Slot Machine", "ג'קפוט! שולף 2 קלפים ליד."},
                {"platinum_chip", "Platinum Chip", "התקפת בונוס (+1) ללוחם."},
                {"nuka_cola", "Nuka Cola", "+5 חיים ללוחם!"},
                {"sunsets", "Sunset Sarsaparilla", "+5 התקפה ללוחם!"}
        };

        float density = getResources().getDisplayMetrics().density;
        int cardWidthPx = (int) (110 * density);
        int cardHeightPx = (int) (154 * density);

        LinearLayout currentRow = null;

        for (int i = 0; i < specialCardsData.length; i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                cardsContainer.addView(currentRow);
            }

            String cardId = specialCardsData[i][0];
            String title = specialCardsData[i][1];
            String description = specialCardsData[i][2];

            Card card = dbHelper.getCardById(cardId);
            if (card != null) {
                LinearLayout itemBox = new LinearLayout(this);
                itemBox.setOrientation(LinearLayout.VERTICAL);
                itemBox.setGravity(Gravity.CENTER);

                LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                boxParams.setMargins(8, 0, 8, 32);
                itemBox.setLayoutParams(boxParams);

                View cardView = UIUtils.createViewCard(getLayoutInflater(), itemBox, card, this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(cardWidthPx, cardHeightPx);
                cardParams.setMargins(0, 0, 0, 12);
                cardView.setLayoutParams(cardParams);
                cardView.setOnLongClickListener(null);

                TextView titleView = new TextView(this);
                titleView.setText(title);
                titleView.setTextColor(Color.parseColor("#FFD700"));
                titleView.setTextSize(14f);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setGravity(Gravity.CENTER);

                TextView descView = new TextView(this);
                descView.setText(description);
                descView.setTextColor(Color.parseColor("#BDC3C7"));
                descView.setTextSize(12f);
                descView.setGravity(Gravity.CENTER);
                descView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                descView.setSingleLine(false);
                descView.setLineSpacing(0, 1.1f);
                descView.setPadding(8, 4, 8, 0);

                itemBox.addView(cardView);
                itemBox.addView(titleView);
                itemBox.addView(descView);

                if (i == specialCardsData.length - 1 && i % 2 == 0) {
                    boxParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    boxParams.weight = 0;
                    itemBox.setPadding(32, 0, 32, 0);
                }

                currentRow.addView(itemBox);
            }
        }
    }
}