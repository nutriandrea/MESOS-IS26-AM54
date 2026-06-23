package it.polimi.ingsw.am54.model;

import java.util.*;


/**
 * Carta Edificio: a differenza dei personaggi ha un costo in cibo (ridotto dagli
 * sconti del giocatore) e fornisce aura e/o effetti che si attivano in fasi diverse
 * (acquisto, fine turno, evento, fine partita).
 */
public class BuildingCard extends Card {

    private String name;

    private int cost;

    private int finalAura;

    private Phases phase;

    private List<IEffect<BuildingCard>> effects;

    private JobType buildingType;

    public BuildingCard(JobType type, int cost, int finalAura, Phases phase, List<IEffect<BuildingCard>> effects, int era) {
        super(era);
        this.buildingType = type;
        this.cost = cost;
        this.finalAura = finalAura;
        this.phase = phase;
        this.effects = effects;
    }

    /**
     * Applica tutti gli effetti "on purchase" dell'edificio sul giocatore.
     *
     * @param p il giocatore che ha costruito l'edificio
     * @param g la partita corrente
     */
    public void resolveEffects(Player p, Game g) {
        for (IEffect<BuildingCard> effect : this.effects) {
            effect.applyOnPurchase(p, g, this);
        }
    }

    public int getFinalAura() {
        return finalAura;
    }
    public List<IEffect<BuildingCard>> getEffects(){
        return this.effects;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public JobType getBuildingType() {
        return buildingType;
    }

    /**
     * Pesca l'edificio: calcola il costo effettivo (al netto dello sconto del
     * giocatore), verifica il cibo disponibile, scala il costo, assegna la proprietà
     * e risolve gli effetti immediati.
     *
     * @param player il giocatore che costruisce l'edificio
     * @param game   la partita corrente
     * @throws Exception se il giocatore non ha cibo sufficiente
     */
    @Override
    public void pick(Player player, Game game) throws Exception {
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);

        //controlla se il player può effettivamente acquistare
        if (player.getFood() < finalCost) {
            throw new Exception("Cibo insufficiente per comprare l'edificio! Hai " + player.getFood() + ", servono " + finalCost);
        }

        player.modifyFood(-finalCost);
        this.addOwner(player);
        player.getBuildings().add(this);

        this.resolveEffects(player, game);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Indica se il giocatore può permettersi l'edificio, considerando lo sconto
     * costruttori già accumulato.
     *
     * @param player il giocatore che vuole costruire
     * @return true se il cibo è sufficiente a coprire il costo scontato
     */
    @Override
    public boolean canBePurchased(Player player) {
        // Calcoliamo il costo effettivo considerando lo sconto del giocatore
        int finalCost = this.cost - player.getMyBuildingDiscount();
        finalCost = Math.max(0, finalCost);
        // Verifichiamo se il cibo corrente è sufficiente
        return player.getFood() >= finalCost;
    }

    /**
     * Rivela l'edificio inserendolo nella fila Edifici del tabellone.
     *
     * @param board la plancia di gioco
     * @param row   la fila di destinazione ("upper" o "lower")
     */
    @Override
    public void reveal(Board board, String row) {
        // Questo metodo sa esattamente come rimettere l'edificio al suo posto!
        board.addBuildingCard(this, row);
    }

    @Override
    public String toClientString() {
        String nomeEdificio = getName() != null ? getName() : "Edificio Speciale";
        return getImageId() + "|" + getEra() + "|0|BUILDING|" + getCost() + "|" + getFinalAura() + "|" + nomeEdificio;
    }
}
