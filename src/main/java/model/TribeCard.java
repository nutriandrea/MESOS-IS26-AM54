package model;

import java.io.*;
import java.util.*;


public abstract class TribeCard extends Card implements IDrawable{

    /**
     * Constructor
     */
    public TribeCard(int era) {
        super(era);
    }

}