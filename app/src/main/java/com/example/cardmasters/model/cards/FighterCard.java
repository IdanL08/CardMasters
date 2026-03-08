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

    public FighterCard(String id, String name, int hp, int atk, List<Effect> activeEffects) {
        super(id, name);
        this.hp = hp;
        this.atk = atk;
        this.activeEffects = activeEffects != null ? activeEffects : new ArrayList<>();
    }

    public FighterCard(String id, String name, int cost, int hp, List<Effect> activeEffects, int currentDamage, int atk) {
        super(id, name, cost);
        this.hp = hp;
        this.activeEffects = activeEffects != null ? activeEffects : new ArrayList<>();
        this.currentDamage = currentDamage;
        this.atk = atk;
    }

    // --- LOGIC METHODS ---

    public int getAtk() {
        return this.atk; // מחזיר ישירות בלי חישובים באוויר
    }

    public int getHp() {
        return this.hp - currentDamage; // מחזיר ישירות
    }

    public void takeDamage(int amount) {
        this.currentDamage += amount;
    }

    public boolean isDead() {
        return getHp() <= 0;
    }

    public void onDeath(){
        // apply on death effect
    }

    /**
     * פונקציה קריטית: אם האפקט הוא כוח או חיים - הוא משנה את הסטאטים מיד.
     * אם זה התקפה נוספת או שליפת קלף - הוא נשמר ברשימה לשימוש עתידי.
     */
    public void addEffect(Effect effect) {
        if (effect.getTarget() == Effect.Target.ATK) {
            this.atk = effect.apply(this.atk);
        } else if (effect.getTarget() == Effect.Target.HP) {
            this.hp = effect.apply(this.hp);
        } else {
            activeEffects.add(effect);
        }
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

    public List<Effect> getActiveEffects() {
        return activeEffects;
    }
}