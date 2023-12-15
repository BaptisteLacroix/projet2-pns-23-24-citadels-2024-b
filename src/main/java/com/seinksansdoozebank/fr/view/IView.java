package com.seinksansdoozebank.fr.view;

import com.seinksansdoozebank.fr.model.cards.Card;
import com.seinksansdoozebank.fr.model.player.Player;

import java.util.Optional;

public interface IView {
    void displayPlayerInfo(Player player);

    void displayRound(int roundNumber);

    void displayPlayerPlaysCard(Player player, Optional<Card> optionalCard);

    void displayWinner(Player winner);

    void displayPlayerStartPlaying(Player player);

    void displayPlayerPickCard(Player player);

    void displayPlayerPicksGold(Player player);

    void displayPlayerChooseCharacter(Player player);
}
