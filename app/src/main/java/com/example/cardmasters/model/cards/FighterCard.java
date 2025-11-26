package com.example.cardmasters.model.cards;

import java.util.ArrayList;
import java.util.List;

public class FighterCard extends Card {
    private int baseHp;
    private int baseAtk;

    private int currentHp;
    private int currentAtk;

    private List<EffectCard> activeEffects = new ArrayList<>();

    public FighterCard(String id, String name, int cost, int hp, int atk) {
        super(id, name, cost);
        this.baseHp = hp;
        this.baseAtk = atk;
        this.currentHp = hp;
        this.currentAtk = atk;
    }

    public int getHp() { return currentHp; }
    public int getAtk() { return currentAtk; }

    public void addEffect(EffectCard effect) { activeEffects.add(effect); }
    public List<EffectCard> getEffects() { return activeEffects; }

    @Override
    public FighterCard cloneCard() {
        FighterCard c = new FighterCard(id, name, cost, baseHp, baseAtk);
        c.currentHp = this.currentHp;
        c.currentAtk = this.currentAtk;

        // Deep copy effects
        for (EffectCard e : this.activeEffects) {
            c.activeEffects.add((EffectCard) e.cloneCard());
        }

        return c;
    }
}
