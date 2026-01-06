package com.example.cardmasters.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;

import java.util.ArrayList;
import java.util.List;

public class CardDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CardMasters.db";
    private static final int DATABASE_VERSION = 1;
    public static final int MAX_DECK_SIZE = 4; // Example: deck must have exactly 10 cards


    public CardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. MASTER LIBRARY (Read-Only)
        db.execSQL("CREATE TABLE cards_library (" +
                "id TEXT PRIMARY KEY, " +
                "card_class TEXT, " +
                "name TEXT, " +
                "cost INTEGER, " +
                "hp INTEGER, " +
                "atk INTEGER, " +
                "effect_type TEXT, " +
                "effect_value INTEGER)");



        // 3. THE COLLECTION (Tracks player-owned cards and quantities)
        db.execSQL("CREATE TABLE collection (" +
                "card_id TEXT PRIMARY KEY, " +
                "quantity INTEGER, " +
                "in_use_quantity INTEGER, " +
                "FOREIGN KEY(card_id) REFERENCES cards_library(id))"
                    );


        // --- PRE-LOAD DATA ---
        preLoadCards(db);
    }

    private void preLoadCards(SQLiteDatabase db) {
        db.execSQL(
                "INSERT INTO cards_library VALUES " +
                        "('fishboy', 'FIGHTER', 'Fish Boy', 3, 12, 4, NULL, NULL)"
        );

        db.execSQL(
                "INSERT INTO cards_library VALUES " +
                        "('nepton', 'FIGHTER', 'Nepton', 5, 20, 6, NULL, NULL)"
        );

        db.execSQL(
                "INSERT INTO cards_library VALUES " +
                        "('sponge', 'FIGHTER', 'Sponge', 2, 8, 2, NULL, NULL)"
        );


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cards_library");
        db.execSQL("DROP TABLE IF EXISTS collection"); // New drop
        onCreate(db);
    }

    // --- COLLECTION CRUD: ADD ---

    /**
     * Adds the specified amount of a card to the player's collection.
     * Updates quantity if the card already exists.
     */
    public void addCardsToCollection(String cardId, int amount) {
        if (amount <= 0) return; // Do nothing if trying to add zero or less

        SQLiteDatabase db = this.getWritableDatabase();

        // 1. Check current quantity and calculate new quantity (UPSERT pattern)
        String selectQuery = "SELECT quantity FROM collection WHERE card_id = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{cardId});

        int newQuantity = amount;

        if (cursor.moveToFirst()) {
            int currentQuantity = cursor.getInt(0);
            newQuantity += currentQuantity;

            // 2. Update existing quantity
            ContentValues values = new ContentValues();
            values.put("quantity", newQuantity);
            db.update("collection", values, "card_id = ?", new String[]{cardId});
        } else {
            // 3. Insert new card into collection
            ContentValues values = new ContentValues();
            values.put("card_id", cardId);
            values.put("quantity", newQuantity);
            db.insert("collection", null, values);
        }
        cursor.close();
    }

    // --- CRUD FUNCTIONS ---

    // --- 2. GET THE DECK (Rebuilds your Java Objects using in_use_quantity from collection) ---
    public List<Card> getActiveDeck() {
        List<Card> deckList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM cards_library INNER JOIN collection ON cards_library.id = collection.card_id";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("card_class"));
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int cost = cursor.getInt(cursor.getColumnIndexOrThrow("cost"));
                int inUseQty = cursor.getInt(cursor.getColumnIndexOrThrow("in_use_quantity"));

                if (inUseQty > 0) {  // Only include cards that are in use
                    Card card;
                    if (type.equals("FIGHTER")) {
                        int hp = cursor.getInt(cursor.getColumnIndexOrThrow("hp"));
                        int atk = cursor.getInt(cursor.getColumnIndexOrThrow("atk"));
                        card = new FighterCard(id, name, cost, hp, atk);
                    } else {
                        String effectStr = cursor.getString(cursor.getColumnIndexOrThrow("effect_type"));
                        int val = cursor.getInt(cursor.getColumnIndexOrThrow("effect_value"));
                        card = new EffectCard(id, name, cost, EffectCard.EffectType.valueOf(effectStr), val);
                    }

                    // Add the card the number of times it's in use
                    for (int i = 0; i < inUseQty; i++) {
                        deckList.add(card);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return deckList;
    }

    // --- 1. UPDATE DECK (Add/Remove card based on boolean flag) ---
    public void updateCardInDeck(String cardId, boolean addCard) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query =
                "SELECT quantity, in_use_quantity " +
                        "FROM collection " +
                        "WHERE card_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{cardId});

        if (!cursor.moveToFirst()) {
            // Card not owned → cannot add or remove
            cursor.close();
            return;
        }

        int ownedQuantity = cursor.getInt(0);
        int inUseQuantity = cursor.getInt(1);

        ContentValues values = new ContentValues();

        if (addCard) {
            // ✅ ADD CARD TO DECK
            if (inUseQuantity < ownedQuantity) {
                values.put("in_use_quantity", inUseQuantity + 1);
                db.update("collection", values, "card_id = ?", new String[]{cardId});
            }
            // else → cannot add (deck already uses all owned cards)
        } else {
            // ✅ REMOVE CARD FROM DECK
            if (inUseQuantity > 0) {
                values.put("in_use_quantity", inUseQuantity - 1);
                db.update("collection", values, "card_id = ?", new String[]{cardId});
            }
        }

        cursor.close();
    }



    // --- 3. CLEAR DECK (Reset in_use_quantity for all cards in collection) ---
    public void clearDeck() {
        SQLiteDatabase db = this.getWritableDatabase();

        // Reset the in_use_quantity for all cards in the collection (clear deck)
        ContentValues values = new ContentValues();
        values.put("in_use_quantity", 0);

        db.update("collection", values, null, null);
    }


    public int getDeckSize() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(in_use_quantity) FROM collection",
                null
        );

        int deckSize = 0;
        if (cursor.moveToFirst()) {
            deckSize = cursor.getInt(0);
        }
        cursor.close();
        return deckSize;
    }



}