package com.example.cardmasters.model.cards;

import com.example.cardmasters.model.Effect;
import com.example.cardmasters.utils.CardDatabaseHelper;

public class EffectCard extends Card {
    private Effect effectPayload;

    public EffectCard() { super(); }

    public EffectCard(String id, String name, int cost, Effect payload) {
        super(id, name, cost);
        this.effectPayload = payload;
    }

    public EffectCard(String id, String name, int cost) {
        super(id, name, cost);
        this.effectPayload= getEffectById();
    }

    public EffectCard(String cardId) {
        this.id=cardId;
        this.effectPayload= getEffectById();
    }

    public void applyEffect(FighterCard fighterCard){
        if(fighterCard==null)return;
        fighterCard.getActiveEffects().add(effectPayload);

    }

    public Effect getEffectById(){
        //TODO ליישם getcardbyid return
        return new Effect(Effect.Target.ATK, Effect.Type.ADD, 3);
        }

    @Override
    public EffectCard cloneCard() {
        return new EffectCard(id, name, cost,
                new Effect(effectPayload.getTarget(), effectPayload.getType(), effectPayload.getValue()));
    }

    // Getter/Setter for Firebase
    public Effect getEffectPayload() { return effectPayload; }
}