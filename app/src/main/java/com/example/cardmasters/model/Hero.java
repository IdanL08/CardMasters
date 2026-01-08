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
        if (this.health < 0) this.health = 0;
    }

    public int getHealth() { return health; }
    public boolean isDefeated() { return health <= 0; }
}