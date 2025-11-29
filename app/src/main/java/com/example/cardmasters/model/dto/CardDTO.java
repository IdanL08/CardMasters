package com.example.cardmasters.model.dto;

import java.util.List;


public class CardDTO {

    private String cardId;            // "fighter_slime", "buff_attack"
    private String type;              // "FIGHTER" or "EFFECT"

    // Fighter stats only relevant if type == FIGHTER
    private int baseHp;
    private int baseAttack;

    private List<EffectDTO> appliedEffects;

    public CardDTO() {}

    public CardDTO(String cardId, String type, int baseHp, int baseAttack, List<EffectDTO> appliedEffects) {
        this.cardId = cardId;
        this.type = type;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.appliedEffects = appliedEffects;
    }

    // Getters & Setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getBaseHp() { return baseHp; }
    public void setBaseHp(int baseHp) { this.baseHp = baseHp; }

    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }

    public List<EffectDTO> getAppliedEffects() { return appliedEffects; }
    public void setAppliedEffects(List<EffectDTO> appliedEffects) { this.appliedEffects = appliedEffects; }
}