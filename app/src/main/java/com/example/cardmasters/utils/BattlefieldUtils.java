package com.example.cardmasters.utils;

import android.util.Log;
import com.example.cardmasters.model.Effect;
import com.example.cardmasters.model.Hero;
import com.example.cardmasters.model.cards.FighterCard;
// אל תשכח לייבא את המחלקות של Hero ושאר הדברים שלך

import java.util.Iterator;
import java.util.List;

public class BattlefieldUtils {//(8)

    public static final int NUM_STARTING_CARDS = 4;
    private static final String TAG = "BattlefieldUtils";
    public static final int NUM_LANES = 5;

    public static void fieldBattle(List<FighterCard> firstLanes,
                                   List<FighterCard> secondLanes,
                                   Hero firstHero,
                                   Hero secondHero) {

        for (int i = 0; i < NUM_LANES; i++) {
            laneBattle(i, firstLanes, secondLanes, firstHero, secondHero);
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

    /**
     * פונקציה שסופרת התקפות בונוס ו"שורפת" אותן (מוחקת מהרשימה)
     */
    private static int consumeBonusAttacks(FighterCard card) {
        if (card == null || card.getActiveEffects() == null) return 0;

        int bonusCount = 0;
        Iterator<Effect> iterator = card.getActiveEffects().iterator();

        while (iterator.hasNext()) {
            Effect e = iterator.next();
            if (e.getTarget() == Effect.Target.BONUS_ATK) {
                bonusCount = e.apply(bonusCount);
                iterator.remove(); // מוחק את האפקט מיד כדי שלא יחזור תור הבא
            }
        }
        return bonusCount;
    }

    /**
     * פונקציה שבודקת האם הקלף צריך לשלוף קלפים אחרי התקפה (PvZ Heroes style)
     */
    private static void handleOnAttackEffects(FighterCard attacker, Hero attackerHero) {
        if (attacker == null || attacker.isDead() || attacker.getActiveEffects() == null) return;

        for (Effect e : attacker.getActiveEffects()) {
            if (e.getTarget() == Effect.Target.DRAW_CARD) {
                int cardsToDraw = e.apply(0); // מחשב כמה קלפים לשלוף לפי ה-value של האפקט
                Log.d(TAG, attacker.getName() + " draws " + cardsToDraw + " cards!");
                // כאן תקרא לפונקציה של הגיבור/שחקן ששולפת קלפים:
                // attackerHero.drawCards(cardsToDraw);
            }
        }
    }

    /**
     * מבצע את סדרת התקפות הבונוס של קלף מסוים.
     */
    private static void performBonusAttacks(FighterCard attacker, FighterCard defender, Hero defendingHero, Hero attackingHero) {
        if (attacker == null || attacker.isDead()) return;

        int bonusCount = consumeBonusAttacks(attacker);

        for (int i = 0; i < bonusCount; i++) {
            if (attacker.isDead()) break;

            int dmg = attacker.getAtk();

            if (defender != null && !defender.isDead()) {
                Log.d(TAG, attacker.getName() + " BONUS ATTACKS " + defender.getName() + " for " + dmg);
                defender.takeDamage(dmg);
            } else {
                Log.d(TAG, attacker.getName() + " BONUS ATTACKS Hero for " + dmg);
                defendingHero.takeDamage(dmg);
            }

            // קורא לאפקטים שקורים אחרי כל מכה (כמו שליפת קלף)
            handleOnAttackEffects(attacker, attackingHero);
        }
    }

    private static void laneBattle(int laneIndex,
                                   List<FighterCard> firstLanes,
                                   List<FighterCard> secondLanes,
                                   Hero firstHero,
                                   Hero secondHero) {

        FighterCard firstCard = firstLanes.get(laneIndex);
        FighterCard secondCard = secondLanes.get(laneIndex);

        // ==========================================
        // שלב 1: התקפות בונוס (לפני הקרב הרגיל)
        // ==========================================

        performBonusAttacks(firstCard, secondCard, secondHero, firstHero);
        performBonusAttacks(secondCard, firstCard, firstHero, secondHero);

        // מעדכנים את הלוח במקרה שמישהו מת מהתקפות הבונוס
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

        // ==========================================
        // שלב 2: הקרב הרגיל והסימולטני
        // ==========================================

        if (firstCard != null && secondCard != null) {
            int pAtk = firstCard.getAtk();
            int eAtk = secondCard.getAtk();

            firstCard.takeDamage(eAtk);
            secondCard.takeDamage(pAtk);

            // הפעלת אפקטים שאחרי התקפה רגילה
            handleOnAttackEffects(firstCard, firstHero);
            handleOnAttackEffects(secondCard, secondHero);

        } else if (firstCard != null) {
            int damage = firstCard.getAtk();
            Log.d(TAG, firstCard.getName() + " attacks Enemy Hero for " + damage);
            secondHero.takeDamage(damage);
            handleOnAttackEffects(firstCard, firstHero);

        } else if (secondCard != null) {
            int damage = secondCard.getAtk();
            Log.d(TAG, secondCard.getName() + " attacks Player Hero for " + damage);
            firstHero.takeDamage(damage);
            handleOnAttackEffects(secondCard, secondHero);
        }
    }
}