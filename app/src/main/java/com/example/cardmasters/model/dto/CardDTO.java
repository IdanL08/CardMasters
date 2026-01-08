package com.example.cardmasters.model.dto;

import java.util.List;

public class CardDTO {

    private String cardId;            // "fishboy", "emp"
    private String type;              // "FIGHTER" or "EFFECT"

    // Fighter stats only relevant if type == FIGHTER
    private int hp;
    private int atk;

    private List<EffectDTO> appliedEffects;

    public CardDTO() {}

    public CardDTO(String cardId, String type, int hp, int atk, List<EffectDTO> appliedEffects) {
        this.cardId = cardId;
        this.type = type;
        this.hp = hp;
        this.atk = atk;
        this.appliedEffects = appliedEffects;
    }

    // Getters & Setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public int getAtk() { return atk; }
    public void setAtk(int atk) { this.atk = atk; }

    public List<EffectDTO> getAppliedEffects() { return appliedEffects; }
    public void setAppliedEffects(List<EffectDTO> appliedEffects) { this.appliedEffects = appliedEffects; }
}
