package com.example.cardmasters.model.dto;


public class LaneDTO {

    private CardDTO playerCard;
    private CardDTO enemyCard;

    public LaneDTO() {}

    public LaneDTO(CardDTO playerCard, CardDTO enemyCard) {
        this.playerCard = playerCard;
        this.enemyCard = enemyCard;
    }

    public CardDTO getPlayerCard() { return playerCard; }
    public void setPlayerCard(CardDTO playerCard) { this.playerCard = playerCard; }

    public CardDTO getEnemyCard() { return enemyCard; }
    public void setEnemyCard(CardDTO enemyCard) { this.enemyCard = enemyCard; }
}