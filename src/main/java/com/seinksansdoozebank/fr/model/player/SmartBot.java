package com.seinksansdoozebank.fr.model.player;

import com.seinksansdoozebank.fr.model.bank.Bank;
import com.seinksansdoozebank.fr.model.cards.Card;
import com.seinksansdoozebank.fr.model.cards.Deck;
import com.seinksansdoozebank.fr.model.cards.District;
import com.seinksansdoozebank.fr.model.cards.DistrictType;
import com.seinksansdoozebank.fr.model.character.abstracts.Character;
import com.seinksansdoozebank.fr.model.character.abstracts.CommonCharacter;
import com.seinksansdoozebank.fr.model.character.commoncharacters.WarlordTarget;
import com.seinksansdoozebank.fr.model.character.roles.Role;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Architect;
import com.seinksansdoozebank.fr.model.character.specialscharacters.MagicianTarget;
import com.seinksansdoozebank.fr.view.IView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a smart bot which will try to build the cheaper district
 * in its hand in order to finish its citadel as fast as possible
 */
public class SmartBot extends Player {
    /**
     * SmartBot constructor
     *
     * @param nbGold the number of gold
     * @param deck   the deck
     * @param view   the view
     * @param bank   the bank
     */
    public SmartBot(int nbGold, Deck deck, IView view, Bank bank) {
        super(nbGold, deck, view, bank);
    }

    @Override
    public void playARound() {
        this.getCharacter().applyEffect();
        if (!this.getHand().isEmpty()) { // s'il a des cartes en main
            this.playWhenHandIsNotEmpty();
        } else { //s'il n'a pas de cartes en main
            this.pickCardsKeepSomeAndDiscardOthers();
            //il a choisi de piocher avant de jouer donc on regarde s'il n'a pas la libraire dans sa citadelle
            this.buyXCardsAndAddThemToCitadel(this.getNbDistrictsCanBeBuild());
        }
    }

    private void playWhenHandIsNotEmpty() {
        if (this.hasACardToPlay()) { // s'il y a une carte à jouer
            if (this.character instanceof Architect) {
                this.pickSomething();
                useEffectOfTheArchitect();
            } else {
                this.buyXCardsAndAddThemToCitadel(this.getNbDistrictsCanBeBuild()); //il joue
                this.useCommonCharacterEffect();
                this.pickSomething(); //il pioche quelque chose
            }
        } else {
            this.useCommonCharacterEffect();
            if (this.hasACardToPlay()) {
                this.buyXCardsAndAddThemToCitadel(this.getNbDistrictsCanBeBuild());
                pickSomething();
            } else {
                pickGold();
                if (this.hasACardToPlay()) {
                    this.buyXCardsAndAddThemToCitadel(this.getNbDistrictsCanBeBuild());
                }
            }
        }
    }

    @Override
    protected void pickSomething() {
        Optional<Card> optCheaperPlayableCard = this.chooseCard();
        if (optCheaperPlayableCard.isEmpty()) { //s'il n'y a pas de district le moins cher => la main est vide
            this.pickCardsKeepSomeAndDiscardOthers(); // => il faut piocher
        } else { //s'il y a un district le moins cher
            Card cheaperCard = optCheaperPlayableCard.get();
            if (this.getNbGold() < cheaperCard.getDistrict().getCost()) { //si le joueur n'a pas assez d'or pour acheter le district le moins cher
                this.pickGold(); // => il faut piocher de l'or
            } else { //si le joueur a assez d'or pour construire le district le moins cher
                this.pickCardsKeepSomeAndDiscardOthers(); // => il faut piocher un quartier pour savoir combien d'or sera nécessaire
            }
        }
    }


    /**
     * On choisit ici la carte qui coùte la moins chère des cartes proposées
     *
     * @param pickedCards the cards picked
     * @return the card that will be kept
     */
    @Override
    protected Card keepOneDiscardOthers(List<Card> pickedCards) {
        Optional<Card> cardKept = pickedCards.stream().min(Comparator.comparing(card -> card.getDistrict().getCost()));
        return cardKept.orElse(null);
    }

    /**
     * Choose the cheaper card among those wich are not already in the citadel OR by trying to play a DistrictType not already in the citadel if it has a CommonCharacter
     *
     * @return the chosenCard
     */
    @Override
    protected Optional<Card> chooseCard() {
        //Gathering districts which are not already built in player's citadel
        List<Card> notAlreadyPlayedCardList = this.getHand().stream().filter(cardHand -> this.getCitadel().stream().noneMatch(cardCitadel -> cardHand.getDistrict().equals(cardCitadel.getDistrict()))).toList();
        Optional<Card> cardToPlay;
        if (this.character instanceof CommonCharacter commonCharacter) {
            DistrictType target = commonCharacter.getTarget();
            cardToPlay = notAlreadyPlayedCardList.stream()
                    .filter(card -> card.getDistrict().getDistrictType() == target) // filter the cards that are the same as the character's target
                    .min(Comparator.comparing(card -> card.getDistrict().getCost())); // choose the cheaper one
        } else {
            cardToPlay = this.getCheaperCard(notAlreadyPlayedCardList);
        }
        if (cardToPlay.isPresent() && this.canPlayCard(cardToPlay.get())) {
            return cardToPlay;
        } else {
            return this.getCheaperCard(notAlreadyPlayedCardList);
        }
    }

    /**
     * Returns the cheaper district in the hand if there is one or an empty optional
     *
     * @param notAlreadyPlayedCardList the list of cards that are not already in the citadel
     * @return the cheaper district in the hand if there is one or an empty optional
     */
    protected Optional<Card> getCheaperCard(List<Card> notAlreadyPlayedCardList) {
        return notAlreadyPlayedCardList.stream().min(Comparator.comparing(card -> card.getDistrict().getCost()));
    }

    @Override
    public Character chooseCharacterImpl(List<Character> characters) {
        Character characterChoosen = null;
        Role roleToAvoid = null;
        if (this.getNbCharacterChosenInARow() >= Player.NB_MAX_CHARACTER_CHOSEN_IN_A_ROW) {
            roleToAvoid = this.getLastCharacterChosen().getRole();
        }
        if (this.getHand().size() <= 1 && Role.MAGICIAN != roleToAvoid) {
            Optional<Character> optionalCharacter = characters.stream().filter(c -> c.getRole() == Role.MAGICIAN).findFirst();
            if (optionalCharacter.isPresent()) {
                characterChoosen = optionalCharacter.get();
            }
        }
        // Choose the character by getting the frequency of each districtType in the citadel
        // and choosing the districtType with the highest frequency for the character
        List<DistrictType> districtTypeFrequencyList = getDistrictTypeFrequencyList(this.getCitadel());
        if (!districtTypeFrequencyList.isEmpty() && characterChoosen == null) {
            // Choose the character with the mostOwnedDistrictType
            for (DistrictType districtType : districtTypeFrequencyList) {
                for (Character character : characters) {
                    if (character.getRole() == roleToAvoid) {
                        continue;
                    }
                    if (character instanceof CommonCharacter commonCharacter && (commonCharacter.getTarget() == districtType)) {
                        characterChoosen = character;
                    }
                }
            }
        }
        // If no character has the mostOwnedDistrictType, choose a random character
        if (characterChoosen == null) {
            characterChoosen = characters.get(random.nextInt(characters.size()));
        }
        if (characterChoosen.equals(this.getLastCharacterChosen())) {
            this.setNbCharacterChosenInARow(this.getNbCharacterChosenInARow() + 1);
        } else {
            this.setNbCharacterChosenInARow(1);
        }
        return characterChoosen;
    }

    /**
     * Returns a list of districtType sorted by frequency in the citadel
     *
     * @param citadel the citadel of the player
     * @return a list of districtType sorted by frequency in the citadel
     */
    protected List<DistrictType> getDistrictTypeFrequencyList(List<Card> citadel) {
        return citadel.stream()
                .map(card -> card.getDistrict().getDistrictType())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }


    @Override
    public WarlordTarget chooseWarlordTarget(List<Opponent> opponentsFocusable) {
        // Get the player with the most districts
        Optional<Opponent> playerWithMostDistricts = opponentsFocusable.stream()
                .max(Comparator.comparing(player -> player.getCitadel().size()));
        if (playerWithMostDistricts.isEmpty()) {
            return null;
        }
        // Sort the districts of the player by cost
        List<Card> cardOfPlayerSortedByCost = playerWithMostDistricts.get().getCitadel().stream()
                .filter(card -> !card.getDistrict().equals(District.DONJON))
                .sorted(Comparator.comparing(card -> card.getDistrict().getCost()))
                .toList();
        // Destroy the district with the lowest cost, if not possible destroy the district with the second lowest cost, etc...
        for (Card card : cardOfPlayerSortedByCost) {
            if (this.getNbGold() >= card.getDistrict().getCost() - 1) {
                return new WarlordTarget(playerWithMostDistricts.get(), card.getDistrict());
            }
        }
        return null;
    }

    @Override
    public MagicianTarget useEffectMagician() {
        int numberOfCardsToExchange = this.getHand().size();

        Optional<Opponent> playerWithMostDistricts = this.getOpponents().stream()
                .max(Comparator.comparingInt(Opponent::getHandSize));

        // Case 1: Player has no cards in hand or fewer cards than the player with the most districts
        if (playerWithMostDistricts.isPresent() && numberOfCardsToExchange < playerWithMostDistricts.get().getHandSize()) {
            this.view.displayPlayerUseMagicianEffect(this, playerWithMostDistricts.get());
            return new MagicianTarget(playerWithMostDistricts.get(), null);
        }

        // Case 2: Player exchanges cards with the deck (cost > 2 gold)
        List<Card> cardsToExchange = this.getHand().stream()
                .filter(card -> card.getDistrict().getCost() > 2)
                .toList();

        if (!cardsToExchange.isEmpty()) {
            this.view.displayPlayerUseMagicianEffect(this, null);
            return new MagicianTarget(null, cardsToExchange);
        }

        return null;
    }


    /**
     * Il finit sa citadelle s'il peut en un coup, sinon il pose une merveille, sinon il complète les 5
     * couleurs de districtType sinon il joue comme un joueur normal
     */
    protected void useEffectOfTheArchitect() {
        int numberOfCardsNeededToFinishTheGame = this.getNumberOfDistrictsNeeded() - this.getCitadel().size();
        //On regarde s'il peut finir la partie en un coup en vérifiant si la citadelle a plus de 4 cartes, si dans sa main il a au moins 3 cartes
        //On vérifie s'il peut acheter les x districts manquant en choisissant les moins chèrs
        int nbDistrictsCanBeBuild = this.getNbDistrictsCanBeBuild();
        if (this.getCitadel().size() >= 5 && this.getHand().size() >= 3 && getPriceOfNumbersOfCheaperCards(numberOfCardsNeededToFinishTheGame) >= this.getNbGold()) {
            this.buyXCardsAndAddThemToCitadel(nbDistrictsCanBeBuild);
        } else {
            //on vérifie s'il y a une merveille dans sa main, si oui et qu'il peut la jouer alors il le fait
            Optional<Card> prestigeCard = this.getHand().stream().filter(card -> card.getDistrict().getDistrictType() == DistrictType.PRESTIGE).findFirst();
            if (prestigeCard.isPresent() && canPlayCard(prestigeCard.get())) {
                buyACardAndAddItToCitadel(prestigeCard.get());
            } else if (!this.hasFiveDifferentDistrictTypes()) {
                //il cherche à avoir les 5 districts de couleur dans sa citadelle sinon
                architectTryToCompleteFiveDistrictTypes();
            } else {
                //Il joue comme un joueur normal
                this.buyXCardsAndAddThemToCitadel(nbDistrictsCanBeBuild);
            }
        }
    }

    /**
     * @param numberCards the number of cards needed
     * @return the price of all the cards needed
     */
    public int getPriceOfNumbersOfCheaperCards(int numberCards) {
        this.getHand().sort(Comparator.comparing(card -> card.getDistrict().getCost()));
        return this.getHand().stream().limit(numberCards).mapToInt(card -> card.getDistrict().getCost()).sum();
    }

    /**
     * L'architecte essaye de compléter au maxim le nombre de couleurs de district dans sa citadelle
     */
    public void architectTryToCompleteFiveDistrictTypes() {
        //Création de la liste des cartes qu'il pourrait poser de sa main dans la citadelle intéressante pour lui en appelant la liste des
        //districtType manquant
        List<DistrictType> missingDistrictTypeInCitadel = findDistrictTypesMissingInCitadel();
        List<Card> cardNeeded = this.getHand().stream()
                .filter(card -> missingDistrictTypeInCitadel.contains(card.getDistrict().getDistrictType()))
                .toList();
        cardNeeded = new ArrayList<>(cardNeeded);

        int numberOfCards = 0;
        int i = 0;
        while (i < 3) {
            Optional<Card> optionalChosenCard = getCheaperCard(cardNeeded);
            if (optionalChosenCard.isPresent()) {
                Card cardChosen = optionalChosenCard.get();
                if (numberOfCards < 3 && (canPlayCard(cardChosen))) {
                    buyACardAndAddItToCitadel(cardChosen);
                    numberOfCards++;
                    cardNeeded.remove(cardChosen);
                }
            }
            i++;
        }
        //S'il n'y a aucune carte disponible ou bien qu'il ne peut en poser aucune alors il joue comme un joueur normal
        if (numberOfCards == 0) {
            this.buyXCardsAndAddThemToCitadel(this.getNbDistrictsCanBeBuild());
        }
    }

    @Override
    public Character useEffectAssassin() {
        Character target = this.chooseAssassinTarget();
        view.displayPlayerUseAssassinEffect(this, target);
        return target;
    }

    /**
     * Returns the target of the assassin chosen by using the strength of characters or randomly if no "interesting" character has been found
     *
     * @return the target of the assassin
     */
    @Override
    protected Character chooseAssassinTarget() {
        List<Role> roleInterestingToKill = new ArrayList<>(List.of(Role.ARCHITECT, Role.MERCHANT, Role.KING));
        Collections.shuffle(roleInterestingToKill);
        Character target = null;
        List<Character> charactersList = this.getAvailableCharacters();
        for (Role role : roleInterestingToKill) {
            for (Character character : charactersList) {
                if (character.getRole() == role) {
                    target = character;
                    break;
                }
            }
            if (target != null) {
                break;
            }
        }
        if (target == null) {
            target = charactersList.get(random.nextInt(charactersList.size()));
        }
        return target;
    }

    public void chooseColorCourtyardOfMiracle() {
        // if the player has all different district types except one DistrictType, the bot will choose the missing one
        List<DistrictType> listDifferentDistrictType = getDistrictTypeFrequencyList(this.getCitadel());
        if (listDifferentDistrictType.size() == 4) {
            for (DistrictType districtType : DistrictType.values()) {
                if (!listDifferentDistrictType.contains(districtType)) {
                    this.getCitadel().stream()
                            .filter(card -> card.getDistrict().equals(District.COURTYARD_OF_MIRACLE))
                            .forEach(card -> this.setColorCourtyardOfMiracleType(districtType));
                    return;
                }
            }
        }
        // Do nothing otherwise
    }

    @Override
    public boolean wantToUseManufactureEffect() {
        // if the bot has less than 2 cards in hand, it will use the manufacture effect to get more cards
        return this.getNbGold() > 3 && (this.getHand().size() < 2 || this.isLate());
    }

    /**
     * Determines if the bot is late or not
     *
     * @return true if the bot has less cards in his citadel than the average of the opponents
     */
    public boolean isLate() {
        return averageOpponentCitadelSize() > this.getCitadel().size();
    }

    /**
     * Returns the average size of the citadel of the opponents
     *
     * @return the average size of the citadel of the opponents
     */
    public double averageOpponentCitadelSize() {
        OptionalDouble average = this.getOpponents().stream().mapToInt(opponent -> opponent.getCitadel().size()).average();
        if (average.isEmpty()) {
            return 0;
        }
        return average.getAsDouble();
    }

    protected Optional<Character> chooseThiefTarget() {
        Optional<Character> victim = this.getAvailableCharacters().stream().filter(
                character -> character.getRole() != Role.ASSASSIN && character.getRole() != Role.THIEF &&
                        !character.isDead() && (character.getRole() == Role.ARCHITECT || character.getRole() == Role.MERCHANT)).findFirst();
        if (victim.isEmpty()) {
            victim = this.getAvailableCharacters().stream().filter(character -> character.getRole() != Role.ASSASSIN && character.getRole() != Role.THIEF &&
                    !character.isDead()).findFirst();
        }
        return victim;
    }

    public Card chooseCardToDiscardForLaboratoryEffect() {
        // find every card at 1 gold
        Stream<Card> cardsAtOneGold = this.getHand().stream().filter(card -> card.getDistrict().getCost() == 1);
        if (this.character instanceof CommonCharacter commonCharacter) {
            // find every card with not the same districtType as the character's target
            Optional<Card> cardsWithNotSameDistrictTypeAsTarget = cardsAtOneGold.filter(card -> card.getDistrict().getDistrictType() != commonCharacter.getTarget()).findFirst();
            // if there is a card with not the same districtType as the character's target, discard it
            if (cardsWithNotSameDistrictTypeAsTarget.isPresent()) {
                return cardsWithNotSameDistrictTypeAsTarget.get();
            }
        } else {
            // if there is a card at 1 gold, discard it
            Optional<Card> cardAtOneGold = cardsAtOneGold.findFirst();
            if (cardAtOneGold.isPresent()) {
                return cardAtOneGold.get();
            }
        }
        return null;
    }

    @Override
    protected boolean wantToUseCemeteryEffect(Card card) {
        // if the district cost less than 3, the bot will keep it
        return card.getDistrict().getCost() < 3 && this.getNbGold() > 0;
    }

    @Override
    public String toString() {
        return "Le bot malin " + this.id;
    }

}
