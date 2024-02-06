package com.seinksansdoozebank.fr.model.player;

import com.seinksansdoozebank.fr.model.bank.Bank;
import com.seinksansdoozebank.fr.model.cards.Card;
import com.seinksansdoozebank.fr.model.cards.Deck;
import com.seinksansdoozebank.fr.model.cards.District;
import com.seinksansdoozebank.fr.model.cards.DistrictType;
import com.seinksansdoozebank.fr.model.character.abstracts.Character;
import com.seinksansdoozebank.fr.model.character.abstracts.CommonCharacter;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Condottiere;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Assassin;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Magician;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Thief;
import com.seinksansdoozebank.fr.view.IView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public abstract class Player implements Opponent {
    private static int counter = 1;
    protected final int id;
    private int nbGold;
    private int bonus;
    private boolean isFirstToHaveEightDistricts;
    protected Deck deck;
    protected final List<Card> hand;
    private final List<Card> citadel;
    protected final IView view;
    protected Random random = new Random();
    protected Character character;

    /**
     * List of all the players in the game
     */
    private List<Opponent> opponents;

    private List<Character> availableCharacters;
    private boolean lastCardPlacedCourtyardOfMiracle = false;
    private boolean characterIsRevealed = false;
    private DistrictType colorCourtyardOfMiracleType;
    private boolean hasPlayed;

    protected Player(int nbGold, Deck deck, IView view) {
        this.id = counter++;
        this.nbGold = nbGold;
        this.deck = deck;
        this.hand = new ArrayList<>();
        this.citadel = new ArrayList<>();
        this.opponents = new ArrayList<>();
        this.view = view;
        this.bonus = 0;
        this.isFirstToHaveEightDistricts = false;
        this.hasPlayed = false;
    }

    /**
     * Represents the player's turn
     * MUST CALL view.displayPlayerPlaysDistrict() at the end of the turn with the district built by the player
     */
    public void play() {
        if (this.getCharacter().isDead()) {
            throw new IllegalStateException("The player is dead, he can't play.");
        }
        view.displayPlayerStartPlaying(this);
        this.reveal();
        view.displayPlayerInfo(this);
        this.usePrestigesEffect();
        this.setHasPlayed(false);
        this.playARound();
        view.displayPlayerInfo(this);
    }

    public void usePrestigesEffect() {
        // for every prestiges that the player has, and has effect, we use it
        this.citadel.stream().filter(
                        card -> card.getDistrict().getDistrictType().equals(DistrictType.PRESTIGE) &&
                                card.getDistrict().getActiveEffect() != null)
                .forEach(card ->
                        card.getDistrict().useActiveEffect(this, this.view)
                );
    }

    public abstract void playARound();

    /**
     * Represents the player's choice between drawing 2 gold coins or a district
     */
    protected abstract void pickSomething();

    /**
     * if the bot has got in its citadel 5 different types of districts it returns true else return false
     *
     * @return a boolean
     */
    public boolean hasFiveDifferentDistrictTypes() {
        List<DistrictType> listDifferentDistrictType = new ArrayList<>();
        for (Card card : this.getCitadel()) {
            if (!listDifferentDistrictType.contains(card.getDistrict().getDistrictType())) {
                listDifferentDistrictType.add(card.getDistrict().getDistrictType());
            }
        }
        // if there is 4 different district types and there is a courtyard of miracle in the citadel, we add the last district type
        if (listDifferentDistrictType.size() == 4 && this.hasCourtyardOfMiracleAndItsNotTheLastCard()) {
            listDifferentDistrictType.add(this.getColorCourtyardOfMiracleType());
        }
        return (listDifferentDistrictType.size() == 5);
    }

    public boolean hasCourtyardOfMiracleAndItsNotTheLastCard() {
        return this.getCitadel().stream().anyMatch(card -> card.getDistrict().equals(District.COURTYARD_OF_MIRACLE))
                && !this.isLastCardPlacedCourtyardOfMiracle();
    }


    /**
     * @return list of districtType missing in the citadel of the player
     */
    public List<DistrictType> findDistrictTypesMissingInCitadel() {
        List<DistrictType> listOfDistrictTypeMissing = new ArrayList<>();
        for (DistrictType districtType : DistrictType.values()) {
            if (this.getCitadel().stream().anyMatch(card -> card.getDistrict().getDistrictType() == districtType)) {
                listOfDistrictTypeMissing.add(districtType);
            }
        }
        return listOfDistrictTypeMissing;
    }

    public void setHasPlayed(boolean hasPlayed) {
        this.hasPlayed = hasPlayed;
    }

    public boolean hasPlayed() {
        return hasPlayed;
    }

    /**
     * Represents the player's choice to draw x districts keep one and discard the other one
     * MUST CALL this.hand.add() AND this.deck.discard() AT EACH CALL
     */
    public void pickCardsKeepSomeAndDiscardOthers() {
        List<Card> pickedCards = new ArrayList<>();
        int numberOfCardsToPick = numberOfCardsToPick();
        for (int i = 0; i < numberOfCardsToPick; i++) {
            pickCardFromDeck(pickedCards);
        }
        if (pickedCards.isEmpty()) return;
        if ((!this.hasPlayed) && isLibraryPresent()) {
            this.view.displayPlayerKeepBothCardsBecauseOfLibrary(this);
            this.hand.addAll(pickedCards);
        } else {
            this.view.displayPlayerPickCards(this, 1);
            Card chosenCard = keepOneDiscardOthers(pickedCards);
            this.hand.add(chosenCard);
            pickedCards.stream().filter(card -> card.hashCode() != chosenCard.hashCode()).forEach(card -> this.deck.discard(card));
        }
    }

    void pickCardFromDeck(List<Card> pickedCards) {
        Optional<Card> cardPick = this.deck.pick();
        cardPick.ifPresent(pickedCards::add);
    }

    /**
     * On regarde si dans la citadelle du joueur le player à l'observatoire, on retourne le nombre de cartes à piocher en fonction
     *
     * @return nombre de cartes à piocher
     */
    protected int numberOfCardsToPick() {
        Optional<Card> observatory = getCitadel().stream().filter(card -> card.getDistrict() == District.OBSERVATORY).findFirst();
        if (observatory.isPresent()) {
            this.view.displayPlayerHasGotObservatory(this);
            return 3;
        }
        return 2;
    }

    protected abstract Card keepOneDiscardOthers(List<Card> pickedCards);

    /**
     * Allow the player to pick a card from the deck (usefull when it needs to switch its hand with the deck)
     */
    public final void pickACard() {
        pickCardFromDeck(this.getHand());
    }

    public final void discardACard(Card card) {
        this.getHand().remove(card);
        this.deck.discard(card);
    }

    /**
     * Represents the phase where the player build a district chosen by chooseDistrict()
     *
     * @return the district built by the player
     */
    protected final Optional<Card> playACard() {
        Optional<Card> optChosenCard = chooseCard();
        if (optChosenCard.isEmpty() || !canPlayCard(optChosenCard.get())) {
            return Optional.empty();
        }
        Card chosenCard = optChosenCard.get();
        this.getHand().remove(chosenCard);
        // if the chose card is CourtyardOfMiracle, we set the attribute lastCardPlacedCourtyardOfMiracle to true
        this.lastCardPlacedCourtyardOfMiracle = chosenCard.getDistrict().equals(District.COURTYARD_OF_MIRACLE);
        this.citadel.add(chosenCard);
        this.returnGoldToBank(chosenCard.getDistrict().getCost());
        return optChosenCard;
    }

    public void buyXCardsAndAddThemToCitadel(int numberOfCards) {
        setHasPlayed(true);
        if (numberOfCards <= 0) {
            throw new IllegalArgumentException("Number of cards to play must be positive");
        } else if (numberOfCards > this.getNbDistrictsCanBeBuild()) {
            throw new IllegalArgumentException("Number of cards to play must be less than the number of districts the player can build");
        }
        for (int i = 0; i < numberOfCards; i++) {
            Optional<Card> card = playACard();
            card.ifPresent(value -> this.view.displayPlayerPlaysCard(this, value));
        }
    }

    /**
     * make the player play the Card given in argument by removing it from its hand, adding it to its citadel and decreasing golds
     */
    public void buyACardAndAddItToCitadel(Card card) {
        if (!canPlayCard(card)) {
            return;
        }
        this.hand.remove(card);
        this.citadel.add(card);
        this.returnGoldToBank(card.getDistrict().getCost());
        this.view.displayPlayerPlaysCard(this, card);
    }


    /**
     * Collect gold with the effect of the character if it is a common character
     */
    protected void useCommonCharacterEffect() {
        if (this.getCharacter() instanceof CommonCharacter commonCharacter) {
            int nbGoldSave = this.getNbGold();
            commonCharacter.goldCollectedFromDistrictType();
            if (this.getNbGold() - nbGoldSave > 0)
                this.view.displayGoldCollectedFromDistrictType(this, this.getNbGold() - nbGoldSave, commonCharacter.getTarget());
        }
    }

    /**
     * Effect of architect character (pick 2 cards)
     */
    protected void useEffectArchitectPickCards() {
        int i;
        for (i = 0; i < 2; i++) {
            Optional<Card> cardPick = this.deck.pick();
            if (cardPick.isEmpty()) break;
            cardPick.ifPresent(this.hand::add);
        }
        if (i == 0) return;
        view.displayPlayerPickCards(this, i);
    }

    abstract void useEffectMagician(Magician magician);

    abstract void useEffectAssassin(Assassin assassin);

    abstract Character chooseAssassinTarget();

    abstract void useEffectCondottiere(Condottiere condottiere);

    abstract void useEffectThief(Thief thief);

    protected boolean hasACardToPlay() {
        return this.hand.stream().anyMatch(this::canPlayCard);
    }

    /**
     * Choose a district to build from the hand
     *
     * @return the district to build
     */
    protected abstract Optional<Card> chooseCard();

    /**
     * Verify if the player can build the district passed in parameter (he can build it if he has enough gold and if he doesn't already have it in his citadel)
     *
     * @param card the district to build
     * @return true if the player can build the district passed in parameter, false otherwise
     */
    public final boolean canPlayCard(Card card) {
        return card.getDistrict().getCost() <= this.nbGold && !this.getCitadel().contains(card) && this.getCitadel().size() < 8;
    }

    public void decreaseGold(int gold) {
        this.nbGold -= gold;
    }

    public void increaseGold(int gold) {
        this.nbGold += gold;
    }

    /**
     * Return gold to the bank
     *
     * @param gold the number of gold coins to return
     */
    public void returnGoldToBank(int gold) {
        this.nbGold -= gold;
        Bank.getInstance().retrieveCoin(gold);
    }

    /**
     * Represents the player's choice to draw 2 gold coins from the bank
     */
    public final void pickGold() {
        int nbPickedGold = Bank.getInstance().pickXCoin();
        view.displayPlayerPicksGold(this, nbPickedGold);
        this.nbGold += nbPickedGold;
    }

    /**
     * Represents the player's choice to draw x gold coins
     *
     * @param nbOfGold the number of gold coins to pick
     */
    public final void pickGold(int nbOfGold) {
        int nbPickedGold = Bank.getInstance().pickXCoin(nbOfGold);
        if (nbPickedGold > 0) {
            view.displayPlayerPicksGold(this, nbPickedGold);
            this.nbGold += nbPickedGold;
        }
    }


    public List<Card> getHand() {
        return this.hand;
    }

    public List<Card> getCitadel() {
        return Collections.unmodifiableList(this.citadel);
    }

    void setCitadel(List<Card> citadel) {
        this.citadel.clear();
        this.citadel.addAll(citadel);
    }

    public int getNbGold() {
        return this.nbGold;
    }

    public int getId() {
        return this.id;
    }

    public static void resetIdCounter() {
        counter = 1;
    }

    /**
     * We add bonus with the final state of the game to a specific player
     *
     * @param bonus point to add to a player
     */
    public void addBonus(int bonus) {
        this.bonus += bonus;
    }

    /**
     * getter
     *
     * @return bonus point of a player
     */
    public int getBonus() {
        return this.bonus;
    }

    /**
     * getter
     *
     * @return a boolean which specify if the player is the first to have 8Districts in his citadel
     */
    public boolean getIsFirstToHaveEightDistricts() {
        return this.isFirstToHaveEightDistricts;
    }

    /**
     * setter, this method set the isFirstToHaveEightDistricts attribute to true
     */
    public void setIsFirstToHaveEightDistricts() {
        this.isFirstToHaveEightDistricts = true;
    }

    public final int getScore() {
        //calcule de la somme du cout des quartiers de la citadelle et rajoute les bonus s'il y en a
        return (getCitadel().stream().mapToInt(card -> card.getDistrict().getCost()).sum()) + getBonus();
    }

    /**
     * Choose a character from the list of characters passed in parameter by calling the method choseCharacterImpl
     *
     * @param characters the list of characters to choose from
     * @return the chosen character
     */
    public Character chooseCharacter(List<Character> characters) {
        this.character = this.chooseCharacterImpl(characters);
        this.character.setPlayer(this);
        this.view.displayPlayerChooseCharacter(this);
        return this.character;
    }

    /**
     * Chose a character from the list of characters passed in parameter
     *
     * @param characters the list of characters to choose from
     * @return the chosen character
     */
    protected abstract Character chooseCharacterImpl(List<Character> characters);

    public Character getCharacter() {
        return this.character;
    }

    public int getNbDistrictsCanBeBuild() {
        return this.character.getRole().getNbDistrictsCanBeBuild();
    }

    public Character retrieveCharacter() {
        if (this.character == null) {
            throw new IllegalStateException("No character to retrieve");
        }
        this.hide();
        Character characterToRetrieve = this.character;
        this.character = null;
        characterToRetrieve.resurrect();
        characterToRetrieve.setPlayer(null);
        return characterToRetrieve;
    }

    @Override
    public Optional<Card> destroyDistrict(Player attacker, District district) {
        // if the district is in the citadel, we remove it and return the card removed
        Optional<Card> card = this.getCitadel().stream().filter(c -> c.getDistrict().equals(district)).findFirst();
        if (card.isPresent()) {
            this.citadel.remove(card.get());
            this.view.displayPlayerUseCondottiereDistrict(attacker, this, district);
            return card;
        } else {
            throw new IllegalArgumentException("The player doesn't have the district to destroy");
        }
    }

    public abstract void useCemeteryEffect(Card card);

    public List<Opponent> getOpponents() {
        return Collections.unmodifiableList(this.opponents);
    }

    public void setOpponents(List<Opponent> opponents) {
        this.opponents = opponents;
    }

    @Override
    public void switchHandWith(Player magician) {
        List<Card> handToSwitch = new ArrayList<>(this.getHand());
        this.getHand().clear();
        this.getHand().addAll(magician.getHand());
        magician.getHand().clear();
        magician.getHand().addAll(handToSwitch);
    }

    public abstract void chooseColorCourtyardOfMiracle();

    public boolean isLastCardPlacedCourtyardOfMiracle() {
        return this.lastCardPlacedCourtyardOfMiracle;
    }

    public void setLastCardPlacedCourtyardOfMiracle(boolean lastCardPlacedCourtyardOfMiracle) {
        this.lastCardPlacedCourtyardOfMiracle = lastCardPlacedCourtyardOfMiracle;
    }

    public DistrictType getColorCourtyardOfMiracleType() {
        return this.colorCourtyardOfMiracleType;
    }

    public void setColorCourtyardOfMiracleType(DistrictType colorCourtyardOfMiracleType) {
        this.colorCourtyardOfMiracleType = colorCourtyardOfMiracleType;
    }

    public abstract boolean wantToUseManufactureEffect();

    public boolean isCharacterIsRevealed() {
        return this.characterIsRevealed;
    }

    public void reveal() {
        if (this.characterIsRevealed) {
            throw new IllegalStateException("The player is already revealed");
        }
        view.displayPlayerRevealCharacter(this);
        this.characterIsRevealed = true;
    }

    public void hide() {
        if (!this.characterIsRevealed && !this.character.isDead()) {
            throw new IllegalStateException("The player is already hidden");
        }
        this.characterIsRevealed = false;
    }

    @Override
    public Character getOpponentCharacter() {
        if (this.isCharacterIsRevealed()) {
            return this.getCharacter();
        } else {
            return null;
        }
    }

    @Override
    public int nbDistrictsInCitadel() {
        return this.getCitadel().size();
    }

    public List<Character> getAvailableCharacters() {
        return availableCharacters;
    }

    public void setAvailableCharacters(List<Character> availableCharacters) {
        this.availableCharacters = availableCharacters;
    }

    /**
     * Discard a card from the hand of the player (for laboratory effect)
     */
    public boolean discardFromHand(Card card) {
        if (this.hand.isEmpty()) {
            return false;
        }
        this.hand.remove(card);
        this.deck.discard(card);
        this.view.displayPlayerDiscardCard(this, card);
        return true;
    }

    public abstract Card chooseCardToDiscardForLaboratoryEffect();

    public boolean isLibraryPresent() {
        return this.getCitadel().stream().anyMatch(card -> card.getDistrict().equals(District.LIBRARY));
    }


    @Override
    public int getHandSize() {
        return this.getHand().size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return this.getId() == player.getId();
    }

    @Override
    public int hashCode() {
        return this.getId();
    }
}
