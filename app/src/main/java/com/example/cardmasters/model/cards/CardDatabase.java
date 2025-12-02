package com.example.cardmasters.model.cards;

import java.util.HashMap;
import java.util.Map;

public class CardDatabase {

    private static final Map<String, Card> cards = new HashMap<>();

    static {
        // Define all your cards here
        cards.put("larry", new FighterCard("larry", "Larry", 3, 5, 3));
        cards.put("brute", new FighterCard("brute", "Brute", 5, 8, 6));
        // Add more cards...
    }

    public static Card getCardPrototype(String id) {
        Card c = cards.get(id);
        if (c != null) return c.cloneCard();
        else throw new IllegalArgumentException("Card not found: " + id);
    }
}
