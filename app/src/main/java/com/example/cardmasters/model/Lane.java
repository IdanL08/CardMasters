package com.example.cardmasters.model;

import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;

import java.util.ArrayList;
import java.util.List;

public class Lane {
    private FighterCard fighter;
    private List<EffectCard> pendingEffects = new ArrayList<>();

    public boolean hasFighter() { return fighter != null; }
    public FighterCard getFighter() { return fighter; }
    public List<EffectCard> getPendingEffects() { return pendingEffects; }

    public void placeFighter(FighterCard f) {
        this.fighter = f.cloneCard();
    }

    public void addEffect(EffectCard e) {
        pendingEffects.add((EffectCard) e.cloneCard());
    }

    public void clearPendingEffects() {
        pendingEffects.clear();
    }
}
