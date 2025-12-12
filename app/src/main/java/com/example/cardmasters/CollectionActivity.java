package com.example.cardmasters;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.utils.CardDatabaseHelper;

import java.util.List;

public class CollectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collection);
        Button btnBack = findViewById(R.id.btn_back_to_main);
        testDeckAndCollectionManagement();
        btnBack.setOnClickListener(v -> {

            finish();
        });
    }



// NOTE: This entire function should be placed inside an Activity class (e.g., ProfileActivity)
// where 'this' refers to the Context.

    public void testDeckAndCollectionManagement() {
        // 1. Initialize the Database Helper
        // The 'this' keyword provides the necessary Context.
        CardDatabaseHelper dbHelper = new CardDatabaseHelper(this);

        // Define the Card IDs we know exist from preLoadCards:
        final String KNIGHT_ID = "f1"; // Fighter Card
        final String HEAL_ID = "e1"; // Effect Card

        Log.d("CardMasters", "--- DB MANAGEMENT DEMO START ---");

        // =========================================================
        // CRUD: COLLECTION MANAGEMENT (The Player owns these cards)
        // =========================================================

        // 2. Clear previous data to ensure a clean test environment (Optional)
        dbHelper.clearDeck();
        // For a real app, you would NOT clear the deck or collection here.

        // 3. ADD: Give the player 5 Knights and 2 Heals (This creates/updates the collection table)
        dbHelper.addCardsToCollection(KNIGHT_ID, 5);
        dbHelper.addCardsToCollection(HEAL_ID, 2);
        Log.d("CardMasters", "Collection: Added 5 Knights (f1) and 2 Heals (e1).");

        // 4. UPDATE: The player gains 3 more Knights
        dbHelper.addCardsToCollection(KNIGHT_ID, 3);
        Log.d("CardMasters", "Collection: Added 3 more Knights (f1). Total should be 8.");

        // (Note: To view the collection data, you'd need a separate `getCollection` method.)

        // =========================================================
        // CRUD: DECK BUILDING (The Active Deck)
        // =========================================================

        // 5. CREATE: Set the initial deck contents (uses the deck table)
        dbHelper.updateCardInDeck(KNIGHT_ID, 2); // Put 2 Knights in the deck
        dbHelper.updateCardInDeck(HEAL_ID, 1);   // Put 1 Heal in the deck
        Log.d("CardMasters", "Deck: Initialized with 2 Knights and 1 Heal.");

        // 6. UPDATE: Change the quantity of the Knight
        dbHelper.updateCardInDeck(KNIGHT_ID, 4); // Now 4 Knights in the deck
        Log.d("CardMasters", "Deck: Updated Knight count to 4.");

        // 7. DELETE: Remove the Heal card entirely (by setting quantity <= 0)
        dbHelper.updateCardInDeck(HEAL_ID, 0); // Quantity of 0 deletes the row
        Log.d("CardMasters", "Deck: Removed Heal card by setting quantity to 0.");

        // =========================================================
        // CRUD: READ (Testing the final result)
        // =========================================================

        // 8. READ: Get the final list of Card objects for the game
        List<Card> activeDeck = dbHelper.getActiveDeck();

        Log.d("CardMasters", "--- FINAL ACTIVE DECK RESULTS ---");
        Log.d("CardMasters", "Total Cards in Deck (Object count): " + activeDeck.size());

        for (Card card : activeDeck) {
            // NOTE: Assuming your Card class has a getName() method
            Log.d("CardMasters", "Card Loaded: " + card.getName() + " (ID: " + card.getId() + ")");
        }

        // 9. Final Cleanup
        // You should close the database helper when the app is destroyed.
        // dbHelper.close(); // Uncomment this line if you are done with the DB for a long time.

        Log.d("CardMasters", "--- DB MANAGEMENT DEMO END ---");
    }
}