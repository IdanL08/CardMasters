package com.example.cardmasters.model.cards;

import com.example.cardmasters.model.Effect;

public class EffectCard extends Card {
    private Effect effectPayload;

    public EffectCard() { super(); }

    public EffectCard(String id, String name, int cost, Effect payload) {
        super(id, name, cost);
        this.effectPayload = payload;
    }

    @Override
    public EffectCard cloneCard() {
        return new EffectCard(id, name, cost,
                new Effect(effectPayload.getTarget(), effectPayload.getType(), effectPayload.getValue()));
    }

    // Getter/Setter for Firebase
    public Effect getEffectPayload() { return effectPayload; }
}