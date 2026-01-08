package com.example.cardmasters.model;

public class Effect {
    public enum Type { ADD, MULTIPLY }
    public enum Target { HP, ATK }

    private Target target;
    private Type type;
    private int value;

    // Required for Firebase
    public Effect() {}

    public Effect(Target target, Type type, int value) {
        this.target = target;
        this.type = type;
        this.value = value;
    }

    public int apply(int baseValue) {
        if (type == Type.ADD) return baseValue + value;
        if (type == Type.MULTIPLY) return baseValue * value;
        return baseValue;
    }

    // Getters and Setters for Firebase
    public Target getTarget() { return target; }
    public Type getType() { return type; }
    public int getValue() { return value; }
}