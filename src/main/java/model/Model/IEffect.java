package model.Model;

import java.io.*;
import java.util.*;

@FunctionalInterface
public interface IEffect {
    /**
     */
    public void apply(Player p, Game g, CharacterCard cc);
}