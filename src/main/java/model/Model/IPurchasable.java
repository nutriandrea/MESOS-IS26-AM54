package model.Model;

import java.io.*;
import java.util.*;


public interface IPurchasable {

    public boolean canBePurchased(Player player);

    public void buy(Player p);

}