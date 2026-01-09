package com.example.cardmasters.model.cards;

public abstract class Card {
    protected String id;
    protected String name;
    protected int cost;

    public Card(String id, String name, int cost) {
        this.id = id;
        this.name = name;
        this.cost = cost;
    }
    public Card() {} // Required for Firebase

    public Card(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCost() { return cost; }

    // returns a fresh copy of this card
    public abstract Card cloneCard();
}
