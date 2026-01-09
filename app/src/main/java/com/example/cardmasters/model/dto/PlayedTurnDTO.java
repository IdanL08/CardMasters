package com.example.cardmasters.model.dto;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayedTurnDTO {

    private String playerId;
    private int turnNumber;
    private List<PlayedActionDTO> actions;

    public PlayedTurnDTO() {}

    public PlayedTurnDTO(String playerId, int turnNumber, List<PlayedActionDTO> actions) {
        this.playerId = playerId;
        this.turnNumber = turnNumber;
        this.actions = actions;
    }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId);
         map.put("turnNumber", turnNumber);
        map.put("submittedAt", FieldValue.serverTimestamp());

        List<Map<String, Object>> actionsList = new ArrayList<>();
        for (PlayedActionDTO action : actions) {
            actionsList.add(action.toMap());
        }
        map.put("actions", actionsList);

        return map;
    }

    public String getPlayerId() { return playerId; }

    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public List<PlayedActionDTO> getActions() { return actions; }
    public void setActions(List<PlayedActionDTO> actions) { this.actions = actions; }
}