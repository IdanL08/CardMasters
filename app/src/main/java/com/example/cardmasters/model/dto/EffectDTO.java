package com.example.cardmasters.model.dto;

public class EffectDTO {

    private String effectId;      // Unique ID for effect
    private String effectType;    // BUFF / DEBUFF / STATUS
    private int amount;           // +5 ATK, -3 HP, etc.
    private int duration;         // 0 = instant, >0 = number of turns

    public EffectDTO() {
        // Required for Firebase
    }

    public EffectDTO(String effectId, String effectType, int amount, int duration) {
        this.effectId = effectId;
        this.effectType = effectType;
        this.amount = amount;
        this.duration = duration;
    }

    // Getters & Setters
    public String getEffectId() { return effectId; }
    public void setEffectId(String effectId) { this.effectId = effectId; }

    public String getEffectType() { return effectType; }
    public void setEffectType(String effectType) { this.effectType = effectType; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}