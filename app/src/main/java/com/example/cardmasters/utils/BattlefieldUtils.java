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
     * פונקציה שמדמה את ציר הזמן בדיוק לפי סדר הנחת הקלפים (פותר את בעיית הבונוס לפני באף כוח)
     */
    private static void executePreCombatTimeline(FighterCard attacker, FighterCard defender, Hero defendingHero, Hero attackingHero) {
        if (attacker == null || attacker.isDead() || attacker.getActiveEffects() == null) return;

        // מתחילים מהכוח הבסיסי של הקלף ומתקדמים לאורך ציר הזמן
        int timelineAtk = attacker.getBaseAtk();

        Iterator<Effect> iterator = attacker.getActiveEffects().iterator();

        while (iterator.hasNext()) {
            Effect e = iterator.next();

            // אם זה באף של כוח, אנחנו מעדכנים את הכוח הווירטואלי בציר הזמן
            if (e.getTarget() == Effect.Target.ATK) {
                timelineAtk = e.apply(timelineAtk);
            }
            // אם זו התקפת בונוס, היא משתמשת בכוח הווירטואלי שנצבר *עד כה*
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

                    handleOnAttackEffects(attacker, attackingHero);
                }

                // חשוב: אנחנו מוחקים *רק* את התקפת הבונוס הנוכחית דרך האיטרטור
                // כדי שהיא לא תקרה שוב בתור הבא, שאר האפקטים (כמו תוספת כוח) נשארים!
                iterator.remove();
            }
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
        // שלב 1: ציר הזמן של ההשפעות (לפני הקרב הרגיל)
        // ==========================================

        executePreCombatTimeline(firstCard, secondCard, secondHero, firstHero);
        executePreCombatTimeline(secondCard, firstCard, firstHero, secondHero);

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
            int pAtk = firstCard.getAtk(); // כאן זה ישתמש ב-getAtk המלא שכולל את כל הבאפים
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