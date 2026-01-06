package com.example.cardmasters;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.utils.CardDatabaseHelper;

public class CollectionActivity extends AppCompatActivity {

    private CardDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        dbHelper = new CardDatabaseHelper(this);

        // Set up UI elements and back button
        findViewById(R.id.btn_back_to_main).setOnClickListener(v -> finish());

        // Refresh UI based on the current state of the deck and collection
        refreshUI();
    }

    // Helper method to create a view for each card (image + quantity)
    private View createCardView(String cardId, int imageRes, int count, boolean isDeck) {
        // Create a new card container (LinearLayout)
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(8, 8, 8, 8);
        card.setGravity(Gravity.CENTER);

        // Add image
        ImageView image = new ImageView(this);
        image.setImageResource(imageRes);
        image.setLayoutParams(new LinearLayout.LayoutParams(160, 220));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Add quantity text
        TextView qty = new TextView(this);
        qty.setText("x" + count);
        qty.setTextColor(getResources().getColor(android.R.color.white));
        qty.setGravity(Gravity.CENTER);

        card.addView(image);
        card.addView(qty);

        // Handle card tap (add/remove logic)
        card.setOnClickListener(v -> {
            dbHelper.updateCardInDeck(cardId, !isDeck);  // Toggle the card from deck/collection
            refreshUI();  // Refresh the UI after the update
        });

        return card;
    }

    // Method to update UI with the current deck and collection data
    private void refreshUI() {
        LinearLayout deckContainer = findViewById(R.id.deck_container);
        LinearLayout collectionContainer = findViewById(R.id.collection_container);

        // Clear previous views before refreshing
        deckContainer.removeAllViews();
        collectionContainer.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        TextView deckSizeLabel = findViewById(R.id.deck_size_label);
        int deckSize = dbHelper.getDeckSize();
        deckSizeLabel.setText("Deck: " + deckSize + "/" + CardDatabaseHelper.MAX_DECK_SIZE);
        // Change text color if deck size is not exact
        if (deckSize != CardDatabaseHelper.MAX_DECK_SIZE) {
            deckSizeLabel.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            deckSizeLabel.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }

        // Query to get all cards in the collection
        Cursor cursor = db.rawQuery(
                "SELECT card_id, quantity, in_use_quantity FROM collection",
                null
        );

        while (cursor.moveToNext()) {
            String cardId = cursor.getString(0);
            int quantity = cursor.getInt(1);
            int inUse = cursor.getInt(2);

            int imageRes = getImageForCard(cardId); // Get the image resource for this card

            // If the card is in use (in the deck), show it in the deck section
            if (inUse > 0) {
                deckContainer.addView(createCardView(cardId, imageRes, inUse, true));
            }

            // If there are any cards available in the collection, show them
            int available = quantity - inUse;
            if (available > 0) {
                collectionContainer.addView(createCardView(cardId, imageRes, available, false));
            }
        }

        cursor.close();
    }

    // Helper method to get the image resource ID based on cardId
    private int getImageForCard(String cardId) {
        switch (cardId) {
            case "fishboy":
                return R.drawable.im_fishboy; // Replace with actual drawable resource ID
            case "nepton":
                return R.drawable.im_nepton; // Replace with actual drawable resource ID
            case "sponge":
                return R.drawable.im_sponge; // Replace with actual drawable resource ID
            default:
                return R.drawable.ic_launcher_foreground; // Default image for undefined card
        }
    }
}
