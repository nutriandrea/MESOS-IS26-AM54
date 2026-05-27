package it.polimi.ingsw.am54.gui.states;

import it.polimi.ingsw.am54.network.client.ClientModel;

import java.util.HashMap;
import java.util.Map;

public final class ClientStateRegistry {
    private static final Map<String, ClientStateHandler> HANDLERS = new HashMap<>();

    static {
        HANDLERS.put("STARTTURNSTATE", new StartTurnHandler());
        HANDLERS.put("STARTTURN",      new StartTurnHandler());
        HANDLERS.put("BIDDINGSTATE",   new StartTurnHandler());
        HANDLERS.put("EVENTSTATE",     new EventHandler());
        HANDLERS.put("ENDGAMESTATE",   new EndGameHandler());
        HANDLERS.put("SETUPGAMESTATE", new SetUpHandler());
        HANDLERS.put("LOBBY",          new SetUpHandler());
        HANDLERS.put("PICKINGSTATE",   new PickingHandler());
        HANDLERS.put("PICKING",        new PickingHandler());
    }

    private ClientStateRegistry() {}

    public static ClientStateHandler getHandler(String stateName) {
        ClientStateHandler handler = HANDLERS.get(stateName.toUpperCase());
        return handler != null ? handler : new DefaultHandler();
    }

    private record SimpleHandler(String instruction, boolean isGameState, boolean blockWhenNotMyTurn) implements ClientStateHandler {
        SimpleHandler(String instruction) {
            this(instruction, false, false);
        }

        SimpleHandler(String instruction, boolean isGameState, boolean blockWhenNotMyTurn) {
            this.instruction = instruction;
            this.isGameState = isGameState;
            this.blockWhenNotMyTurn = blockWhenNotMyTurn;
        }

        @Override
        public boolean showsEndTurnButton() {
            return false;
        }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return instruction;
        }

        @Override
        public boolean isLobbyState() {
            return !isGameState;
        }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) {
            return isGameState && !isMyTurn;
        }

        @Override
        public void renderCLI(CLIAbstract cli) {
        }
    }

    private record StartTurnHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() { return false; }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return "Scegli dove piazzare il tuo totem!";
        }

        @Override
        public boolean canInteractWithTiles() { return true; }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) { return !isMyTurn; }

        @Override
        public void renderCLI(CLIAbstract cli) { cli.renderStartTurn(); }
    }

    private record EventHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() { return false; }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return "Eventi in corso...";
        }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) { return true; }

        @Override
        public void renderCLI(CLIAbstract cli) {
            cli.renderEvent();
        }
    }

    private record EndGameHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() { return false; }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return "Partita terminata!";
        }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) { return true; }

        @Override
        public void renderCLI(CLIAbstract cli) { cli.renderEndGame(); }
    }

    private record SetUpHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() { return false; }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return "In attesa che l'host avvii la partita";
        }

        @Override
        public boolean isLobbyState() { return true; }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) { return false; }

        @Override
        public void renderCLI(CLIAbstract cli) { cli.renderSetup(); }
    }

    private record PickingHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() {
            return true;
        }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            int myPosition = -1;
            for (Map.Entry<Integer, String> entry : model.getOfferingTrack().entrySet()) {
                if (entry.getValue() != null && entry.getValue().contains(myName)) {
                    myPosition = entry.getKey();
                    break;
                }
            }
            if (myPosition == -1) {
                return "Extra pick! Pesca una carta dalla fila sopra o termina il turno!";
            }
            return "Scegli che carta prendere!";
        }

        @Override
        public boolean shouldBlockInput(boolean isMyTurn) { return !isMyTurn; }

        @Override
        public void renderCLI(CLIAbstract cli) { cli.renderPicking(); }
    }

    private record DefaultHandler() implements ClientStateHandler {
        @Override
        public boolean showsEndTurnButton() { return false; }

        @Override
        public String getInstruction(ClientModel model, String myName) {
            return "Tocca a te! Fai la tua mossa.";
        }

        @Override
        public void renderCLI(CLIAbstract cli) { cli.renderDefault(); }
    }
}
