package com.seinksansdoozebank.fr.model.player.custombot.strategies.picking;

import com.seinksansdoozebank.fr.model.player.Player;

/**
 * Implementation of the picking strategy where the player always pick district
 */
public class PickingAlwaysDistrict implements IPickingStrategy {
    @Override
    public void apply(Player player) {
        player.pickCardsKeepSomeAndDiscardOthers();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PickingAlwaysDistrict;
    }

    @Override
    public int hashCode() {
        return PickingAlwaysDistrict.class.getName().hashCode();
    }
}
