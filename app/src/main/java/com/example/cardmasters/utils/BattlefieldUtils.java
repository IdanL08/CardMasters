package com.example.cardmasters.utils;

import com.example.cardmasters.model.cards.FighterCard;
import com.example.cardmasters.model.Hero;
import android.util.Log;
import java.util.List;

public class BattlefieldUtils {

    private static final String TAG = "BattlefieldUtils";
    public static final int NUM_LANES = 5;

    /**
     * Settles the entire battlefield.
     * Requires the two Heroes to handle direct damage.
     */
    public static void fieldBattle(List<FighterCard> playerLanes,
                                   List<FighterCard> enemyLanes,
                                   Hero playerHero,
                                   Hero enemyHero) {

        for (int i = 0; i < NUM_LANES; i++) {
            laneBattle(i, playerLanes, enemyLanes, playerHero, enemyHero);
        }
        handleDeaths(playerLanes, enemyLanes);
    }

    private static void handleDeaths(List<FighterCard> playerLanes, List<FighterCard> enemyLanes) {
        for (int i = 0; i < NUM_LANES; i++) {

            if (enemyLanes.get(i) != null && enemyLanes.get(i).isDead()&&playerLanes.get(i) != null && playerLanes.get(i).isDead()){
                playerLanes.get(i).onDeath();
                enemyLanes.get(i).onDeath();
                playerLanes.set(i, null);
                enemyLanes.set(i, null);
                handleDeaths(playerLanes, enemyLanes);
            }

            // Handle Player Deaths
            if (playerLanes.get(i) != null && playerLanes.get(i).isDead()) {
                playerLanes.get(i).onDeath();
                playerLanes.set(i, null);
            }

            // Handle Enemy Deaths
            if (enemyLanes.get(i) != null && enemyLanes.get(i).isDead()) {
                enemyLanes.get(i).onDeath();
                enemyLanes.set(i, null);
            }
        }
    }

    /**
     * Handles all possibilities for a single lane.
     */
    private static void laneBattle(int laneIndex,
                                   List<FighterCard> pLanes,
                                   List<FighterCard> eLanes,
                                   Hero pHero,
                                   Hero eHero) {

        FighterCard pCard = pLanes.get(laneIndex);
        FighterCard eCard = eLanes.get(laneIndex);

        // CASE 1: Both lanes have cards (FIGHT!)
        if (pCard != null && eCard != null) {
            int pAtk = pCard.getAtk();
            int eAtk = eCard.getAtk();

            pCard.takeDamage(eAtk);
            eCard.takeDamage(pAtk);

            // Post-combat effects (as we planned before)
            applyAfterAttackEffects(pCard);
            applyAfterAttackEffects(eCard);

            // Clean up dead cards
            if (pCard.isDead()) pLanes.set(laneIndex, null);
            if (eCard.isDead()) eLanes.set(laneIndex, null);
        }

        // CASE 2: Player has a card, Enemy lane is empty (DIRECT ATTACK)
        else if (pCard != null) {
            int damage = pCard.getAtk();
            Log.d(TAG, pCard.getName() + " attacks Enemy Hero for " + damage);
            eHero.takeDamage(damage);
            applyAfterAttackEffects(pCard);
        }

        // CASE 3: Enemy has a card, Player lane is empty (DIRECT ATTACK)
        else if (eCard != null) {
            int damage = eCard.getAtk();
            Log.d(TAG, eCard.getName() + " attacks Player Hero for " + damage);
            pHero.takeDamage(damage);
            applyAfterAttackEffects(eCard);
        }

        // CASE 4: Both are null (Do nothing)
    }

    private static void applyAfterAttackEffects(FighterCard card) {
        if (card == null || card.isDead()) return;
        // Logic for "After Attack" effects here...
        Log.d(TAG, "Triggering effects for survivor: " + card.getName());
    }
}