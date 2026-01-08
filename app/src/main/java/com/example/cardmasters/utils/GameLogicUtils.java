package com.example.cardmasters.utils;

import com.example.cardmasters.model.Effect;
import com.example.cardmasters.model.cards.FighterCard;
import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.EffectDTO;

public class GameLogicUtils {

    public static FighterCard mapDtoToFighter(CardDTO dto) {
        FighterCard f = new FighterCard(dto.getCardId(), dto.getCardId(), 0, dto.getHp(), dto.getAtk());
        if (dto.getAppliedEffects() != null) {
            for (EffectDTO e : dto.getAppliedEffects()) {
                f.addEffect(new Effect(
                        "HP".equals(e.getTarget()) ? Effect.Target.HP : Effect.Target.ATK,
                        "MULT".equals(e.getType()) ? Effect.Type.MULTIPLY : Effect.Type.ADD,
                        e.getValue()
                ));
            }
        }
        return f;
    }

    public static Effect lookupEffectById(String id) {
        switch (id) {
            case "emp": return new Effect(Effect.Target.ATK, Effect.Type.ADD, -2);
            case "stim": return new Effect(Effect.Target.ATK, Effect.Type.MULTIPLY, 2);
            case "potion": return new Effect(Effect.Target.HP, Effect.Type.ADD, 5);
            default: return new Effect(Effect.Target.HP, Effect.Type.ADD, 0);
        }
    }
}