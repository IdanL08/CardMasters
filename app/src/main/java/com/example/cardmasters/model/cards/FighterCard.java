package com.example.cardmasters.model.cards;

import com.example.cardmasters.model.Effect;

import java.util.ArrayList;
import java.util.List;


public class FighterCard extends Card {
    private int hp;
    private int atk;
    private int currentDamage = 0; // Tracking damage taken
    private List<Effect> activeEffects = new ArrayList<>();

    public FighterCard() { super(); }



    public FighterCard(String id, String name, int cost, int hp, int atk) {
        super(id, name, cost);
        this.hp = hp;
        this.atk = atk;
    }

    // --- LOGIC METHODS ---

    public int getAtk() {
        int atk = this.atk;
        for (Effect e : activeEffects) {
            if (e.getTarget() == Effect.Target.ATK) atk = e.apply(atk);
        }
        return atk;
    }

    public int getHp() {
        int hp = this.hp;
        for (Effect e : activeEffects) {
            if (e.getTarget() == Effect.Target.HP) hp = e.apply(hp);
        }
        return hp - currentDamage;
    }

    public void takeDamage(int amount) {
        this.currentDamage += amount;
    }

    public boolean isDead() {
        return getHp() <= 0;
    }

    public void onDeath(){
        //apply on death effect
    }

    public void addEffect(Effect effect) {
        activeEffects.add(effect);
    }

    public FighterCard(String id, String name, int hp, int atk, List<Effect> activeEffects) {
        super(id, name);
        this.hp = hp;
        this.atk = atk;
        this.activeEffects = activeEffects;
    }

    public FighterCard(String id, String name, int cost, int hp, List<Effect> activeEffects, int currentDamage, int atk) {
        super(id, name, cost);
        this.hp = hp;
        this.activeEffects = activeEffects;
        this.currentDamage = currentDamage;
        this.atk = atk;
    }

    @Override
    public FighterCard cloneCard() {
        FighterCard clone = new FighterCard(id, name, cost, hp, atk);
        clone.currentDamage = this.currentDamage;
        // Deep copy the effects
        for (Effect e : this.activeEffects) {
            clone.addEffect(new Effect(e.getTarget(), e.getType(), e.getValue()));
        }
        return clone;
    }



    public List<Effect> getActiveEffects() { return activeEffects; }
}