package com.example.cardmasters;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;
import com.example.cardmasters.utils.CardDatabaseHelper;
import com.example.cardmasters.utils.UIUtils;

import java.util.Objects;

public class CollectionActivity extends AppCompatActivity {

    private CardDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        dbHelper = new CardDatabaseHelper(this);

        // Set up UI elements and back button
        findViewById(R.id.btn_back_to_main).setOnClickListener(v -> finish());


        // Refresh UI based on the current state of the deck and collection


        dbHelper.addCardsToCollection("fishboy", 2);
        dbHelper.addCardsToCollection("ghoul_cowboy",3);
        dbHelper.addCardsToCollection("robo_cowboy",4);
        dbHelper.addCardsToCollection("emp",4);
        dbHelper.addCardsToCollection("gambler",4);

        refreshUI();
    }

    // Helper method to create a view for each card (image + quantity)
    private View createCardView(String cardId, Card c, int count, boolean isDeck)/** credits (1)**/{
        // This container is now "tight" - it won't expand past the card image
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);

        // --- Quantity Text ---
        TextView qty = new TextView(this);
        qty.setText("x" + count);
        qty.setTextColor(Color.WHITE);
        qty.setTextSize(14f);
        qty.setTypeface(null, Typeface.BOLD);
        qty.setGravity(Gravity.END); // Locked to the right side of the card

        // --- Card Image ---
        // UIUtils will define the width of the 'cardView'
        View cardView = UIUtils.createViewCard(getLayoutInflater(), cardLayout, c, this);

        cardLayout.addView(qty);
        cardLayout.addView(cardView);

        // --- The No-Drift Grid Logic ---
        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
        gridParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
        gridParams.height = GridLayout.LayoutParams.WRAP_CONTENT;

        // We manually set the gravity to TOP and START (Left)
        // so they pack together instead of spreading out
        gridParams.setGravity(Gravity.TOP | Gravity.START);

        cardLayout.setLayoutParams(gridParams);

        cardLayout.setOnClickListener(v -> {
            dbHelper.updateCardInDeck(cardId, !isDeck);
            refreshUI();
        });

        return cardLayout;
    }
    // Method to update UI with the current deck and collection data
    private void refreshUI() {
        GridLayout deckContainer = findViewById(R.id.deck_container);
        GridLayout collectionContainer = findViewById(R.id.collection_container);

        // Clear previous views before refreshing
        deckContainer.removeAllViews();
        collectionContainer.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Card c=null;

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
                "SELECT " +
                        "cards_library.id AS card_id, " +
                        "collection.quantity, " +
                        "collection.in_use_quantity, " +
                        "cards_library.name, " +
                        "cards_library.cost, " +
                        "cards_library.hp, " +
                        "cards_library.atk, " +
                        "cards_library.card_class " +
                        "FROM cards_library " +
                        "INNER JOIN collection ON cards_library.id = collection.id",
                null
        );


        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            int quantity = cursor.getInt(1);
            int inUse = cursor.getInt(2);
            String name = cursor.getString(3);
            int cost = cursor.getInt(4);
            int hp = cursor.getInt(5);
            int atk = cursor.getInt(6);
            String card_class = cursor.getString(7);
            if(Objects.equals(card_class, "FIGHTER"))
                {
                    c=new FighterCard( id,  name,  cost,  hp,  atk);
                }
            else if(Objects.equals(card_class, "EFFECT")){
                c= new EffectCard(id,name, cost);
            }




            // If the card is in use (in the deck), show it in the deck section
            if (inUse > 0) {
                deckContainer.addView(createCardView(id, c, inUse, true));
            }

            // If there are any cards available in the collection, show them
            int available = quantity - inUse;
            if (available > 0) {
                collectionContainer.addView(createCardView(id, c, available, false));
            }
        }

        cursor.close();
    }



}
