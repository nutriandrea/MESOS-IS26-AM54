package it.polimi.ingsw.am54.model;


/**
 * Abstract Tribe card that acts as a population multiplier and base
 * for Character and Event cards. Implements the IDrawable interface for
 * managing drawing from the deck. All cards drawn from the tribe deck
 * are subclasses of TribeCard.
 * @author AM54
 */
public abstract class TribeCard extends Card implements IDrawable{

    /**
     * Constructor
     */
    public TribeCard(int era) {
        super(era);
    }

}