package model;

import java.io.*;
import java.util.*;

@FunctionalInterface
public interface IEffect<T extends Card> {
    /**
     */
    public void apply(Player p, Game g, T card);

    default boolean isFarmerEffect() {
        return false;
    }

    default boolean isNoLossEffect() {
        return false;
    }

    default boolean isDoubleAuraShamanEffect() {
        return false;
    }

    default boolean shouldResolveLast() {
        return false;
    }
}