package com.example.cardmasters.model.dto;

import java.util.List;


public class PlayerTurnDTO {

    private String playerId;
    private int turnNumber;
    private List<PlayedActionDTO> actions;

    public PlayerTurnDTO() {}

    public PlayerTurnDTO(String playerId, int turnNumber, List<PlayedActionDTO> actions) {
        this.playerId = playerId;
        this.turnNumber = turnNumber;
        this.actions = actions;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public List<PlayedActionDTO> getActions() { return actions; }
    public void setActions(List<PlayedActionDTO> actions) { this.actions = actions; }
}