package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

public class StateChangedUpdate implements Serializable {
    private final String stateName;

    public StateChangedUpdate(String stateName) {
        this.stateName = stateName;
    }

    public String getStateName() { return stateName; }
}
