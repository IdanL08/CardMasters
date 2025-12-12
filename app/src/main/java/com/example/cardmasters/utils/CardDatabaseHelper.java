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

        // 2. THE DECK (Active deck for combat)
        db.execSQL("CREATE TABLE deck (" +
                "card_id TEXT PRIMARY KEY, " +
                "quantity INTEGER, " +
                "FOREIGN KEY(card_id) REFERENCES cards_library(id))");

        // 3. THE COLLECTION (Tracks player-owned cards and quantities)
        db.execSQL("CREATE TABLE collection (" +
                "card_id TEXT PRIMARY KEY, " +
                "quantity INTEGER, " +
                "FOREIGN KEY(card_id) REFERENCES cards_library(id))");


        // --- PRE-LOAD DATA ---
        preLoadCards(db);
    }

    private void preLoadCards(SQLiteDatabase db) {
        // Fighter Card Examples
        db.execSQL("INSERT INTO cards_library VALUES ('f1', 'FIGHTER', 'Knight', 3, 10, 5, NULL, NULL)");
        db.execSQL("INSERT INTO cards_library VALUES ('f2', 'FIGHTER', 'Orc', 4, 15, 7, NULL, NULL)");

        // Effect Card Examples
        db.execSQL("INSERT INTO cards_library VALUES ('e1', 'EFFECT', 'Heal', 2, NULL, NULL, 'ADD_HP', 5)");
        db.execSQL("INSERT INTO cards_library VALUES ('e2', 'EFFECT', 'Rage', 2, NULL, NULL, 'ADD_ATK', 3)");

        /*// OPTIONAL: Give the player a starting collection immediately
         addCardsToCollection("f1", 3);
         addCardsToCollection( "e1", 2);*/
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cards_library");
        db.execSQL("DROP TABLE IF EXISTS deck");
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

    // 1. UPDATE DECK (Add/Change quantity)
    public void updateCardInDeck(String cardId, int quantity) {//מחליף את הכמות במקום להוסיף או לחסר
        SQLiteDatabase db = this.getWritableDatabase();
        if (quantity <= 0) {
            db.delete("deck", "card_id = ?", new String[]{cardId});
        } else {
            ContentValues values = new ContentValues();
            values.put("card_id", cardId);
            values.put("quantity", quantity);
            db.insertWithOnConflict("deck", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    // 2. GET THE DECK (Rebuilds your Java Objects)
    public List<Card> getActiveDeck() {
        List<Card> deckList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM cards_library INNER JOIN deck ON cards_library.id = deck.card_id";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("card_class"));
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int cost = cursor.getInt(cursor.getColumnIndexOrThrow("cost"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));

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

                for (int i = 0; i < qty; i++) {
                    deckList.add(card);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return deckList;
    }

    // 3. CLEAR DECK (Delete everything in deck)
    public void clearDeck() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM deck");
    }


}