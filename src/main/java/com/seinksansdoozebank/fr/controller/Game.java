package com.seinksansdoozebank.fr.controller;

import com.seinksansdoozebank.fr.model.bank.Bank;
import com.seinksansdoozebank.fr.model.cards.Card;
import com.seinksansdoozebank.fr.model.cards.Deck;
import com.seinksansdoozebank.fr.model.cards.District;
import com.seinksansdoozebank.fr.model.character.abstracts.Character;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Bishop;
import com.seinksansdoozebank.fr.model.character.commoncharacters.King;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Merchant;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Warlord;
import com.seinksansdoozebank.fr.model.character.roles.Role;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Architect;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Assassin;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Magician;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Thief;
import com.seinksansdoozebank.fr.model.player.Opponent;
import com.seinksansdoozebank.fr.model.player.Player;
import com.seinksansdoozebank.fr.view.IView;
import com.seinksansdoozebank.fr.view.logger.CustomLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The Game class represents a game of Citadels
 */
public class Game {
    /**
     * The maximum number of players
     */
    protected static final int NB_PLAYER_MAX = 6;
    /**
     * The minimum number of players
     */
    protected static final int NB_PLAYER_MIN = 3;
    private static final int NB_CARD_BY_PLAYER = 4;
    public static final int NORMAL_NB_DISTRICT_TO_WIN = 8;
    private int nbOfDistrictsInCitadel;
    private boolean findFirstPlayerWithAllDistricts = false;
    final Deck deck;
    final Bank bank;
    /**
     * The list of players in the initial order
     */
    protected List<Player> playersInInitialOrder;
    /**
     * The list of players
     */
    protected List<Player> players;
    Player crownedPlayer;
    private List<Character> availableCharacters;
    private List<Character> charactersInTheRound;
    private List<Character> unusedCharacters;
    private final IView view;
    private int nbCurrentRound;
    private boolean finished;
    private boolean isVariante;

    /**
     * Constructor of the Game class
     *
     * @param view       the view
     * @param deck       the deck of cards
     * @param bank       the bank
     * @param playerList the list of players
     */
    protected Game(IView view, Deck deck, Bank bank, List<Player> playerList) {
        if (playerList.size() > NB_PLAYER_MAX || playerList.size() < NB_PLAYER_MIN) {
            throw new IllegalArgumentException("The number of players must be between " + NB_PLAYER_MIN + " and " + NB_PLAYER_MAX);
        }
        this.view = view;
        this.deck = deck;
        this.playersInInitialOrder = playerList;
        this.players = new ArrayList<>(playerList);
        this.availableCharacters = new ArrayList<>();
        this.crownedPlayer = null;
        this.finished = false;
        this.bank = bank;
        if (playerList.size() == 3) {
            nbOfDistrictsInCitadel = 10;
        } else {
            nbOfDistrictsInCitadel = NORMAL_NB_DISTRICT_TO_WIN;
        }
    }

    public void setVariante(boolean variante) {
        isVariante = variante;
    }


    public boolean isVariante() {
        return isVariante;
    }

    /**
     * Run the game
     */
    public void run() {
        this.init();
        this.nbCurrentRound = 1;
        while (!finished && !this.isStuck()) {
            view.displayRound(nbCurrentRound);
            createCharacters();
            this.playARound();
        }
        if (finished) {
            view.displayGameFinished();
        } else {
            view.displayGameStuck();
        }
        updatePlayersBonus();
        view.displayWinner(this.getWinner());
    }

    /**
     * Check if the game is stuck
     * @return true if the game is stuck
     */
    protected boolean isStuck() {
        boolean aPlayerCanPlay = players.stream()
                .anyMatch(player -> player.getHand().stream()
                        .anyMatch(player::canPlayCard));
        return (deck.getDeck().isEmpty() && this.bank.getNbOfAvailableCoins() <= 0 && !aPlayerCanPlay) || this.nbCurrentRound > 1000;
    }

    /**
     * Play a round
     */
    protected void playARound() {
        orderPlayerBeforeChoosingCharacter();
        playersChooseCharacters();
        orderPlayerBeforePlaying();
        for (Player player : players) {
            if (!player.getCharacter().isDead()) {
                player.setAvailableCharacters(charactersInTheRound);
                player.setCharactersNotInRound(unusedCharacters);
                this.updateCrownedPlayer(player);
                checkPlayerStolen(player);
                player.play();
            }else if (this.isVariante()){
                this.updateCrownedPlayer(player);
            }
            //We set the attribute to true if player is the first who has eight districts
            isTheFirstOneToHaveAllDistricts(player);
        }
        retrieveCharacters();
        finished = players.stream().anyMatch(player -> player.getCitadel().size() >= this.getNumberOfDistrictsNeeded());
        this.nbCurrentRound++;
    }

    /**
     * Update the crowned player
     * @param player the player to update
     */
    void updateCrownedPlayer(Player player) {
        crownedPlayer = player.getCharacter().getRole().equals(Role.KING) ? player : crownedPlayer;
    }

    /**
     * Get the number of the current round
     * @return the number of the current round
     */
    protected int getNbCurrentRound() {
        return nbCurrentRound;
    }

    /**
     * Order the players before playing
     */
    void orderPlayerBeforePlaying() {
        players.sort(Comparator.comparing(player -> player.getCharacter().getRole()));
    }

    /**
     * Retrieve the characters from the players
     */
    void retrieveCharacters() {
        for (Player player : players) {
            availableCharacters.add(player.retrieveCharacter());
        }
    }

    /**
     * Order the players before choosing a character if a
     * player revealed himself being the king during the last round
     */
    void orderPlayerBeforeChoosingCharacter() {
        players = new ArrayList<>(playersInInitialOrder);
        if (crownedPlayer != null) {
            List<Player> orderedPlayers = new ArrayList<>();
            //récupération de l'index du roi dans la liste des joueurs
            int indexOfTheKingPlayer = players.indexOf(crownedPlayer);
            for (int i = indexOfTheKingPlayer; i < players.size(); i++) {
                orderedPlayers.add((i - indexOfTheKingPlayer) % players.size(), players.get(i));
            }
            for (int i = 0; i < indexOfTheKingPlayer; i++) {
                orderedPlayers.add((i + players.size() - indexOfTheKingPlayer) % players.size(), players.get(i));
            }
            players = orderedPlayers;
        }
    }


    /**
     * Ask the player to choose their characters
     */
    protected void playersChooseCharacters() {
        List<Opponent> opponentsWhichHasChosenCharacter = new ArrayList<>();
        for (Player player : players) {
            player.setPositionInDrawToPickACharacter(players.indexOf(player));
            player.setOpponentsWhichHasChosenCharacterBefore(opponentsWhichHasChosenCharacter);
            availableCharacters.remove(player.chooseCharacter(availableCharacters));
            opponentsWhichHasChosenCharacter.add(player);
        }
    }


    /**
     * Initialize the game
     */
    protected void init() {
        CustomLogger.resetAvailableColors();
        dealCards();
    }

    /**
     * Create the list of characters ordered
     */
    protected void createCharacters() {
        int nbPlayers = this.players.size();
        availableCharacters = new ArrayList<>();
        List<Character> notMandatoryCharacters = new ArrayList<>(List.of(
                new Assassin(),
                new Thief(),
                new Magician(),
                new Bishop(),
                new Merchant(),
                new Architect(),
                new Warlord()));
        if (nbPlayers + 1 > notMandatoryCharacters.size()) {
            throw new UnsupportedOperationException("The number of players is too high for the number of characters implemented");
        }
        if (nbPlayers == 3) {
            notMandatoryCharacters.remove(0);
        }
        Collections.shuffle(notMandatoryCharacters);
        // the king must always be available
        availableCharacters.add(new King());
        //adding as much characters as there are players because the king is already added and
        // the rules say that the number of characters must be equal to the number of players +1
        for (int i = 0; i < nbPlayers + 1; i++) {
            availableCharacters.add(notMandatoryCharacters.get(i));
        }
        charactersInTheRound = new ArrayList<>(availableCharacters);
        //remove the characters that are available from the list of not mandatory characters
        notMandatoryCharacters.removeAll(availableCharacters);
        //display the characters that are not in availableCharacters
        this.unusedCharacters = notMandatoryCharacters;
        for (Character unusedCharacter : notMandatoryCharacters) {
            view.displayUnusedCharacterInRound(unusedCharacter);
        }
    }

    /**
     * Deal the cards to the players
     */
    private void dealCards() {
        for (int i = 0; i < NB_CARD_BY_PLAYER; i++) {
            for (Player player : players) {
                Optional<Card> cardPick = deck.pick();
                cardPick.ifPresent(card -> player.getHand().add(card));
            }
        }
    }

    /**
     * Get the winner of the game
     *
     * @return The player who win the game
     */
    public Player getWinner() {
        this.orderPlayersByPoints();
        return this.players.get(0);
    }

    /**
     * Order the players by points, considering tiebreakers.
     */
    protected void orderPlayersByPoints() {
        this.getPlayers().sort((player1, player2) -> {
            // Compare by total points
            int scoreComparaison = Integer.compare(player2.getScore(), player1.getScore());

            if (scoreComparaison == 0) {
                // If points are tied, compare by the number of districts in the citadel
                int citadelComparaison = Integer.compare(player2.getCitadel().size(), player1.getCitadel().size());

                if (citadelComparaison == 0) {
                    // If districts are tied, compare by the total points of all districts
                    return Integer.compare(
                            player2.getCitadel().stream().mapToInt(
                                    card -> card.getDistrict().getCost()
                            ).sum(),
                            player1.getCitadel().stream().mapToInt(
                                    card -> card.getDistrict().getCost()
                            ).sum()
                    );
                }
                return citadelComparaison;
            }
            return scoreComparaison;
        });
    }

    /**
     * Set the list of players (For Test ONLY)
     *
     * @param players the list of players
     */
    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    /**
     * Get the characters that are still available.
     *
     * @return the list of characters available
     */
    public List<Character> getAvailableCharacters() {
        return availableCharacters;
    }

    /**
     * Method which sets the attribute isTheFirstOneToHaveAllDistricts at true if it's the case
     *
     * @param player who added a card to his deck
     */
    public void isTheFirstOneToHaveAllDistricts(Player player) {
        if (player.getCitadel().size() == nbOfDistrictsInCitadel && !findFirstPlayerWithAllDistricts) {
            //we mark the bot as true if it is first to have 8 districts
            player.setIsFirstToHaveAllDistricts();
            findFirstPlayerWithAllDistricts = true;
        }
    }

    /**
     * This method adds bonus to the attribute bonus of each player of the game if they have got one
     */
    public void updatePlayersBonus() {
        for (Player player : players) {
            // Check if the player contain the district COURTYARD_OF_MIRACLE
            if (player.hasCourtyardOfMiracleAndItsNotTheLastCard()) {
                player.chooseColorCourtyardOfMiracle();
            }
            if (player.hasFiveDifferentDistrictTypes()) {
                player.addBonus(3);
                view.displayPlayerGetBonus(player, 3, "5 quartiers de types différents");
            }
            if (player.getCitadel().size() == this.getNumberOfDistrictsNeeded()) {
                if (player.getIsFirstToHaveAllDistricts()) {
                    player.addBonus(2);
                    view.displayPlayerGetBonus(player, 2, "premier joueur a atteindre " + this.getNumberOfDistrictsNeeded() + " quartiers");
                }
                player.addBonus(2);
                view.displayPlayerGetBonus(player, 2, this.getNumberOfDistrictsNeeded() + " quartiers");
            }
            checkUniversityOrPortForDragonsInCitadel(player);
            view.displayPlayerScore(player);
        }
    }


    /**
     * this method check if the district university of port for dragons in the citadel of the player
     * if it's the case we add 2 bonus for each
     *
     * @param player the player to check
     */
    public void checkUniversityOrPortForDragonsInCitadel(Player player) {
        for (Card card : player.getCitadel()) {
            if (card.getDistrict() == District.UNIVERSITY || card.getDistrict() == District.PORT_FOR_DRAGONS) {
                view.displayPlayerGetBonus(player, 2, "présence du district " + card.getDistrict().getName());
                player.addBonus(2);
            }
        }
    }


    /**
     * @param role the role of the player we want to get
     * @return an optional of Player with the given role
     */
    public Optional<Player> getPlayerByRole(Role role) {
        for (Player player : players) {
            if (player.getCharacter().getRole() == role) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }


    /**
     * we apply this if the player has savedThief==true
     *
     * @param player player to check
     */
    public void checkPlayerStolen(Player player) {
        if (player.getCharacter().getSavedThief() != null) {
            view.displayStolenCharacter(player.getCharacter());
            player.getCharacter().isStolen();
            Optional<Player> playerByRole = getPlayerByRole(Role.THIEF);
            playerByRole.ifPresent(view::displayActualNumberOfGold);
        }
    }

    /**
     * Get the list of players
     * @return the list of players
     */
    public List<Player> getPlayers() {
        return players;
    }

    public void setNbOfDistrictsInCitadel(int nbOfDistrictsInCitadel) {
        this.nbOfDistrictsInCitadel = nbOfDistrictsInCitadel;
    }

    public int getNumberOfDistrictsNeeded() {
        return nbOfDistrictsInCitadel;
    }
}
