package it.polimi.ingsw.am54.model.effects.characters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import it.polimi.ingsw.am54.model.*;
import java.util.ArrayList;

public class CharacterEffectsTest {

    private Player player;
    private Game game;

    @BeforeEach
    public void setUp() {
        // Prepariamo un giocatore pulito prima di ogni test
        player = new Player();
        game = new Game();
    }

    // Metodo di supporto per snellire il codice: crea la carta e la mette già nella tribù
    private CharacterCard creaPersonaggio(JobType type, int level) {
        // Passo parametri nulli/0 dove non servono per i test di Livello 1
        CharacterCard card = new CharacterCard(level, type, 0, null, new ArrayList<>(), 1);
        player.getMyTribe().add(card);
        return card;
    }

    @Test
    public void testConstructor() {
        CharacterCard costruttore = creaPersonaggio(JobType.CONSTRUCTOR, 2);
        CharacterConstructor effect = new CharacterConstructor();

        effect.apply(player, game, costruttore);

        // Il manuale: "Durante la partita, ogni Costruttore riduce, di quanto indicato in alto a destra, il costo in Cibo di ogni Edificio che prendete."[cite: 258].
        assertEquals(2, player.getMyBuildingDiscount(), "Il discount edifici deve aumentare del livello del costruttore");
    }

    @Test
    public void testShaman() {
        CharacterCard sciamano = creaPersonaggio(JobType.SHAMAN, 3);
        CharacterShaman effect = new CharacterShaman();

        effect.apply(player, game, sciamano);

        // Gli sciamani forniscono icone che hai mappato sui MagicPoints
        assertEquals(3, player.getMagicPoints(), "I punti magia devono aumentare del livello (icone) dello sciamano");
    }

    @Test
    public void testFarmer_NessunEffettoImmediato() {
        CharacterCard raccoglitore = creaPersonaggio(JobType.FARMER, 1);
        CharacterPassive effect = new CharacterPassive();

        int ciboIniziale = player.getFood();
        int auraIniziale = player.getAuraPoints();

        effect.apply(player, game, raccoglitore);

        // Il manuale: "Non ottenete mai Cibo direttamente dai Raccoglitori."
        // Lo sconto verrà applicato solo da EventFarmer. L'apply non deve fare nulla.
        assertEquals(ciboIniziale, player.getFood(), "Il cibo non deve cambiare");
        assertEquals(auraIniziale, player.getAuraPoints(), "L'aura non deve cambiare");
    }

    @Test
    public void testPassive() {
        CharacterCard artista = creaPersonaggio(JobType.ARTIST, 1);
        CharacterPassive effect = new CharacterPassive();

        int initialFood = player.getFood();
        int initialAura = player.getAuraPoints();

        effect.apply(player, game, artista);

        // Verifica che il Null Object Pattern funzioni e non alteri nulla
        assertEquals(initialFood, player.getFood(), "Le risorse non devono cambiare");
        assertEquals(initialAura, player.getAuraPoints(), "Le risorse non devono cambiare");
    }

    @Test
    public void testHunter_WithoutIcon() {
        // Creiamo un Cacciatore di Livello 0 (Senza icona carne)
        CharacterCard cacciatoreSenzaIcona = creaPersonaggio(JobType.HUNTER, 0);
        CharacterHunter effect = new CharacterHunter();

        effect.apply(player, game, cacciatoreSenzaIcona);

        // Il manuale: "Ogni volta che aggiungete alla vostra tribù un Cacciatore senza icona, non ottenete niente."[cite: 282].
        assertEquals(0, player.getFood(), "Un cacciatore livello 0 non deve fornire cibo immediato");
    }

    @Test
    public void testHunter_WithIcon() {
        // 1. Aggiungo prima un cacciatore senza icona (livello 0)
        creaPersonaggio(JobType.HUNTER, 0);

        // 2. Ora aggiungo un cacciatore CON icona (livello 1)
        CharacterCard cacciatoreConIcona = creaPersonaggio(JobType.HUNTER, 1);
        CharacterHunter effect = new CharacterHunter();

        effect.apply(player, game, cacciatoreConIcona);

        // Il manuale: "Ogni volta che aggiungete un Cacciatore con icona, prendete immediatamente 1 Cibo per ogni Cacciatore nella vostra tribù (con o senza icona)."[cite: 283].
        assertEquals(2, player.getFood(), "Deve dare 1 cibo per ogni cacciatore presente. Essendo in 2, deve dare 2 cibo");
    }
}