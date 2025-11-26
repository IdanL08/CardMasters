package com.example.cardmasters.model;

import java.util.Arrays;

public class Game {
    public static final int NUM_LANES = 5;

    private Lane[] playerLanes = new Lane[NUM_LANES];
    private Lane[] enemyLanes = new Lane[NUM_LANES];

    public Game() {
        for (int i = 0; i < NUM_LANES; i++) {
            playerLanes[i] = new Lane();
            enemyLanes[i] = new Lane();
        }
    }

    public Lane getPlayerLane(int i) { return playerLanes[i]; }
    public Lane getEnemyLane(int i) { return enemyLanes[i]; }

    public Lane[] getPlayerLanes() { return playerLanes; }
    public Lane[] getEnemyLanes() { return enemyLanes; }
}
