package com.example.cardmasters.model.dto;

import java.util.HashMap;
import java.util.Map;

public class PlayedActionDTO {

    private String laneId;     // "0", "1", "2"
    private CardDTO card;      // Fighter OR Effect card

    public PlayedActionDTO() {}

    public PlayedActionDTO(String laneId, CardDTO card) {
        this.laneId = laneId;
        this.card = card;
    }



    public String getLaneId() { return laneId; }
    public void setLaneId(String laneId) { this.laneId = laneId; }

    public CardDTO getCard() { return card; }
    public void setCard(CardDTO card) { this.card = card; }
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("laneId", laneId);

        // If your card is stored as a DTO inside this action
        if (card != null) {
            // This creates the nested "card" object seen in your screenshot
            map.put("card", card.toMap());
        }

        return map;
    }
}