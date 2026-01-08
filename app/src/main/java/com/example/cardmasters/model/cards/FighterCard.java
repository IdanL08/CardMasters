package com.example.cardmasters.model.cards;

import com.example.cardmasters.model.Effect;

import java.util.ArrayList;
import java.util.List;


public class FighterCard extends Card {
    private int baseHp;
    private int baseAtk;
    private int currentDamage = 0; // Tracking damage taken
    private List<Effect> activeEffects = new ArrayList<>();

    public FighterCard() { super(); }

    public FighterCard(String id, String name, int cost, int hp, int atk) {
        super(id, name, cost);
        this.baseHp = hp;
        this.baseAtk = atk;
    }

    // --- LOGIC METHODS ---

    public int getFinalAtk() {
        int atk = baseAtk;
        for (Effect e : activeEffects) {
            if (e.getTarget() == Effect.Target.ATK) atk = e.apply(atk);
        }
        return atk;
    }

    public int getFinalHp() {
        int hp = baseHp;
        for (Effect e : activeEffects) {
            if (e.getTarget() == Effect.Target.HP) hp = e.apply(hp);
        }
        return hp - currentDamage;
    }

    public void takeDamage(int amount) {
        this.currentDamage += amount;
    }

    public boolean isDead() {
        return getFinalHp() <= 0;
    }

    public void addEffect(Effect effect) {
        activeEffects.add(effect);
    }

    @Override
    public FighterCard cloneCard() {
        FighterCard clone = new FighterCard(id, name, cost, baseHp, baseAtk);
        clone.currentDamage = this.currentDamage;
        // Deep copy the effects
        for (Effect e : this.activeEffects) {
            clone.addEffect(new Effect(e.getTarget(), e.getType(), e.getValue()));
        }
        return clone;
    }

    // Getters/Setters for Firebase
    public int getBaseHp() { return baseHp; }
    public int getBaseAtk() { return baseAtk; }
    public List<Effect> getActiveEffects() { return activeEffects; }
}