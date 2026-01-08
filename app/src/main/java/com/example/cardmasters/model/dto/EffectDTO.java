package com.example.cardmasters.model.dto;

public class EffectDTO {

    private String target; // HP, ATK
    private String type;   // ADD, MULTIPLY
    private int value;

    public EffectDTO() {
        // Required for Firebase
    }

    public EffectDTO(String target, String type, int value) {
        this.target = target;
        this.type = type;
        this.value = value;
    }

    // Getters & Setters
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
