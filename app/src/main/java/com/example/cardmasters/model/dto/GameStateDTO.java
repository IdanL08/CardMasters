package com.example.cardmasters.model.dto;

import java.util.Map;

public class GameStateDTO {

    private int turnNumber;

    // laneId -> LaneDTO (with player card & enemy card already resolved)
    private Map<String, LaneDTO> lanes;

    private boolean waitingForOpponent;

    public GameStateDTO() {}

    public GameStateDTO(int turnNumber, Map<String, LaneDTO> lanes, boolean waitingForOpponent) {
        this.turnNumber = turnNumber;
        this.lanes = lanes;
        this.waitingForOpponent = waitingForOpponent;
    }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public Map<String, LaneDTO> getLanes() { return lanes; }
    public void setLanes(Map<String, LaneDTO> lanes) { this.lanes = lanes; }

    public boolean isWaitingForOpponent() { return waitingForOpponent; }
    public void setWaitingForOpponent(boolean waitingForOpponent) {
        this.waitingForOpponent = waitingForOpponent;
    }
}