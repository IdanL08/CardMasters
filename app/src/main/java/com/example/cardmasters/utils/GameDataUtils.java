package com.example.cardmasters.utils;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.EffectDTO;
import com.example.cardmasters.model.dto.PlayedActionDTO;
import com.example.cardmasters.model.dto.PlayedTurnDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameDataUtils {

    private static final String TAG = "GameDataUtils";

    /**
     * Converts the raw Map from Firebase into a structured PlayedTurnDTO.
     */
    public static PlayedTurnDTO parseTurnData(Map<String, Object> data) {
        try {
            if (data == null) return null;

            PlayedTurnDTO turn = new PlayedTurnDTO();
            turn.setPlayerId((String) data.get("playerId"));

            Object turnNumObj = data.get("turnNumber");
            int turnNum = (turnNumObj instanceof Long) ? ((Long) turnNumObj).intValue() : (int) (turnNumObj != null ? turnNumObj : 0);
            turn.setTurnNumber(turnNum);

            List<Map<String, Object>> actionsRaw = (List<Map<String, Object>>) data.get("actions");
            List<PlayedActionDTO> parsedActions = new ArrayList<>();

            if (actionsRaw != null) {
                for (Map<String, Object> actionMap : actionsRaw) {
                    PlayedActionDTO action = new PlayedActionDTO();
                    action.setLaneId((String) actionMap.get("laneId"));

                    Map<String, Object> cardMap = (Map<String, Object>) actionMap.get("card");
                    if (cardMap != null) {
                        action.setCard(parseCardDTO(cardMap));
                    }
                    parsedActions.add(action);
                }
            }
            turn.setActions(parsedActions);
            return turn;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse turn data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a raw Card Map from Firebase into a CardDTO.
     */
    public static CardDTO parseCardDTO(Map<String, Object> cardMap) {
        if (cardMap == null) return null;

        CardDTO card = new CardDTO();
        card.setCardId((String) cardMap.get("cardId"));
        card.setType((String) cardMap.get("type"));

        Object hp = cardMap.get("baseHp");
        card.setHp(hp instanceof Long ? ((Long) hp).intValue() : (int) (hp != null ? hp : 0));

        Object atk = cardMap.get("baseAtk");
        card.setAtk(atk instanceof Long ? ((Long) atk).intValue() : (int) (atk != null ? atk : 0));

        List<Map<String, Object>> effectsRaw = (List<Map<String, Object>>) cardMap.get("appliedEffects");
        if (effectsRaw != null) {
            List<EffectDTO> parsedEffects = new ArrayList<>();
            for (Map<String, Object> eMap : effectsRaw) {
                EffectDTO e = new EffectDTO();
                e.setTarget((String) eMap.get("target"));
                e.setType((String) eMap.get("type"));
                Object val = eMap.get("value");
                e.setValue(val instanceof Long ? ((Long) val).intValue() : (int) (val != null ? val : 0));
                parsedEffects.add(e);
            }
            card.setAppliedEffects(parsedEffects);
        }
        return card;
    }

    /**
     * Pulls stats from the Local SQLite Database to build the DTO.
     * Use this when drawing a card from the deck.
     */
    public static CardDTO createDtoFromDb(Context context, String cardId) {
        CardDatabaseHelper dbHelper = new CardDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        CardDTO dto = new CardDTO();

        // Assuming your table 'cards' has columns: card_id, type, hp, atk
        Cursor cursor = db.query("cards_library", null, "id = ?", new String[]{cardId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            dto.setCardId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
            dto.setType(cursor.getString(cursor.getColumnIndexOrThrow("card_class")));
            dto.setHp(cursor.getInt(cursor.getColumnIndexOrThrow("hp")));
            dto.setAtk(cursor.getInt(cursor.getColumnIndexOrThrow("atk")));
            dto.setAppliedEffects(new ArrayList<>()); // Starts clean
            cursor.close();
        } else {
            Log.e(TAG, "Card ID " + cardId + " not found in SQLite!");
        }

        return dto;
    }
}