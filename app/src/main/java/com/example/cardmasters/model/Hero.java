package com.example.cardmasters.model;

public class Hero {
    private String name;
    private int health;

    public Hero(String name, int health) {
        this.name = name;
        this.health = health;
    }

    public void takeDamage(int amount) {
        this.health -= amount;

    }

    public int getHealth() { return health; }
    public boolean isDefeated() { return health <= 0; }

    public void setHealth(int health) {
        this.health = health;
    }
}