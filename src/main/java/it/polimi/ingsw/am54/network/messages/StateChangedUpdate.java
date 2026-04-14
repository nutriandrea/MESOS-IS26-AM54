package it.polimi.ingsw.am54.network.messages;

// Per ora mettiamo ClientMessage (al posto di Serializable).
// Più tardi, quando faremo il refactoring finale, diventerà "implements Update"!
public class StateChangedUpdate implements Update {

    private final String stateName;

    public StateChangedUpdate(String stateName) {
        this.stateName = stateName;
    }

    public String getStateName() { return stateName; }
}