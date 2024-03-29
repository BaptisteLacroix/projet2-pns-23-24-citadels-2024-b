package com.seinksansdoozebank.fr.model.character.commoncharacters;

import com.seinksansdoozebank.fr.model.cards.DistrictType;
import com.seinksansdoozebank.fr.model.character.abstracts.CommonCharacter;
import com.seinksansdoozebank.fr.model.character.roles.Role;

/**
 * The merchant character
 */
public class Merchant extends CommonCharacter {
    /**
     * Merchant constructor
     */
    public Merchant() {
        super(Role.MERCHANT, DistrictType.TRADE_AND_CRAFTS);
    }

    /**
     * The merchant get 1 gold at the beginning of his turn
     */
    public void useEffect() {
        this.getPlayer().pickGold(1);
    }

    @Override
    public void applyEffect() {
        this.useEffect();
    }

}
