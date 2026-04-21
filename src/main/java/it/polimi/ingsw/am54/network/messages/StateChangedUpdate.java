package it.polimi.ingsw.am54.network.messages;

public class StateChangedUpdate implements Update {

    private final String stateName;

    public StateChangedUpdate(String stateName) {
        this.stateName = stateName;
    }

    public String getStateName() { return stateName; }
}