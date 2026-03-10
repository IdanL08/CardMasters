package com.example.cardmasters.utils;

import android.util.Log;
import com.example.cardmasters.model.Effect;
import com.example.cardmasters.model.Hero;
import com.example.cardmasters.model.cards.FighterCard;

import java.util.Iterator;
import java.util.List;

public class BattlefieldUtils {

    public static final int NUM_STARTING_CARDS = 4;
    private static final String TAG = "BattlefieldUtils";
    public static final int NUM_LANES = 5;

    // הממשק שמונע ספגטי קוד - מעביר את האירועים חזרה ל-Activity
    public interface BattleEventsListener {
        void onCardDraw(Hero hero, int amount);
    }

    public static void fieldBattle(List<FighterCard> firstLanes,
                                   List<FighterCard> secondLanes,
                                   Hero firstHero,
                                   Hero secondHero,
                                   BattleEventsListener listener) { // הוספנו את המאזין

        for (int i = 0; i < NUM_LANES; i++) {
            laneBattle(i, firstLanes, secondLanes, firstHero, secondHero, listener);
        }
        handleDeaths(firstLanes, secondLanes);
    }

    private static void handleDeaths(List<FighterCard> firstLanes, List<FighterCard> secondLanes) {
        for (int i = 0; i < NUM_LANES; i++) {
            if (firstLanes.get(i) != null && firstLanes.get(i).isDead()) {
                firstLanes.get(i).onDeath();
                firstLanes.set(i, null);
            }
            if (secondLanes.get(i) != null && secondLanes.get(i).isDead()) {
                secondLanes.get(i).onDeath();
                secondLanes.set(i, null);
            }
        }
    }

    private static void handleOnAttackEffects(FighterCard attacker, Hero attackerHero, BattleEventsListener listener) {
        if (attacker == null || attacker.isDead() || attacker.getActiveEffects() == null) return;

        for (Effect e : attacker.getActiveEffects()) {
            if (e.getTarget() == Effect.Target.DRAW_CARD) {
                int cardsToDraw = e.apply(0);
                Log.d(TAG, attacker.getName() + " triggers draw effect for " + cardsToDraw + " cards!");
                // העברת הטיפול למי שמאזין (GameActivity) בלי לערבב UI
                if (listener != null) {
                    listener.onCardDraw(attackerHero, cardsToDraw);
                }
            }
        }
    }

    private static void executePreCombatTimeline(FighterCard attacker, FighterCard defender, Hero defendingHero, Hero attackingHero, BattleEventsListener listener) {
        if (attacker == null || attacker.isDead() || attacker.getActiveEffects() == null) return;

        int timelineAtk = attacker.getBaseAtk();
        Iterator<Effect> iterator = attacker.getActiveEffects().iterator();

        while (iterator.hasNext()) {
            Effect e = iterator.next();

            if (e.getTarget() == Effect.Target.ATK) {
                timelineAtk = e.apply(timelineAtk);
            }
            else if (e.getTarget() == Effect.Target.BONUS_ATK) {
                int bonusCount = e.apply(0);

                for (int i = 0; i < bonusCount; i++) {
                    if (attacker.isDead()) break;

                    if (defender != null && !defender.isDead()) {
                        Log.d(TAG, attacker.getName() + " BONUS ATTACKS " + defender.getName() + " for " + timelineAtk);
                        defender.takeDamage(timelineAtk);
                    } else {
                        Log.d(TAG, attacker.getName() + " BONUS ATTACKS Hero for " + timelineAtk);
                        defendingHero.takeDamage(timelineAtk);
                    }

                    handleOnAttackEffects(attacker, attackingHero, listener);
                }
                iterator.remove();
            }
        }
    }

    private static void laneBattle(int laneIndex,
                                   List<FighterCard> firstLanes,
                                   List<FighterCard> secondLanes,
                                   Hero firstHero,
                                   Hero secondHero,
                                   BattleEventsListener listener) {

        FighterCard firstCard = firstLanes.get(laneIndex);
        FighterCard secondCard = secondLanes.get(laneIndex);

        executePreCombatTimeline(firstCard, secondCard, secondHero, firstHero, listener);
        executePreCombatTimeline(secondCard, firstCard, firstHero, secondHero, listener);

        if (firstCard != null && firstCard.isDead()) {
            firstCard.onDeath();
            firstLanes.set(laneIndex, null);
            firstCard = null;
        }
        if (secondCard != null && secondCard.isDead()) {
            secondCard.onDeath();
            secondLanes.set(laneIndex, null);
            secondCard = null;
        }

        if (firstCard != null && secondCard != null) {
            int pAtk = firstCard.getAtk();
            int eAtk = secondCard.getAtk();

            firstCard.takeDamage(eAtk);
            secondCard.takeDamage(pAtk);

            handleOnAttackEffects(firstCard, firstHero, listener);
            handleOnAttackEffects(secondCard, secondHero, listener);

        } else if (firstCard != null) {
            int damage = firstCard.getAtk();
            Log.d(TAG, firstCard.getName() + " attacks Enemy Hero for " + damage);
            secondHero.takeDamage(damage);
            handleOnAttackEffects(firstCard, firstHero, listener);

        } else if (secondCard != null) {
            int damage = secondCard.getAtk();
            Log.d(TAG, secondCard.getName() + " attacks Player Hero for " + damage);
            firstHero.takeDamage(damage);
            handleOnAttackEffects(secondCard, secondHero, listener);
        }
    }
}