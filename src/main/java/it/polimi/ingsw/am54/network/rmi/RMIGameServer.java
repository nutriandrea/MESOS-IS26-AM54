package it.polimi.ingsw.am54.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI remota per la connessione iniziale del client.
 * Il client chiama connect() passando il proprio stub per ricevere
 * un RMIClientHandlerInterface personale su cui inviare le richieste.
 */
public interface RMIGameServer extends Remote {
    // Il client si connette, passa se stesso (il suo stub) e riceve in cambio il suo handler personale
    RMIClientHandlerInterface connect(RMIClientInterface clientStub) throws RemoteException;
}
