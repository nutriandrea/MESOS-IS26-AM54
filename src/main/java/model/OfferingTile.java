package model;

import java.io.*;

public class OfferingTile {

    private char id;
    private int cardsFromUpper;
    private int cardsFromLower;
    private int foodBonus;
    private Player occupiedBy;

    /**
     * Costruttore di default
     */
    public OfferingTile() {
    }

    /**
     * Costruttore che legge gli attributi dal file in base all'ID
     * * @param id L'identificativo della tessera (es. 'A', 'B', 'C')
     */
    public OfferingTile(char id) {
        this.id = id;

        // Sostituisci con il percorso corretto del tuo file
        String filePath = "offering_tiles.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Ignoriamo righe vuote o commenti
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] data = line.split(",");

                // Controlliamo se la riga inizia con l'ID che stiamo cercando
                if (data.length >= 4 && data[0].trim().charAt(0) == id) {
                    this.cardsFromUpper = Integer.parseInt(data[1].trim());
                    this.cardsFromLower = Integer.parseInt(data[2].trim());
                    this.foodBonus = Integer.parseInt(data[3].trim());
                    break; // Trovata la tessera, fermiamo la lettura
                }
            }
        } catch (IOException e) {
            System.err.println("Errore di lettura del file per la tessera " + id + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Errore nel formato dei numeri nel file per la tessera " + id);
        }
    }

    public Player getOccupiedBy() {
        return occupiedBy;
    }

    public boolean isAvailable() {
        return occupiedBy == null;
    }

    public void occupy(Player p) {
        this.occupiedBy = p;
    }

    /**
     * Restituisce le carte che il giocatore può prendere.
     * Indice 0: carte dalla fila inferiore
     * Indice 1: carte dalla fila superiore
     */
    public int[] getCards() {
        // CORRETTO: l'array deve avere dimensione 2 per contenere due elementi
        int[] x = new int[2];
        x[0] = this.cardsFromLower;
        x[1] = this.cardsFromUpper;
        return x;
    }

    public char getId() {
        return id;
    }

    public int getFoodBonus() {
        return foodBonus;
    }
}