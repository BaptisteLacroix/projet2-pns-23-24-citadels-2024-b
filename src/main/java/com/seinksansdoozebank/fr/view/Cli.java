package com.seinksansdoozebank.fr.view;

import com.seinksansdoozebank.fr.model.cards.Card;
import com.seinksansdoozebank.fr.model.cards.District;
import com.seinksansdoozebank.fr.model.player.Player;
import com.seinksansdoozebank.fr.view.logger.CustomLogger;

import java.util.*;
import java.util.logging.Level;


public class Cli implements IView {

    private final HashMap<Player, String> playerColors = new HashMap<>();
    private final Random random = new Random();

    private final List<String> availableColors = new ArrayList<>(List.of(
            "\u001B[32m", // Green
            "\u001B[33m", // Yellow
            "\u001B[34m", // Blue
            "\u001B[35m", // Purple
            "\u001B[36m", // Cyan
            "\u001B[92m", // Light Green
            "\u001B[93m", // Light Yellow
            "\u001B[94m", // Light Blue
            "\u001B[95m", // Light Purple
            "\u001B[96m" // Light Cyan
    ));

    private void setPlayerColors(Player player) {
        if (playerColors.containsKey(player)) {
            return;
        }
        int index = random.nextInt(availableColors.size());
        String randomColor = availableColors.get(index);
        // remove the color from the list so that it can't be used again
        availableColors.remove(index);
        playerColors.put(player, randomColor);
    }

    private String applyColor(Player player, String message) {
        return playerColors.getOrDefault(player, "") + message + "\u001B[0m";
    }

    public void displayPlayerPlaysCard(Player player, Optional<Card> optionalCard) {
        this.setPlayerColors(player);
        if (optionalCard.isEmpty()) {
            CustomLogger.log(Level.INFO, applyColor(player, "{0} ne pose pas de quartier. "), player);
        } else {
            District builtDistrict = optionalCard.get().getDistrict();
            CustomLogger.log(Level.INFO, applyColor(player, "{0} pose un/e {1} qui lui coute {2}, il lui reste {3}  pièces d'or."), new Object[]{player, builtDistrict.getName(), builtDistrict.getCost(), player.getNbGold()});
        }
    }

    public void displayWinner(Player winner) {
        this.setPlayerColors(winner);
        CustomLogger.log(Level.INFO, applyColor(winner, "{0} gagne avec un score de {1} ."), new Object[]{winner, winner.getScore()});
    }

    @Override
    public void displayPlayerStartPlaying(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "\n{0} commence à jouer."), player);
    }

    @Override
    public void displayPlayerPickCard(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "{0} pioche un quartier."), player);
    }

    @Override
    public void displayPlayerPicksGold(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "{0} pioche 2 pièces d'or."), player);
    }

    @Override
    public void displayPlayerChooseCharacter(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "{0} choisit un personnage."), player);
    }

    @Override
    public void displayPlayerRevealCharacter(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "{0} se révèle être {1} ."), new Object[]{player, player.getCharacter()});
    }

    @Override
    public void displayPlayerDestroyDistrict(Player attacker, Player defender, District district) {
        this.setPlayerColors(attacker);
        this.setPlayerColors(defender);
        CustomLogger.log(Level.INFO, applyColor(attacker, "{0} détruit le quartier {1} de {2} en payant {3} pièces d'or."), new Object[]{attacker, district.getName(), defender, district.getCost() + 1});
    }

    private void displayPlayerHand(Player player) {
        this.setPlayerColors(player);
        List<Card> hand = player.getHand();
        StringBuilder sb = new StringBuilder();
        if (!hand.isEmpty()) {
            if (hand.size() == 1) {
                sb.append("\t- la carte suivante dans sa main : \n");
            } else {
                sb.append("\t- les cartes suivantes dans sa main : \n");
            }
            for (int i = 0; i < hand.size(); i++) {
                sb.append("\t\t- ").append(hand.get(i));
                if (i != hand.size() - 1) {
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\t- pas de carte dans sa main.");
        }
        CustomLogger.log(Level.INFO, applyColor(player, sb.toString()));
    }

    private void displayPlayerCitadel(Player player) {
        this.setPlayerColors(player);
        List<Card> citadel = player.getCitadel();
        StringBuilder sb = new StringBuilder();
        if (!citadel.isEmpty()) {
            if (citadel.size() == 1) {
                sb.append("\t- le quartier suivant dans sa citadelle : \n");
            } else {
                sb.append("\t- les quartiers suivants dans sa citadelle : \n");
            }
            for (int i = 0; i < citadel.size(); i++) {
                sb.append("\t\t- ").append(citadel.get(i));
                if (i != citadel.size() - 1) {
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\t- pas de quartier dans sa citadelle.");
        }
        CustomLogger.log(Level.INFO, applyColor(player, sb.toString()));
    }

    @Override
    public void displayPlayerInfo(Player player) {
        this.setPlayerColors(player);
        CustomLogger.log(Level.INFO, applyColor(player, "{0} possède : \n\t- {1} pièces d'or."), new Object[]{player, player.getNbGold()});
        this.displayPlayerHand(player);
        this.displayPlayerCitadel(player);
    }

    public void displayRound(int roundNumber) {
        CustomLogger.log(Level.INFO, "\u001B[31m\n\n########## Début du round {0} ##########\u001B[0m", roundNumber);
    }
}
