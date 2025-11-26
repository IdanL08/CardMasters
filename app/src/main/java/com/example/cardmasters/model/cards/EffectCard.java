package com.example.cardmasters.model.cards;

public class EffectCard extends Card {

    public enum EffectType {
        ADD_HP, ADD_ATK,
        MULT_HP, MULT_ATK
    }

    private EffectType type;
    private int value;

    public EffectCard(String id, String name, int cost, EffectType type, int value) {
        super(id, name, cost);
        this.type = type;
        this.value = value;
    }

    public EffectType getType() { return type; }
    public int getValue() { return value; }

    @Override
    public EffectCard cloneCard() {
        return new EffectCard(id, name, cost, type, value);
    }
}

