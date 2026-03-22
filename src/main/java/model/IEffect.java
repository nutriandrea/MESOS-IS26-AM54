package model;

import java.io.*;
import java.util.*;

@FunctionalInterface
public interface IEffect<T extends Card> {
    /**
     */
    public void apply(Player p, Game g, T card);
}