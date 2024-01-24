package com.seinksansdoozebank.fr.model.player.custombot.strategies.murderereffect;

import com.seinksansdoozebank.fr.model.character.abstracts.Character;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Bishop;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Condottiere;
import com.seinksansdoozebank.fr.model.character.commoncharacters.King;
import com.seinksansdoozebank.fr.model.character.commoncharacters.Merchant;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Architect;
import com.seinksansdoozebank.fr.model.character.specialscharacters.Assassin;
import com.seinksansdoozebank.fr.model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsingMurdererEffectToFocusRusherTest {
    Assassin spyAssassin;
    List<Character> characters;
    IUsingMurdererEffectStrategy strategy;
    Player mockPlayer;

    @BeforeEach
    void setUp() {
        spyAssassin = spy(Assassin.class);
        characters = new ArrayList<>(List.of(new Bishop(),
                spyAssassin,
                new Condottiere()));
        strategy = new UsingMurdererEffectToFocusRusher();
        mockPlayer = mock(Player.class);
    }

    @Test
    void applyWithMerchantInListShouldTargetMerchant() {
        characters.add(new Architect());
        characters.add(new Merchant());
        characters.add(new King());
        when(mockPlayer.getAvailableCharacters()).thenReturn(characters);
        strategy.apply(mockPlayer, spyAssassin);
        verify(spyAssassin).useEffect(new Merchant());
    }

    @Test
    void applyWithArchitectInListShouldTargetArchitect() {
        characters.add(new King());
        characters.add(new Architect());
        when(mockPlayer.getAvailableCharacters()).thenReturn(characters);
        strategy.apply(mockPlayer, spyAssassin);
        verify(spyAssassin).useEffect(new Architect());
    }

    @Test
    void applyWithKingInListShouldTargetKing() {
        characters.add(new King());
        when(mockPlayer.getAvailableCharacters()).thenReturn(characters);
        strategy.apply(mockPlayer, spyAssassin);
        verify(spyAssassin).useEffect(new King());
    }

    @Test
    void applyWithNoImportantCharacterInListShouldTargetRandom() {
        when(mockPlayer.getAvailableCharacters()).thenReturn(characters);
        strategy.apply(mockPlayer, spyAssassin);
        verify(spyAssassin).useEffect(any());
    }
}