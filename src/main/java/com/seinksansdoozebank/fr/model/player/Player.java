package com.seinksansdoozebank.fr.model.player;

public class Player {
    int id;
    int nbGold;
    Hand hand;
    Citadel citadel;
    public Player(int nbGold) {
        this.nbGold = nbGold;
    }
}
