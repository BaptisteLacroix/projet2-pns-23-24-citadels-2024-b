package com.seinksansdoozebank.fr.model.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private final List<District> districtList;
    private Random random;

    /**
     * Constructor which implements a new deck of 65 districts
     */
    public Deck() {
        this.districtList = new ArrayList<>();
        random = new Random();
        fillDeck();
    }

    /**
     * We created the deck of 65 cards and then we shuffle it
     */
    private void fillDeck() {
        for (District district : District.values()) {
            //We take the ordinal which corresponds to a district and take the number of appearances
            int numberOfAppearance = district.getNumberOfAppearance();
            for (int j = 0; j < numberOfAppearance; j++) {
                //We add to the list the right number of the district called
                this.districtList.add(district);
            }
        }
        shuffle();
    }

    /**
     * @return the last element of districtList and we remove it
     */
    public District pick() {
        //On vérifie que la liste n'est pas vide
        if (districtList.isEmpty()) {
            //On recrée le deck
            fillDeck();
        }
        //On renvoie la dernière carte district du paquet et on l'enlève du paquet.
        return districtList.remove(districtList.size() - 1);
    }

    /**
     * Allows to discard a district
     * @param districtToDiscard the district to discard
     */
    public void discard(District districtToDiscard){
        this.districtList.add(0,districtToDiscard);
    }


    /**
     * The method shuffle takes the list of districts and shuffles it
     */
    protected void shuffle() {
        //On commence par la dernière carte du paquet
        for (int i = districtList.size() - 1; i >= 1; i--) {
            //on choisit un index au hasard parmi les autres éléments, cet index pourra prendre sa valeur entre 0 et i
            int j = random.nextInt(i + 1);
            //On échange l'élément à la i-ème place avec celui à la j-ème place
            Collections.swap(districtList, i, j);
        }
    }

    /**
     * getter
     *
     * @return the list of districts
     */
    public List<District> getDeck() {
        return districtList;
    }

}
