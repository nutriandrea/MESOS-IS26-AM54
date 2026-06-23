package it.polimi.ingsw.am54.model;

/**
 * Classe base di tutte le carte di gioco (Personaggi, Eventi, Edifici).
 * Definisce i comportamenti comuni (era, proprietario, immagine) e i metodi
 * polimorfici che le sottoclassi specializzano: pesca, acquistabilità, rivelazione
 * sul tabellone e serializzazione per il client.
 */
public class Card {


    public Card(int era) {
        this.era = era;
    }

    private int era;

    private transient Player owner;

    public int getEra() {
        return era;
    }

    /**
     * Imposta l'era della carta.
     * Utilizzato dal EventCard.getName() per generare il nome della carta.
     *
     * @param era l'era della carta (1-3)
     */
    public void setEra(int era) {
        this.era = era;
    }

    public void addOwner(Player p) {
        this.owner = p;
    }

    /**
     * Esegue la logica di pesca/acquisto della carta da parte di un giocatore.
     * Implementazione di default non supportata: ogni sottoclasse concreta la
     * ridefinisce (es. il personaggio entra in tribù, l'edificio scala il cibo).
     *
     * @param player il giocatore che pesca la carta
     * @param game   la partita corrente (per applicare effetti e regole)
     * @throws Exception se il tipo di carta non è pescabile o l'azione non è valida
     */
    public void pick(Player player, Game game) throws Exception {
        throw new UnsupportedOperationException("Cannot pick this card type");
    }

    /**
     * Indica se il giocatore può permettersi questa carta. Di base sempre vero
     * (le carte gratuite); le sottoclassi a pagamento (es. {@link BuildingCard})
     * verificano il cibo disponibile.
     *
     * @param player il giocatore che vuole acquistare la carta
     * @return true se la carta è acquistabile dal giocatore
     */
    public boolean canBePurchased(Player player) {
        return true;
    }

    /**
     * Rivela la carta posizionandola nella fila corretta del tabellone.
     * Ogni sottoclasse sa in quale struttura inserirsi (personaggi, eventi, edifici).
     *
     * @param board la plancia su cui rivelare la carta
     * @param row   la fila di destinazione ("upper" o "lower")
     */
    public void reveal(Board board, String row) {
        throw new UnsupportedOperationException("Cannot reveal this generic card type");
    }

    private String imageId = "0";

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    /**
     * Restituisce una rappresentazione testuale della carta per il Client.
     * Ogni sottoclasse produce un formato specifico con i propri campi.
     * I dati sono separati da '|' per facilitare il parsing lato client.
     *
     * @return la stringa serializzata della carta
     */
    public String toClientString() {
        throw new UnsupportedOperationException("Formato client non definito per questa carta");
    }
}
