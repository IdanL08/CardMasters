package com.example.cardmasters.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cardmasters.model.Effect;
import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;

import java.util.ArrayList;
import java.util.List;

public class CardDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CardMasters.db";
    private static final int DATABASE_VERSION = 3; // Incremented for schema simplification
    public static final int MAX_DECK_SIZE = 4;

    public CardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. MASTER LIBRARY (One table for all card definitions)
        db.execSQL("CREATE TABLE cards_library (" +
                "id TEXT PRIMARY KEY, " +
                "card_class TEXT, " +
                "name TEXT, " +
                "cost INTEGER, " +
                "hp INTEGER, " + // Used by Fighter
                "atk INTEGER, " + // Used by Fighter
                "effect_target TEXT, " + // Used by Effect cards
                "effect_type TEXT, " +   // Used by Effect cards
                "effect_value INTEGER)"); // Used by Effect cards

        // 2. THE COLLECTION (Tracks player-owned cards)
        db.execSQL("CREATE TABLE collection (" +
                "id TEXT PRIMARY KEY, " +
                "quantity INTEGER, " +
                "in_use_quantity INTEGER, " +
                "FOREIGN KEY(id) REFERENCES cards_library(id))");

        preLoadCards(db);
    }

    private void preLoadCards(SQLiteDatabase db) {
        db.execSQL("DELETE FROM cards_library");

        // Fighter Cards
        insertFighter(db, "fishboy", "Fish Boy", 1, 12, 4);
        insertFighter(db, "nepton", "Nepton", 5, 20, 6);
        insertFighter(db, "sponge", "Sponge", 2, 8, 2);
        insertFighter(db, "gambler", "Gambler", 4, 15, 5);
        insertFighter(db, "ghoul_cowboy", "Ghoul Cowboy", 4, 18, 5);
        insertFighter(db, "robo_cowboy", "Robo Cowboy", 4, 16, 6);

        // Effect Cards (Stored in the same table)
        insertEffectCard(db, "emp", "EMP", 3, Effect.Target.ATK, Effect.Type.ADD, 3);
    }

    private void insertFighter(SQLiteDatabase db, String id, String name, int cost, int hp, int atk) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("card_class", "FIGHTER");
        values.put("name", name);
        values.put("cost", cost);
        values.put("hp", hp);
        values.put("atk", atk);
        db.insert("cards_library", null, values);
    }

    private void insertEffectCard(SQLiteDatabase db, String id, String name, int cost, Effect.Target target, Effect.Type type, int value) {
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("card_class", "EFFECT");
        values.put("name", name);
        values.put("cost", cost);
        values.put("effect_target", target.name());
        values.put("effect_type", type.name());
        values.put("effect_value", value);
        db.insert("cards_library", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS cards_library");
        db.execSQL("DROP TABLE IF EXISTS card_effects"); // Clean up old table if exists
        db.execSQL("DROP TABLE IF EXISTS collection");
        onCreate(db);
    }

    public List<Card> getActiveDeck() {
        List<Card> deckList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM cards_library INNER JOIN collection ON cards_library.id = collection.id";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String cardClass = cursor.getString(cursor.getColumnIndexOrThrow("card_class"));
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int cost = cursor.getInt(cursor.getColumnIndexOrThrow("cost"));
                int inUseQty = cursor.getInt(cursor.getColumnIndexOrThrow("in_use_quantity"));

                if (inUseQty > 0) {
                    Card card;
                    if ("FIGHTER".equals(cardClass)) {
                        int hp = cursor.getInt(cursor.getColumnIndexOrThrow("hp"));
                        int atk = cursor.getInt(cursor.getColumnIndexOrThrow("atk"));
                        card = new FighterCard(id, name, cost, hp, atk);
                    } else {
                        String targetStr = cursor.getString(cursor.getColumnIndexOrThrow("effect_target"));
                        String typeStr = cursor.getString(cursor.getColumnIndexOrThrow("effect_type"));
                        int val = cursor.getInt(cursor.getColumnIndexOrThrow("effect_value"));
                        Effect effect = new Effect(Effect.Target.valueOf(targetStr), Effect.Type.valueOf(typeStr), val);
                        card = new EffectCard(id, name, cost, effect);
                    }

                    for (int i = 0; i < inUseQty; i++) {
                        deckList.add(card.cloneCard());
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return deckList;
    }

    // Remaining methods (addCardsToCollection, updateCardInDeck, clearDeck, getDeckSize)
    // stay the same as they only interact with the 'collection' table.

    public void addCardsToCollection(String cardId, int amount) {
        if (amount <= 0) return;
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT quantity FROM collection WHERE id = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{cardId});
        int newQuantity = amount;
        if (cursor.moveToFirst()) {
            int currentQuantity = cursor.getInt(0);
            newQuantity += currentQuantity;
            ContentValues values = new ContentValues();
            values.put("quantity", newQuantity);
            db.update("collection", values, "id = ?", new String[]{cardId});
        } else {
            ContentValues values = new ContentValues();
            values.put("id", cardId);
            values.put("quantity", newQuantity);
            values.put("in_use_quantity", 0);
            db.insert("collection", null, values);
        }
        cursor.close();
    }

    public void updateCardInDeck(String cardId, boolean addCard) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT quantity, in_use_quantity FROM collection WHERE id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{cardId});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        int ownedQuantity = cursor.getInt(0);
        int inUseQuantity = cursor.getInt(1);
        ContentValues values = new ContentValues();
        if (addCard) {
            if (inUseQuantity < ownedQuantity) {
                values.put("in_use_quantity", inUseQuantity + 1);
                db.update("collection", values, "id = ?", new String[]{cardId});
            }
        } else {
            if (inUseQuantity > 0) {
                values.put("in_use_quantity", inUseQuantity - 1);
                db.update("collection", values, "id = ?", new String[]{cardId});
            }
        }
        cursor.close();
    }

    public void clearDeck() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("in_use_quantity", 0);
        db.update("collection", values, null, null);
    }

    public int getDeckSize() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(in_use_quantity) FROM collection", null);
        int deckSize = 0;
        if (cursor.moveToFirst()) {
            deckSize = cursor.getInt(0);
        }
        cursor.close();
        return deckSize;
    }
}