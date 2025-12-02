package com.example.cardmasters.model.dto;

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
}